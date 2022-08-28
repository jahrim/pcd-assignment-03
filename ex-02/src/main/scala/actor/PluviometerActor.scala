package actor

import actor.*
import actor.CityActor.*
import actor.PluviometerActor.*
import actor.ZoneActor.*
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.Behavior
import cluster.message.CborSerializable
import configuration.C.City.*
import configuration.C.Log.*
import util.{Id, Point2D}

/**
 * Model the actor for a pluviometer.
 */
object PluviometerActor:
  /**
   * Model the messages of a pluviometer actor.
   */
  trait Message extends CborSerializable
  /** Tells this pluviometer actor to take a measurement and forward the signal to the specified zone. */
  case class RequestSignal(zone: ZoneRef) extends Message
  /** Tells this actor to take a snapshot of its state. */
  case object TakeSnapshot extends Message

  /**
   * @param pluviometer the initial state of this pluviometer actor
   * @param cityId      the identifier of the city this pluviometer belongs to
   */
  def apply(pluviometer: Pluviometer, cityId: String): Behavior[Message] =
    Behaviors.setup { context =>
      context.system.receptionist ! Register(ServiceKey[Message](pluviometer.id), context.self)
      Active(pluviometer, context.spawnAnonymous(Routers.group(ServiceKey[CityActor.Message](cityId))))
    }

  /** Behavior where this pluviometer actor takes measurements under the requests of other actors. */
  private[PluviometerActor] object Active:
    def apply(pluviometer: Pluviometer, city: CityRef): Behavior[Message] =
      Behaviors.receiveMessage {
        case RequestSignal(zone) =>
          zone ! Signal(pluviometer.id, pluviometer.measureAndSignal)
          Behaviors.same
        case TakeSnapshot =>
          city ! NotifyPluviometerState(pluviometer.data)
          Behaviors.same
      }

  /**
   * Model a pluviometer that will emit a signal if the perceived rain
   * is greater than a certain threshold.
   * @param position  the position of this pluviometer
   * @param threshold the threshold of this pluviometer
   * @param id        the identifier of this pluviometer
   */
  abstract class Pluviometer(val position: Point2D, val threshold: Double, val id: String = Id.newId) extends Id:
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
    /** @return the data representing this pluviometer. */
    def data: PluviometerData = PluviometerData(position, lastMeasurement, threshold, signal, id)

  /**
   * Companion object of [[Pluviometer]].
   */
  object Pluviometer:
    /** As [[RandomPluviometer new RandomPluviometer(position, signalProbability, id)]]. */
    def withRandomMeasurements(position: Point2D, signalProbability: Double, id: String = Id.newId): Pluviometer =
      RandomPluviometer(position, signalProbability, id)

    /**
     * Model a pluviometer that will perform random measurements.
     * @param position          the position of the pluviometer
     * @param signalProbability the probability of a measurement being greater than the threshold of the pluviometer
     * @param id                the identifier of this pluviometer
     */
    private class RandomPluviometer(position: Point2D, signalProbability: Double, id: String = Id.newId)
      extends Pluviometer(position, 1 - signalProbability, id) :
      override def measure(): Unit = this._lastMeasurement = Math.random()
      override def toString: String = s"Pluviometer(#$id,${lastMeasurement.pretty},$signal,${signalProbability.pretty})"

  /**
   * Model the data representing a pluviometer.
   */
  case class PluviometerData(
    position: Point2D,
    lastMeasurement: Double,
    threshold: Double,
    signal: Boolean,
    id: String = Id.newId
  ) extends CborSerializable