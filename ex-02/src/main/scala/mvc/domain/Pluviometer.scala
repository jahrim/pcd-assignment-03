package mvc.domain

import configuration.C.Log.*
import mvc.domain.Point2D

/**
 * Model a pluviometer that will emit a signal if the perceived rain
 * is greater than a certain threshold.
 * @param zone the zone this pluviometer belongs to
 * @param position the position of this pluviometer in the specified zone
 * @param threshold the threshold of the pluviometer
 */
abstract class Pluviometer[Z](val zone: Z, val position: Point2D, val threshold: Double):
    protected var _lastMeasurement: Double = 0

    /** @return the last measurement of this pluviometer. */
    def lastMeasurement: Double = this._lastMeasurement
    /** @return true if the last measurement of this pluviometer is greater than its threshold, false otherwise. */
    def signal: Boolean = this.lastMeasurement > this.threshold
    /** Measures the amount of perceived rain. */
    def measure(): Unit

/**
 * Companion object of [[Pluviometer]].
 */
object Pluviometer:
    /** As [[RandomPluviometer new RandomPluviometer(position, signalProbability)]]. */
    def withRandomMeasurements[Z](zone: Z, position: Point2D, signalProbability: Double): Pluviometer[Z] = RandomPluviometer(zone, position, signalProbability)

    /**
     * Model a pluviometer that will perform random measurements.
     * @param zone the zone this pluviometer belongs to
     * @param position the position of the pluviometer
     * @param signalProbability the probability of a measurement being greater than the threshold of the pluviometer
     */
    private case class RandomPluviometer[Z](override val zone: Z, override val position: Point2D, signalProbability: Double)
      extends Pluviometer[Z](zone, position, 1 - signalProbability):
        override def measure(): Unit = this._lastMeasurement = Math.random()
        override def toString: String = s"RandomPluviometer($position,${(signalProbability * 100).pretty}%)"