package actor

import actor.*
import actor.CityActor.*
import actor.FireStationActor.*
import actor.FireStationActor.FireStation.State.*
import actor.FireStationActor.FireStation.State
import actor.PluviometerActor.Pluviometer
import actor.ZoneActor.*
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.Behavior
import cluster.message.CborSerializable
import configuration.C.FireStation.*
import configuration.C.Zone.RANDOM_POSITION_PADDING
import util.{Id, Point2D, StateIn}
import scala.util.Random

/**
 * Model the actor for a fire-station.
 */
object FireStationActor:
  /**
   * Model the messages of a fire-station actor.
   */
  trait Message extends CborSerializable
  /** Tells this actor to take a snapshot of its state and forward it to the specified city. */
  case class TakeSnapshot(city: CityRef) extends Message
  /** Tells this fire-station actor that the specified zone is under alarm. */
  case class Alert(zone: ZoneRef) extends Message
  /** Tells this fire-station actor to depart towards the specified zone. */
  case class Depart(zone: ZoneRef) extends Message
  /** Tells this fire-station actor to return his fire-fighters back to the station. */
  case object Return extends Message
  /** Tells this fire-station actor to notify the zone which he is taking care of that its alarm has been solved. */
  private[FireStationActor] case object SolveAlarm extends Message

  /**
   * @param fireStation the initial state of this fire-station actor
   */
  def apply(fireStation: FireStation): Behavior[Message] =
    Behaviors.setup { context =>
      context.system.receptionist ! Register(ServiceKey[Message](fireStation.id), context.self)
      AvailableBehavior(fireStation)
    }

  /** Behavior where the fire-fighters of this fire-station are at the station. */
  private[FireStationActor] object AvailableBehavior:
    def apply(fireStation: FireStation): Behavior[Message] =
      Behaviors.setup { context =>
        fireStation.become(Available)
        Behaviors.receiveMessage {
          case Alert(zone) =>
            zone ! DepartureRequest(context.self)
            Behaviors.same
          case Depart(zone) => BusyBehavior(fireStation, zone)
          case TakeSnapshot(city) =>
            city ! NotifyFireStationState(fireStation.data)
            Behaviors.same
          case _ => Behaviors.unhandled
        }
      }

  /** Behavior where the fire-fighters of this fire-station have departed to solve an emergency. */
  private[FireStationActor] object BusyBehavior:
    def apply(fireStation: FireStation, monitoredZone: ZoneRef): Behavior[Message] =
      Behaviors.setup { context =>
        fireStation.become(Busy)
        Behaviors.withTimers { timers =>
          timers.startTimerWithFixedDelay(SolveAlarm, SolveAlarm, INTERVENTION_DURATION)
          Behaviors.receiveMessage {
            case SolveAlarm =>
              monitoredZone ! Solved
              Behaviors.same
            case Return =>
              timers.cancel(SolveAlarm)
              AvailableBehavior(fireStation)
            case TakeSnapshot(city) =>
              city ! NotifyFireStationState(fireStation.data)
              Behaviors.same
            case _ => Behaviors.unhandled
          }
        }
      }

  /**
   * Model the state of a fire-station actor.
   * @param position the position of the fire-station
   * @param id       the identifier of this fire-station
   */
  case class FireStation(position: Point2D, id: String = Id.newId) extends StateIn[State](Available) with Id:
    /** @return the data representing this fire-station. */
    def data: FireStationData = FireStationData(position, id, state.ordinal)
    override def toString: String = s"FireStation(#$id,$state,$position)"
  /**
   * Companion object of [[FireStation]].
   */
  object FireStation:
    /**
     * @param zone the specified zone
     * @return a new fire-station randomly positioned in the specified zone.
     */
    def random(zone: Zone): FireStation = FireStation(zone.randomPosition(RANDOM_POSITION_PADDING))

    /**
     * Model the state of a fire-station.
     */
    enum State:
      /** State where the fire-fighters of this fire-station are still at the station. */
      case Available
      /** State where the fire-fighters of this fire-station are busy taking care of an alarm. */
      case Busy
    /**
     * Companion object of [[State]].
     */
    object State:
      /** @return a random zone state. */
      def random: State = State.fromOrdinal(Random.nextInt(State.values.length))

  /**
   * Model the data representing a fire-station.
   */
  case class FireStationData(position: Point2D, id: String = Id.newId, state: Int) extends CborSerializable with Id:
    /** @return the fire-station represented by this data. */
    def asFireStation: FireStation =
      val fireStation = FireStation(position, id)
      fireStation.become(State.fromOrdinal(state))
      fireStation
    override def toString: String =
      s"FireStationData(id:$id, position:$position, state:${State.fromOrdinal(state)})"