package mvc.domain

import mvc.domain.Point2D
import configuration.C.Log.*
import java.util.Locale

/**
 * Model a rectangular zone in a two dimensional space.
 * @param position the position of this city
 * @param width the width of this city
 * @param height the height of this city
 */
abstract class City(val position: Point2D, val width: Double, val height: Double):
    type Zone
    private var _subZones: List[Zone] = List()

    /** @return the zones this city is divided into. */
    def subZones: List[Zone] = this._subZones
    /**
     * Adds the specified sub-zones to this city.
     * @param zs the specified sub-zones
     */
    def addSubZones(zs: Iterable[Zone]): Unit = this._subZones = zs.toList ::: this._subZones
    /** As [[addSubZones addSubZones(Iterable[Zone])]]. */
    def addSubZones(zs: Zone*): Unit = this.addSubZones(zs)
    /**
     * Removes the specified sub-zones to this city.
     * @param zs the specified sub-zones
     */
    def removeSubZones(zs: Iterable[Zone]): Unit = this._subZones = this._subZones.filterNot(zs.toList.contains)
    /** As [[removeSubZones removeSubZones(Iterable[Zone])]]. */
    def removeSubZones(zs: Zone*): Unit = this.removeSubZones(zs)

    override def toString = s"City($position,${width.pretty},${height.pretty})})"