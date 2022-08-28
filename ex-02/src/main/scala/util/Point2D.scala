package util

import cluster.message.CborSerializable
import configuration.C.Log.*

/**
 * Model a point in two dimensions.
 * @param x the value of the first coordinate
 * @param y the value of the second coordinate
 */
case class Point2D(x: Double, y: Double) extends CborSerializable:
    /**
     * @param p the specified point
     * @return the distance between this point and the specified point
     */
    def distance(p: Point2D): Double = Math.sqrt((this.x - p.x) * (this.x - p.x) + (this.y - p.y) * (this.y - p.y))
    /**
     * @param x the horizontal amount
     * @param y the vertical amount
     * @return the point result of the translation of this point by the specified amounts.
     */
    def translate(x: Double, y: Double): Point2D = Point2D(this.x + x, this.y + y)
    /** As [[translate translate(x,0)]].*/
    def translateX(x: Double): Point2D = Point2D(this.x + x, this.y)
    /** As [[translate translate(0,y)]].*/
    def translateY(y: Double): Point2D = Point2D(this.x, this.y + y)
    /**
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     * @return the point result of the relocation of this point to the specified coordinates.
     */
    def relocate(x: Double, y: Double): Point2D = Point2D(x, y)
    /** As [[relocate relocate(x,this.y)]]. */
    def relocateX(x: Double): Point2D = Point2D(x, this.y)
    /** As [[relocate relocate(this.x,y)]]. */
    def relocateY(y: Double): Point2D = Point2D(this.x, y)
    override def toString = s"Point(${x.pretty},${y.pretty})"

/**
 * Companion object of [[Point2D]].
 */
object Point2D:
    given Conversion[(Double, Double), Point2D] = coordinates => Point2D(coordinates._1, coordinates._2)