package mvc.domain

import mvc.domain.Zone.*
import mvc.domain.{Point2D, Zone}
import configuration.C.Log.*

import java.time.temporal.TemporalQueries.zone
import java.util.Locale
import scala.::
import scala.util.Random

/**
 * Model a rectangular zone in a two dimensional space.
 * @param position the position of the top left corner of this zone
 * @param width the width of this zone
 * @param height the height of this zone
 */
abstract class Zone(position: Point2D, width: Double, height: Double) extends ZoneArea(position, width, height) with StateIn[State](OK):
    type FireStation
    type Pluviometer
    private var _pluviometers: List[Pluviometer] = List()
    private var _fireStations: List[FireStation] = List()

    /** @return the list of pluviometers in this zone. */
    def pluviometers: List[Pluviometer] = this._pluviometers
    /**
     * Installs the specified pluviometers in this zone.
     * @param ps the specified pluviometers
     */
    def installPluviometers(ps: Iterable[Pluviometer]): Unit = this._pluviometers = ps.toList ::: this._pluviometers
    /** As [[installPluviometers installPluviometers(Iterable[Pluviometer])]]. */
    def installPluviometers(ps: Pluviometer*): Unit = this.installPluviometers(ps)
    /**
     * Uninstalls the specified pluviometers in this zone.
     * @param ps the specified pluviometers
     */
    def uninstallPluviometers(ps: Iterable[Pluviometer]): Unit = this._pluviometers = this._pluviometers.filterNot(ps.toList.contains)
    /** As [[uninstallPluviometers uninstallPluviometers(Iterable[Pluviometer])]]. */
    def uninstallPluviometers(ps: Pluviometer*): Unit = this.uninstallPluviometers(ps)

    /** @return the list of fire-stations in this zone. */
    def fireStations: List[FireStation] = this._fireStations
    /**
     * Adds the specified fire-stations to this zone.
     * @param fs the specified fire-stations
     */
    def addFireStations(fs: Iterable[FireStation]): Unit = this._fireStations = fs.toList ::: this._fireStations
    /** As [[addFireStations addFireStations(Iterable[FireStation])]]. */
    def addFireStations(fs: FireStation*): Unit = this.addFireStations(fs)
    /**
     * Removes the specified fire-stations to this zone.
     * @param fs the specified fire-stations
     */
    def removeFireStations(fs: Iterable[FireStation]): Unit = this._fireStations = this._fireStations.filterNot(fs.toList.contains)
    /** As [[removeFireStations removeFireStations(Iterable[FireStation])]]. */
    def removeFireStations(fs: FireStation*): Unit = this.removeFireStations(fs)

    /**
     * Divides this zone in the specified number of sub-zones, randomly created.
     * @param numberOfSubZones the specified number of sub-zones
     * @param provider the provider that will be used to create the sub-zones
     * @return a list of the sub-zones of this zone
     */
    def splitInto[Z <: Zone](numberOfSubZones: Int)(using provider: ZoneProvider[Z]): List[Z] =
        super.splitInto(numberOfSubZones).map(area => provider(area.position, area.width, area.height))

/**
 * Companion object of [[Zone]].
 */
object Zone:
    export State.*
    enum State { case OK, ALARM, UNDER_CONTROL }
    type ZoneProvider[Z <: Zone] = (Point2D, Double, Double) => Z

    private[Zone] enum SplitDirection { case HORIZONTAL, VERTICAL }
    private[Zone] class ZoneArea(val position: Point2D, val width: Double, val height: Double):
        /**
         * Divides this zone area in the specified number of sub-zones, randomly created.
         * @param numberOfSubZones the specified number of sub-zones
         * @return a list of the zone areas of the newly created sub-zones
         */
        def splitInto(numberOfSubZones: Int): List[ZoneArea] =
            def splitIntoDirectional(zone: ZoneArea, subZones: Int, direction: SplitDirection): List[ZoneArea] =
                val splitPoint: Point2D = zone.randomPosition
                val surplus: (Int, Int) = if subZones % 2 == 0 then (0, 0) else Seq((0, 1), (1, 0))(Random.nextInt(2))
                if subZones > 1 then direction match
                    case SplitDirection.HORIZONTAL =>
                        val top: ZoneArea = new ZoneArea(zone.position, zone.width, zone.height - splitPoint.y)
                        val bottom: ZoneArea = new ZoneArea(zone.position.relocateY(splitPoint.y), zone.width, splitPoint.y)
                        splitIntoDirectional(top, subZones / 2 + surplus._1, SplitDirection.VERTICAL)
                        :::
                        splitIntoDirectional(bottom, subZones / 2 + surplus._2, SplitDirection.VERTICAL)
                    case SplitDirection.VERTICAL =>
                        val left: ZoneArea = new ZoneArea(zone.position, splitPoint.x, zone.height)
                        val right: ZoneArea = new ZoneArea(zone.position.relocateX(splitPoint.x), zone.width - splitPoint.x, zone.height)
                        splitIntoDirectional(left, subZones / 2 + surplus._1, SplitDirection.HORIZONTAL)
                        :::
                        splitIntoDirectional(right, subZones / 2 + surplus._2, SplitDirection.HORIZONTAL)
                else List(zone)
            splitIntoDirectional(this, numberOfSubZones,  Seq(SplitDirection.HORIZONTAL, SplitDirection.VERTICAL)(Random.nextInt(2)))

        /** @return a random point inside this zone area. */
        def randomPosition: Point2D = (this.position.x + Math.random() * this.width, this.position.y - Math.random() * this.height)

        override def toString = s"ZoneArea($position,${width.pretty},${height.pretty})"