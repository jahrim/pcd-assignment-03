package mvc.domain

import mvc.domain.FireStation.*
import mvc.domain.{FireStation, Point2D, StateIn}

/**
 * Model a fire-station.
 * @param position the position of the fire-station
 */
case class FireStation(position: Point2D) extends StateIn[State](AVAILABLE)

/**
 * Companion object of [[FireStation]].
 */
object FireStation:
    export State.*
    enum State { case AVAILABLE, BUSY }