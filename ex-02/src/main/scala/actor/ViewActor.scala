package actor

import actor.CityActor.Snapshot
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import cluster.message.CborSerializable
import util.Id
import view.MainFXController
import actor.CityActor.*
import actor.ZoneActor.Solved
import configuration.C.View.*

object ViewActor:
  /**
   * Model the messages of a view actor.
   */
  trait Message extends CborSerializable
  /** Tells this view actor to handle the specified snapshot. */
  case class ReceiveSnapshot(snapshot: Snapshot) extends Message
  /** Tells this view actor to disable the alarm of the specified zone. */
  case class DisableAlarm(zoneId: String) extends Message
  /** Tells this view actor that he has successfully registered to the city he is displaying. */
  case object Registered extends Message
  /** Tells this view actor to register himself in the city he is displaying. */
  private case object RegisterSelf extends Message
  /** Tells this view actor to stop trying to register himself in the city he is displaying. */
  private case object FailedConnection extends Message

  /**
   * @param cityId the identifier of the city displayed by this view actor
   */
  def apply(viewController: MainFXController, cityId: String, viewId: String = Id.newId): Behavior[Message] =
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(ServiceKey[Message](viewId), context.self)
      viewController.viewActor = Option(context.spawnAnonymous(Routers.group(ServiceKey[ViewActor.Message](viewId))))
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(RegisterSelf, RegisterSelf, RECONNECTION_PERIOD)
        timers.startTimerAtFixedRate(FailedConnection, FailedConnection, CONNECTION_TIMEOUT)
        Active(viewId, viewController, context.spawnAnonymous(Routers.group(ServiceKey[CityActor.Message](cityId))))
      }
    }

  /** Behavior where the city takes a snapshot of itself periodically, notifying the view in the process. */
  private[ViewActor] object Active:
    def apply(viewId: String, viewController: MainFXController, city: CityRef, children: CityChildren = CityChildren()): Behavior[Message] =
      println(s"ViewActor: $children")
      Behaviors.setup { context =>
        Behaviors.withTimers { timers =>
          Behaviors.receiveMessage {
            case RegisterSelf =>
              city ! CityActor.RegisterView(viewId)
              Behaviors.same
            case Registered =>
              timers.cancel(RegisterSelf)
              timers.cancel(FailedConnection)
              Behaviors.same
            case FailedConnection =>
              context.log.error("View actor failed to connect to the specified city")
              viewController.exit()
              Behaviors.stopped
            case ReceiveSnapshot(snapshot) =>
              viewController.display(snapshot)
              Behaviors.same
            case DisableAlarm(zoneId) =>
              children.zones.get(zoneId) match
                case Some(zone) =>
                  zone ! Solved
                  Behaviors.same
                case None =>
                  val zone: ZoneRef = context.spawnAnonymous(Routers.group(ServiceKey[ZoneActor.Message](zoneId)))
                  children.zones = children.zones + (zoneId -> zone)
                  zone ! Solved
                  Active(viewId, viewController, city, children)
            case _ => Behaviors.unhandled
          }
        }
      }