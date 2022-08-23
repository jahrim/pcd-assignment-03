package mvc.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import PluviometerActor.Message.*
import mvc.ActorModel
import mvc.domain.Pluviometer
import mvc.actor.ZoneActor
import mvc.actor.ZoneActor.Message.*

import scala.concurrent.duration.*

object PluviometerActor:
  def apply(pluviometer: Pluviometer[ActorRef[ZoneActor.Message]]): Behavior[Message] = Awaiting(pluviometer)

  /** Messages */
  enum Message { case StartMeasuring, StopMeasuring, Measure }

  /** Behavior A */
  object Awaiting:
    def apply(pluviometer: Pluviometer[ActorRef[ZoneActor.Message]]): Behavior[Message] =
      Behaviors.receive((_, message) =>
        message match {
          case StartMeasuring => Measuring(pluviometer)
          case StopMeasuring => Behaviors.stopped
          case _ => Behaviors.same
        }
      )

  /** Behavior B */
  object Measuring:
    def apply(pluviometer: Pluviometer[ActorRef[ZoneActor.Message]]): Behavior[Message] =
      Behaviors.setup { context =>
        Behaviors.withTimers { timers =>
          timers.startTimerWithFixedDelay(Measure, Measure, 1.seconds)
          Behaviors.receiveMessage {
            case Measure =>
              pluviometer.measure()
              pluviometer.zone ! Signal(pluviometer.signal)
              Behaviors.same
            case StopMeasuring => Behaviors.stopped
            case _ => Behaviors.same
          }
        }
      }