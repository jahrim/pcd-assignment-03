package actor

import actor.*
import actor.CityActor.*
import actor.CityActor.Message.*
import actor.FireStationActor.FireStation
import actor.PluviometerActor.Message.*
import actor.PluviometerActor.Pluviometer
import actor.ZoneActor.Message.Signal
import actor.ZoneActor.Zone
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior}
import cluster.AkkaCluster
import configuration.C.City.*
import configuration.C.Zone.MAX_PLUVIOMETERS_PER_ZONE
import util.Point2D

/**
 * Model the actor for a pluviometer.
 */
object PluviometerActor:
  /**
   * Model the messages of a pluviometer actor.
   */
  enum Message:
    /** Tells this pluviometer actor to take a measurement and forward the signal to the specified zone. */
    case RequestSignal(zone: ZoneRef)
    /** Tells this actor to take a snapshot of its state. */
    case TakeSnapshot

  /**
   * @param pluviometer the initial state of this pluviometer actor
   */
  def apply(pluviometer: Pluviometer): Behavior[Message] =
    Behaviors.setup { context => Active(pluviometer, context.spawn(Routers.group(CityActor.serviceKey), "CityRef")) }

  /** Behavior where this pluviometer actor takes measurements under the requests of other actors. */
  private[PluviometerActor] object Active:
    def apply(pluviometer: Pluviometer, city: CityRef): Behavior[Message] =
      Behaviors.setup { context =>
        Behaviors.receiveMessage {
          case RequestSignal(zone) =>
            zone ! Signal(context.self, pluviometer.measureAndSignal)
            Behaviors.same
          case TakeSnapshot =>
            city ! NotifyPluviometerState(context.self, pluviometer)
            Behaviors.same
        }
      }

  /**
   * Model a pluviometer that will emit a signal if the perceived rain
   * is greater than a certain threshold.
   * @param position  the position of this pluviometer
   * @param threshold the threshold of this pluviometer
   */
  abstract class Pluviometer(val position: Point2D, val threshold: Double):
    protected var _lastMeasurement: Double = 0
    /** @return the last measurement of this pluviometer. */
    def lastMeasurement: Double = this._lastMeasurement
    /** @return true if the last measurement of this pluviometer is greater than its threshold, false otherwise. */
    def signal: Boolean = this.lastMeasurement > this.threshold
    /**
     * Measures the amount of perceived rain and returns the signal of this pluviometer.
     * @return true if the last measurement of this pluviometer is greater than its threshold, false otherwise.
     */
    def measureAndSignal: Boolean =
      measure()
      signal
    /** Measures the amount of perceived rain. */
    def measure(): Unit

  /**
   * Companion object of [[Pluviometer]].
   */
  object Pluviometer:
    /** As [[RandomPluviometer new RandomPluviometer(position, signalProbability)]]. */
    def withRandomMeasurements(position: Point2D, signalProbability: Double): Pluviometer = RandomPluviometer(position, signalProbability)
    /**
     * Model a pluviometer that will perform random measurements.
     * @param position          the position of the pluviometer
     * @param signalProbability the probability of a measurement being greater than the threshold of the pluviometer
     */
    private case class RandomPluviometer(override val position: Point2D, signalProbability: Double)
      extends Pluviometer(position, 1 - signalProbability) :
      override def measure(): Unit = this._lastMeasurement = Math.random()