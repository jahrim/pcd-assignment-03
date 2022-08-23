package mvc

import mvc.domain.{FireStation, Pluviometer, Point2D}
import mvc.domain.Zone.ZoneProvider
import configuration.C

import java.time.temporal.TemporalQueries.zone
import scala.util.Random

/**
 * The model of this application.
 */
object Model:
    /**
     * Model a basic city implementation.
     * @param position the position of this city
     * @param width the width of this city
     * @param height the height of this city
     */
    case class City(override val position: Point2D, override val width: Double, override val height: Double)
      extends domain.City(position, width, height):
        override type Zone = Model.Zone

    /**
     * Companion object of [[City]].
     */
    object City:
        given ZoneProvider[Zone] = (position, width, height) => Zone(position, width, height, Random.nextInt(C.City.MAX_PLUVIOMETERS_PER_ZONE) + 1)
        /**
         * @param position the position of the city
         * @param width the width of the city
         * @param height the height of the city
         * @param numberOfZones the number of zones of the city
         * @return a new city divided into the specified number of zones.
         */
        def apply(position: Point2D, width: Double, height: Double, numberOfZones: Int): City =
            val city: City = City(position, width, height)
            city.addSubZones(Zone(position, width, height).splitInto(numberOfZones))
            city

    /**
     * Model a basic zone implementation.
     * @param position the position of the top left corner of this zone
     * @param width the width of this zone
     * @param height the height of this zone
     */
    case class Zone(override val position: Point2D, override val width: Double, override val height: Double)
      extends domain.Zone(position, width, height):
        override type FireStation = domain.FireStation[Zone]
        override type Pluviometer = domain.Pluviometer[Zone]

    /**
     * Companion object of [[Zone]].
     */
    object Zone:
        def apply(position: Point2D, width: Double, height: Double, numberOfPluviometers: Int): Zone =
            val zone = Zone(position, width, height)
            zone.installPluviometers((0 until numberOfPluviometers).map(_ => createPluviometerIn(zone)))
            zone.addFireStations(createFireStationIn(zone))
            zone

        private[Zone] def createPluviometerIn(zone: Zone): Pluviometer[Zone] =
            Pluviometer.withRandomMeasurements(zone, zone.randomPosition, C.City.PLUVIOMETER_SIGNAL_PROBABILITY)
        private[Zone] def createFireStationIn(zone: Zone): FireStation[Zone] = FireStation(zone, zone.randomPosition)