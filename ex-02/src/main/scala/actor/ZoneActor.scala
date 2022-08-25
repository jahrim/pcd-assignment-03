package actor

import actor.CityActor.*
import actor.CityActor.Message.*
import actor.FireStationActor.Message.{Alert, Depart, Return}
import actor.PluviometerActor.Message.RequestSignal
import actor.ZoneActor.Message.*
import actor.ZoneActor.Zone.State
import actor.ZoneActor.Zone.State.*
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior}
import configuration.C.Zone.{ALERT_PERIOD, MEASUREMENT_PERIOD}
import util.{Point2D, StateIn}

import scala.util.Random

/**
 * Model the actor for a zone.
 */
object ZoneActor:
  /**
   * Model the messages of a zone actor.
   */
  enum Message:
    /** Tells this actor to take a snapshot of its state. */
    case TakeSnapshot
    /** Tells this zone actor that the specified pluviometer has emitted the specified signal. */
    case Signal(sender: PluviometerRef, on: Boolean)
    /** Asks this zone actor if the specified fire-station can take care of his alarm. */
    case DepartureRequest(sender: FireStationRef)
    /** Tells this zone actor that his alarm has been taken care of. */
    case Solved
    /** Tells this zone actor request the signals of his pluviometers. */
    private[ZoneActor] case RequestSignals
    /** Tells this zone actor to alert all of his fire-stations that he is under alarm. */
    private[ZoneActor] case AlertFirestations

  /**
   * @param zone the initial state of this zone
   * @param pluviometers the pluviometers in this zone
   * @param fireStations the fire-stations in this zone
   */
  def apply(zone: Zone, pluviometers: List[PluviometerRef], fireStations: List[FireStationRef]): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(RequestSignals, RequestSignals, MEASUREMENT_PERIOD)
        CalmBehavior(zone, context.spawn(Routers.group(CityActor.serviceKey), "CityRef"), Map.from(pluviometers.map((_, false))), fireStations)
      }
    }

  /** Behavior where this zone actor collects the signals from his pluviometers and evaluates their majority. */
  private[ZoneActor] object CalmBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[PluviometerRef, Boolean], fireStations: List[FireStationRef]): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Calm)
        Behaviors.receiveMessage {
          case Signal(sensor, signal) =>
            val updatedMap = signals + (sensor -> signal)
            updatedMap.values.partition(s => s) match
              case (on, off) if on.size >= off.size => AlarmedBehavior(zone, city, updatedMap, fireStations)
              case _ => CalmBehavior(zone, city, updatedMap, fireStations)
          case RequestSignals =>
            signals.keys.foreach(_ ! RequestSignal(context.self))
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyZoneState(context.self, zone)
            Behaviors.same
          case _ => Behaviors.unhandled
        }
      }

  /**
   * Behavior where this zone actor periodically notifies all of his fire-stations that he is under alarm,
   * until a fire-station takes control of his situation.
   */
  private[ZoneActor] object AlarmedBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[PluviometerRef, Boolean], fireStations: List[FireStationRef]): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Alarmed)
        Behaviors.withTimers { timers =>
          timers.startTimerWithFixedDelay(AlertFirestations, AlertFirestations, ALERT_PERIOD)
          Behaviors.receiveMessage {
            case Signal(sensor, signal) => AlarmedBehavior(zone, city, signals + (sensor -> signal), fireStations)
            case RequestSignals =>
              signals.keys.foreach(_ ! RequestSignal(context.self))
              Behaviors.same
            case TakeSnapshot =>
              city ! NotifyZoneState(context.self, zone)
              Behaviors.same
            case AlertFirestations =>
              fireStations.foreach(_ ! Alert(context.self))
              Behaviors.same
            case DepartureRequest(firestation) =>
              firestation ! Depart(context.self)
              timers.cancel(AlertFirestations)
              MonitoredBehavior(zone, city, signals, fireStations, monitoredBy = firestation)
            case _ => Behaviors.unhandled
          }
        }
      }

  /** Behavior where this zone actor is waiting for a specific fire-station to solve his situation. */
  private[ZoneActor] object MonitoredBehavior:
    def apply(zone: Zone, city: CityRef, signals: Map[PluviometerRef, Boolean], fireStations: List[FireStationRef], monitoredBy: FireStationRef): Behavior[Message] =
      Behaviors.setup { context =>
        zone.become(Monitored)
        Behaviors.receiveMessage {
          case Signal(sensor, signal) => MonitoredBehavior(zone, city, signals + (sensor -> signal), fireStations, monitoredBy)
          case RequestSignals =>
            signals.keys.foreach(_ ! RequestSignal(context.self))
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyZoneState(context.self, zone)
            Behaviors.same
          case Solved =>
            monitoredBy ! Return
            CalmBehavior(zone, city, signals, fireStations)
          case _ => Behaviors.unhandled
        }
      }

  /**
   * Model the state of a zone actor.
   * @param position the position of the zone
   * @param width the width of the zone
   * @param height the height of the zone
   */
  case class Zone(position: Point2D, width: Double, height: Double) extends StateIn[State](Calm):
    private[Zone] enum SplitDirection { case HORIZONTAL, VERTICAL }
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

  /**
   * Companion object of [[Zone]].
   */
  object Zone:
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