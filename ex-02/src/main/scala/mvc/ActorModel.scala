package mvc

import akka.actor.typed.ActorRef
import configuration.C
import mvc.actor.*
import mvc.domain.Zone.ZoneProvider
import mvc.domain.{FireStation, Pluviometer, Point2D}

import java.time.temporal.TemporalQueries.zone
import scala.util.Random

/**
 * The model of this application.
 */
object ActorModel:
    /**
     * Model a basic city implementation.
     * @param position the position of this city
     * @param width the width of this city
     * @param height the height of this city
     */
    case class City(override val position: Point2D, override val width: Double, override val height: Double)
      extends domain.City(position, width, height):
        override type Zone = ActorRef[ZoneActor.Message]

    /**
     * Model a basic zone implementation.
     * @param position the position of the top left corner of this zone
     * @param width the width of this zone
     * @param height the height of this zone
     */
    case class Zone(override val position: Point2D, override val width: Double, override val height: Double)
      extends domain.Zone(position, width, height):
        override type FireStation = ActorRef[FireStationActor.Message]
        override type Pluviometer = ActorRef[PluviometerActor.Message]