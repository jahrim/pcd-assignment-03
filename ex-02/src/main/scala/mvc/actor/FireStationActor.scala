package mvc.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior}
import mvc.ActorModel
import mvc.actor.ZoneActor.Message.*
import mvc.actor.FireStationActor.Message.*
import mvc.domain.FireStation
import mvc.domain.FireStation.State.*
import configuration.C

import scala.concurrent.duration.*

object FireStationActor:
  def apply(fireStation: FireStation[ActorRef[ZoneActor.Message]]): Behavior[Message] = Working(fireStation)

  /** Messages */
  enum Message { case Alarm, Depart, Return }

  /** Behavior A */
  object Working:
    def apply(fireStation: FireStation[ActorRef[ZoneActor.Message]]): Behavior[Message] =
      Behaviors.receive((context, message) =>
        message match {
          case Alarm =>
            if fireStation.is(AVAILABLE) then context.scheduleOnce(C.City.PREPARATION_DURATION_MS.milli, fireStation.zone, UnderControl)
            Behaviors.same
          case Depart =>
            fireStation.become(BUSY)
            context.scheduleOnce(C.City.INTERVENTION_DURATION_MS.milli, fireStation.zone, Solved)
            Behaviors.same
          case Return =>
            fireStation.become(AVAILABLE)
            Behaviors.same
        }
      )