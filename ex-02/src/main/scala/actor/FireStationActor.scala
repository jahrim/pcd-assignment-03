package actor

import actor.*
import actor.CityActor.*
import actor.CityActor.Message.*
import actor.FireStationActor.FireStation
import actor.FireStationActor.FireStation.State
import actor.FireStationActor.FireStation.State.*
import actor.FireStationActor.Message.*
import actor.PluviometerActor.Pluviometer
import actor.ZoneActor.Message.{DepartureRequest, Solved}
import actor.ZoneActor.Zone
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import cluster.AkkaCluster
import configuration.C.FireStation.*
import util.{Point2D, StateIn}

/**
 * Model the actor for a fire-station.
 */
object FireStationActor:
  /**
   * Model the messages of a fire-station actor.
   */
  enum Message:
    /** Tells this fire-station actor that the specified zone is under alarm. */
    case Alert(zone: ZoneRef)
    /** Tells this fire-station actor to depart towards the specified zone. */
    case Depart(zone: ZoneRef)
    /** Tells this fire-station actor to return his fire-fighters back to the station. */
    case Return
    /** Tells this actor to take a snapshot of its state. */
    case TakeSnapshot
    /** Tells this fire-station actor to notify the zone which he is taking care of that its alarm has been solved. */
    private[FireStationActor] case SolveAlarm

  /**
   * @param fireStation the initial state of this fire-station actor
   */
  def apply(fireStation: FireStation): Behavior[Message] =
    Behaviors.setup { context =>
      AvailableBehavior(fireStation, context.spawn(Routers.group(CityActor.serviceKey), "CityRef"))
    }

  /** Behavior where the fire-fighters of this fire-station are at the station. */
  private[FireStationActor] object AvailableBehavior:
    def apply(fireStation: FireStation, city: CityRef): Behavior[Message] =
      Behaviors.setup { context =>
        fireStation.become(Available)
        Behaviors.receiveMessage {
          case Alert(zone) =>
            zone ! DepartureRequest(context.self)
            Behaviors.same
          case Depart(zone) => BusyBehavior(fireStation, city, zone)
          case TakeSnapshot =>
            city ! NotifyFireStationState(context.self, fireStation)
            Behaviors.same
          case _ => Behaviors.unhandled
        }
      }

  /** Behavior where the fire-fighters of this fire-station have departed to solve an emergency. */
  private[FireStationActor] object BusyBehavior:
    def apply(fireStation: FireStation, city: CityRef, zone: ZoneRef): Behavior[Message] =
      Behaviors.setup { context =>
        fireStation.become(Busy)
        Behaviors.withTimers { timers =>
          timers.startTimerWithFixedDelay(SolveAlarm, SolveAlarm, INTERVENTION_DURATION)
          Behaviors.receiveMessage {
            case SolveAlarm =>
              zone ! Solved
              Behaviors.same
            case Return => AvailableBehavior(fireStation, city)
            case TakeSnapshot =>
              city ! NotifyFireStationState(context.self, fireStation)
              Behaviors.same
            case _ => Behaviors.unhandled
          }
        }
      }

  /**
   * Model the state of a fire-station actor.
   * @param position the position of the fire-station
   */
  case class FireStation(position: Point2D) extends StateIn[State](Available)
  /**
   * Companion object of [[FireStation]].
   */
  object FireStation:
    /**
     * @param zone the specified zone
     * @return a new fire-station randomly positioned in the specified zone.
     */
    def random(zone: Zone): FireStation = FireStation(zone.randomPosition)

    /**
     * Model the state of a fire-station.
     */
    enum State:
      /** State where the fire-fighters of this fire-station are still at the station. */
      case Available
      /** State where the fire-fighters of this fire-station are busy taking care of an alarm. */
      case Busy