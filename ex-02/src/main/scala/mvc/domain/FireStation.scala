package mvc.domain

import mvc.domain.FireStation.*
import mvc.domain.{FireStation, Point2D, StateIn}

/**
 * Model a fire-station in the specified zone.
 * @param zone the specified zone
 * @param position the position of this fire-station in the specified zone
 */
case class FireStation[Z](zone: Z, position: Point2D) extends StateIn[State](AVAILABLE):
    override def toString: String = s"FireStation($position)"

/**
 * Companion object of [[FireStation]].
 */
object FireStation:
    export State.*
    enum State { case AVAILABLE, BUSY }