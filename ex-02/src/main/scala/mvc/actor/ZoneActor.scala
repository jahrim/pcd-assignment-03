package mvc.actor
//
//import akka.actor.typed.{ActorSystem, Behavior}
//import akka.actor.typed.scaladsl.Behaviors
//import akka.actor.typed.scaladsl.ActorContext
//import mvc.ActorModel
//import scala.concurrent.duration.*
//import ZoneActor.Message.*
//import mvc.actor.PluviometerActor
//
object ZoneActor:
//  def apply(zone: ActorModel.Zone): Behavior[Message] = Monitoring(zone)
//
  /** Messages */
  enum Message:
    case Signal(pluviometerId: String, on: Boolean)
    case UnderControl
    case Solved
//
//  /** Behavior A */
//  object Monitoring:
//    def apply(zone: ActorModel.Zone, signals: Map[String, Boolean] = Map()): Behavior[Message] =
//      Behaviors.receive((_, message) =>
//        message match {
//          case Signal(id, signal) => Monitoring(zone, signals + (id -> signal))
//          case UnderControl =>
//            Behaviors.same
//          case Solved =>
//            Behaviors.same
//        }
//      )