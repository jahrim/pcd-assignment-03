package actor

import actor.CityActor.*
import actor.FireStationActor.*
import actor.PluviometerActor.*
import actor.ZoneActor.*
import actor.ZoneActor.Zone.{SplitDirection, State}
import actor.ZoneActor.Zone.State.*
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior}
import cluster.message.CborSerializable
import configuration.C.Zone.{ALERT_PERIOD, MEASUREMENT_PERIOD}
import configuration.C.Log.pretty
import util.{Id, Point2D, StateIn}

import scala.util.Random

/**
 * Model the actor for a zone.
 */
object ZoneActor:
  /**
   * Model the messages of a zone actor.
   */
  trait Message extends CborSerializable
  /** Tells this actor to take a snapshot of its state. */
  case object TakeSnapshot extends Message
  /** Tells this zone actor that the specified pluviometer has emitted the specified signal. */
  case class Signal(pluviometerId: String, on: Boolean) extends Message
  /** Asks this zone actor if the specified fire-station can take care of his alarm. */
  case class DepartureRequest(sender: FireStationRef) extends Message
  /** Tells this zone actor that his alarm has been taken care of. */
  case object Solved extends Message
  /** Tells this zone actor request the signals of his pluviometers. */
  private[ZoneActor] case object RequestSignals extends Message
  /** Tells this zone actor to alert all of his fire-stations that he is under alarm. */
  private[ZoneActor] case object AlertFirestations extends Message

  /**
   * @param zone the initial state of this zone
   * @param cityId the identifier of the city this zone belongs to
   * @param pluviometerIds the identifiers of the pluviometers in this zone
   * @param fireStationIds the identifiers of the fire-stations in this zone
   */
  def apply(zone: Zone, cityId: String, pluviometerIds: List[String], fireStationIds: List[String]): Behavior[Message] =
    Behaviors.setup { context =>
      context.system.receptionist ! Register(ServiceKey[Message](zone.id), context.self)
      val children: ZoneChildren = ZoneChildren()
      children.pluviometers = pluviometerIds.map(id => context.spawnAnonymous(Routers.group(ServiceKey[PluviometerActor.Message](id))))
      children.fireStations = fireStationIds.map(id => context.spawnAnonymous(Routers.group(ServiceKey[FireStationActor.Message](id))))
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(RequestSignals, RequestSignals, MEASUREMENT_PERIOD)
        CalmBehavior(zone, context.spawnAnonymous(Routers.group(ServiceKey[CityActor.Message](cityId))), Map.from(pluviometerIds.map((_, false))), children)
      }
    }

  /** Behavior where this zone actor collects the signals from his pluviometers and evaluates their majority. */
  private[ZoneActor] object CalmBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[String, Boolean], children: ZoneChildren): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Calm)
        Behaviors.receiveMessage {
          case Signal(sensor, signal) =>
            val updatedMap = signals + (sensor -> signal)
            updatedMap.values.partition(s => s) match
              case (on, off) if on.size >= off.size =>
                Behaviors.withTimers { timers =>
                  timers.startTimerWithFixedDelay(AlertFirestations, AlertFirestations, ALERT_PERIOD)
                  AlarmedBehavior(zone, city, updatedMap, children)
                }
              case _ => CalmBehavior(zone, city, updatedMap, children)
          case RequestSignals =>
            children.pluviometers.foreach(_ ! RequestSignal(context.self))
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyZoneState(zone.data)
            Behaviors.same
          case _ => Behaviors.unhandled
        }
      }

  /**
   * Behavior where this zone actor periodically notifies all of his fire-stations that he is under alarm,
   * until a fire-station takes control of his situation.
   */
  private[ZoneActor] object AlarmedBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[String, Boolean], children: ZoneChildren): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Alarmed)
        Behaviors.receiveMessage {
          case Signal(sensor, signal) => AlarmedBehavior(zone, city, signals + (sensor -> signal), children)
          case RequestSignals =>
            children.pluviometers.foreach(_ ! RequestSignal(context.self))
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyZoneState(zone.data)
            Behaviors.same
          case AlertFirestations =>
            children.fireStations.foreach(_ ! Alert(context.self))
            Behaviors.same
          case DepartureRequest(firestation) =>
            firestation ! Depart(context.self)
            Behaviors.withTimers{ timers =>
              timers.cancel(AlertFirestations)
              MonitoredBehavior(zone, city, signals, children, monitoredBy = firestation)
            }
          case _ => Behaviors.unhandled
        }
      }

  /** Behavior where this zone actor is waiting for a specific fire-station to solve his situation. */
  private[ZoneActor] object MonitoredBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[String, Boolean], children: ZoneChildren, monitoredBy: FireStationRef): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Monitored)
        Behaviors.receiveMessage {
          case Signal(sensor, signal) => MonitoredBehavior(zone, city, signals + (sensor -> signal), children, monitoredBy)
          case RequestSignals =>
            children.pluviometers.foreach(_ ! RequestSignal(context.self))
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyZoneState(zone.data)
            Behaviors.same
          case Solved =>
            monitoredBy ! Return
            CalmBehavior(zone, city, signals, children)
          case _ => Behaviors.unhandled
        }
      }

  /**
   * Model the state of a zone actor.
   * @param position the position of the zone
   * @param width the width of the zone
   * @param height the height of the zone
   * @param id the identifier of the zone
   */
  case class Zone(position: Point2D, width: Double, height: Double, id: String = Id.newId) extends StateIn[State](Calm) with Id:
    /**
     * Divides this zone area in the specified number of sub-zones, randomly created.
     *
     * @param numberOfSubZones the specified number of sub-zones
     * @return a list of the zone areas of the newly created sub-zones
     */
    def splitInto(numberOfSubZones: Int): List[Zone] =
      def splitIntoDirectional(zone: Zone, subZones: Int, direction: SplitDirection): List[Zone] =
        val splitPoint: Point2D = zone.randomPosition
        val surplus: (Int, Int) = if subZones % 2 == 0 then (0, 0) else Seq((0, 1), (1, 0))(Random.nextInt(2))
        if subZones > 1 then direction match
          case SplitDirection.HORIZONTAL =>
            val top: Zone = Zone(zone.position, zone.width, zone.height - splitPoint.y)
            val bottom: Zone = Zone(zone.position.relocateY(splitPoint.y), zone.width, splitPoint.y)
            splitIntoDirectional(top, subZones / 2 + surplus._1, SplitDirection.VERTICAL)
              :::
              splitIntoDirectional(bottom, subZones / 2 + surplus._2, SplitDirection.VERTICAL)
          case SplitDirection.VERTICAL =>
            val left: Zone = Zone(zone.position, splitPoint.x, zone.height)
            val right: Zone = Zone(zone.position.relocateX(splitPoint.x), zone.width - splitPoint.x, zone.height)
            splitIntoDirectional(left, subZones / 2 + surplus._1, SplitDirection.HORIZONTAL)
              :::
              splitIntoDirectional(right, subZones / 2 + surplus._2, SplitDirection.HORIZONTAL)
        else List(zone)
      splitIntoDirectional(this, numberOfSubZones, Seq(SplitDirection.HORIZONTAL, SplitDirection.VERTICAL)(Random.nextInt(2)))
    /** @return a random point inside this zone area. */
    def randomPosition: Point2D = (this.position.x + Math.random() * this.width, this.position.y - Math.random() * this.height)
    /**
     * @param p the specified point
     * @return true if the specified point is inside this zone, false otherwise.
     */
    def contains(p: Point2D): Boolean =
      p.x > this.position.x && p.x < this.position.x + this.width &&
      p.y > this.position.y - this.height && p.y < this.position.y
    /** @return the data representing this zone. */
    def data: ZoneData = ZoneData(position, width, height, id, state.ordinal)
    override def toString: String = s"Zone(#$id,$state,$position,${width.pretty},${height.pretty})"


  /**
   * Companion object of [[Zone]].
   */
  object Zone:
    private[Zone] enum SplitDirection { case HORIZONTAL, VERTICAL }

    /**
     * Model the state of a zone.
     */
    enum State:
      /** State where this zone is not under alarm. */
      case Calm
      /** State where this zone is under alarm. */
      case Alarmed
      /** State where this zone is under alarm but a fire-station is taking care of it. */
      case Monitored

  /**
   * Model the data representing a zone.
   */
  case class ZoneData(position: Point2D, width: Double, height: Double, id: String = Id.newId, state: Int) extends CborSerializable:
    /** @return the zone represented by this data. */
    def zone: Zone =
      val zone = Zone(position, width, height, id)
      zone.become(State.fromOrdinal(state))
      zone

  /**
   * Model a collection of the actors known by a zone.
   *
   * @param pluviometers a list of the pluviometer actors known by the zone
   * @param fireStations a list of the fire-station actors known by the zone
   */
  case class ZoneChildren(
    var pluviometers: List[PluviometerRef] = List(),
    var fireStations: List[FireStationRef] = List(),
  )