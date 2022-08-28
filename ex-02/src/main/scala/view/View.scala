package view

import actor.CityActor.{City, CityData, Snapshot}
import actor.FireStationActor.{FireStation, FireStationData}
import actor.PluviometerActor.{Pluviometer, PluviometerData}
import actor.ZoneActor.Zone.State
import actor.ZoneActor.Zone.State.{Alarmed, Calm, Monitored}
import actor.ZoneActor.{Zone, ZoneData}
import configuration.C.Log.pretty
import configuration.C.View.*
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import util.Point2D
import view.View.*

/**
 * Model the view of this application.
 */
object View:
  /**
   * Model a space with two dimensions.
   * @param width the horizontal dimension
   * @param height the vertical dimension
   */
  case class Size2D(width: Double, height: Double)
  /**
   * Model a mapping function for an entity from a space to another.
   * @tparam S the type of the entity
   */
  @FunctionalInterface
  trait Mapping2D[S]:
    def transform(fromSpace: Size2D)(toSpace: Size2D)(shape: S): S
    def inverse(fromSpace: Size2D)(toSpace: Size2D)(shape: S): S = transform(toSpace)(fromSpace)(shape)

  given Conversion[Canvas, Size2D] = canvas => Size2D(canvas.getWidth, canvas.getHeight)
  given Conversion[CityData, Size2D] = city => Size2D(city.width, city.height)
  given Conversion[(Double, Double), Size2D] = dimensions => Size2D(dimensions._1, dimensions._2)

  /* Mappings of shapes from the logic space to the view space. */
  given Mapping2D[Point2D] with
    override def transform(view: Size2D)(city: Size2D)(cityPoint: Point2D): Point2D =
      Point2D(cityPoint.x / city.width * view.width, (1 - cityPoint.y / city.height) * view.height)
  given Mapping2D[Rectangle] with
    override def transform(view: Size2D)(city: Size2D)(cityRectangle: Rectangle): Rectangle =
      Rectangle(
        mapToView(view)(city)(cityRectangle.position),
        cityRectangle.width / city.width * view.width,
        cityRectangle.height / city.height * view.height
      )

  /**
   * Map the specified shape from the specified logic space to the specified view space.
   * @param view       the specified view space
   * @param logic      the specified logic space
   * @param logicShape the specified shape
   * @tparam S the type of the shape
   * @return the specified shape inside the view space
   */
  def mapToView[S: Mapping2D](view: Size2D)(logic: Size2D)(logicShape: S): S = summon[Mapping2D[S]].transform(view)(logic)(logicShape)
  /**
   * Map the specified shape from the specified view space to the specified logic space.
   * @param view       the specified view space
   * @param logic      the specified logic space
   * @param viewShape  the specified shape
   * @tparam S the type of the shape
   * @return the specified shape inside the logic space
   */
  def mapToLogic[S: Mapping2D](view: Size2D)(logic: Size2D)(viewShape: S): S = summon[Mapping2D[S]].inverse(view)(logic)(viewShape)

  /**
   * Model an entity with a shape that can be drawn.
   * @tparam S the type of shape of this entity
   */
  trait Drawable[S <: Shape]:
    /** @return the shape of this entity. */
    def shape: S
    /** @return the color of the border of the shape of this entity. */
    def borderColor: Color = Color.TRANSPARENT
    /** @return the color of the area of the shape of this entity. */
    def color: Color = Color.TRANSPARENT

  /**
   * Model a shape with a reference position.
   */
  trait Shape:
    /** @return the reference position of this shape. */
    def position: Point2D
    /**
     * @param p the specified point
     * @return true if this shape contains the specified point.
     */
    def contains(p: Point2D): Boolean

  /**
   * Model a view representing the data of a city.
   * @param c     the data of the city
   * @param shape the shape of the city
   */
  case class CityView(private val c: CityData, shape: Rectangle) extends Drawable[Rectangle]:
    export c.{id, position, width, height}

  /**
   * Model a view representing the data of a pluviometer.
   * @param c     the data of the pluviometer
   * @param shape the shape of the pluviometer
   */
  case class PluviometerView(private val p: PluviometerData, shape: Circle) extends Drawable[Circle]:
    export p.{id, position, signal, lastMeasurement, threshold}
    override def borderColor: Color = Colors.ENTITY_BORDER
    override def color: Color = Colors.PLUVIOMETER
    override def toString: String =
      s"""Pluviometer:
          |  id: ${p.id}
          |  position: ${p.position}
          |  signal: ${p.signal}
          |  measurement: ${p.lastMeasurement.pretty}
          |  threshold: ${p.threshold.pretty}
       """.stripMargin

  /**
   * Model a view representing the data of a fire-station.
   * @param c     the data of the fire-station
   * @param shape the shape of the fire-station
   */
  case class FireStationView(private val f: FireStationData, shape: Circle) extends Drawable[Circle]:
    import FireStation.State
    export f.{id, position, state}
    override def borderColor: Color = Colors.ENTITY_BORDER
    override def color: Color = State.fromOrdinal(f.state) match
      case State.Available => Colors.FIRESTATION_AVAILABLE
      case State.Busy => Colors.FIRESTATION_BUSY
    override def toString: String =
      s"""FireStation:
          |  id: ${f.id}
          |  position: ${f.position}
          |  state: ${State.fromOrdinal(f.state)}
       """.stripMargin

  /**
   * Model a view representing the data of a zone.
   * @param c     the data of the zone
   * @param shape the shape of the zone
   */
  case class ZoneView(private val z: ZoneData, shape: Rectangle) extends Drawable[Rectangle]:
    import Zone.State
    export z.{id, position, width, height, state}
    override def borderColor: Color = Colors.ZONE_BORDER
    override def color: Color = State.fromOrdinal(z.state) match
      case State.Calm => Colors.ZONE_CALM
      case State.Alarmed => Colors.ZONE_ALARMED
      case State.Monitored => Colors.ZONE_MONITORED
    override def toString: String =
      s"""Zone:
          |  id: ${z.id}
          |  position: ${z.position}
          |  width: ${z.width.pretty}
          |  height: ${z.height.pretty}
          |  state: ${State.fromOrdinal(z.state)}
       """.stripMargin

  /**
   * Model the view representing the data of a snapshot of the system.
   * @param snapshot the snapshot of the system
   * @param canvas   the canvas where this snapshot will be shown
   */
  case class SnapshotView(private val snapshot: Snapshot, private val canvas: Canvas):
    val cityView: CityView = CityView(snapshot.cityData, mapToView(canvas)(snapshot.cityData)(
      Rectangle(snapshot.cityData.position, snapshot.cityData.width, snapshot.cityData.height)
    ))
    val pluviometerViews: Map[String, PluviometerView] = snapshot.pluviometerDatas.transform((_,p) =>
      PluviometerView(p, Circle(mapToView(canvas)(snapshot.cityData)(p.position), ENTITY_RADIUS_PX))
    )
    val fireStationViews: Map[String, FireStationView] = snapshot.fireStationDatas.transform((_,f) =>
      FireStationView(f, Circle(mapToView(canvas)(snapshot.cityData)(f.position), ENTITY_RADIUS_PX))
    )
    val zoneViews: Map[String, ZoneView] = snapshot.zoneDatas.transform((_,z) =>
      ZoneView(z, mapToView(canvas)(snapshot.cityData)(Rectangle(z.position, z.width, z.height)))
    )
    /**
     * Consumes the city, the zones, the pluviometers and the fire-stations of this snapshot with the specified consumer.
     * @param consumer the specified consumer
     */
    def foreach(consumer: Drawable[_] => Unit): Unit =
      consumer(cityView)
      zoneViews.values.foreach(consumer)
      pluviometerViews.values.foreach(consumer)
      fireStationViews.values.foreach(consumer)
    /**
     * Consumes the city, the zones, the pluviometers and the fire-stations of this snapshot with the specified consumer.
     * @param consumer the specified consumer
     */
    def searchById(entity: Drawable[_]): Option[Drawable[_]] = entity match
      case city: City => if (city.id == cityView.id) then Option(cityView) else Option.empty
      case zone: Zone => zoneViews.get(zone.id)
      case pluviometer: Pluviometer => pluviometerViews.get(pluviometer.id)
      case fireStation: FireStation => fireStationViews.get(fireStation.id)
      case _ => Option.empty

  /**
   * Model a circle.
   * @param center the center of this circle
   * @param radius the radius of this circle
   */
  case class Circle(private val center: Point2D, radius: Double) extends Shape:
    private val _diameter: Double = this.radius * 2
    def diameter: Double = this._diameter
    override def position: Point2D = this.center
    override def contains(p: Point2D): Boolean = this.center.distance(p) <= radius
  /**
   * Model a rectangle.
   * @param topLeft the top-left corner of this rectangle
   * @param width   the width of this rectangle
   * @param height  the height of this rectangle
   */
  case class Rectangle(private val topLeft: Point2D, width: Double, height: Double) extends Shape:
    override def position: Point2D = this.topLeft
    override def contains(p: Point2D): Boolean =
      p.x >= this.topLeft.x && p.x <= this.topLeft.x + this.width &&
      p.y >= this.topLeft.y && p.y <= this.topLeft.y + this.height