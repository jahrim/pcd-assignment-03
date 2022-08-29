package view

import actor.CityActor.Snapshot
import actor.ZoneActor.Zone.State
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.{Button, Label, TextArea, TextField}
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import scalafx.Includes.*
import actor.CityActor.ViewRef
import actor.ViewActor.DisableAlarm
import util.Point2D
import view.MainFXController.*
import _root_.cluster.AkkaCluster
import actor.ViewActor
import com.typesafe.config.ConfigFactory
import view.View.*

import java.net.URL
import java.util.ResourceBundle

//todo fix canvas borders

/**
 * Main controller of ths graphical user interface of this application.
 */
class MainFXController extends Initializable:
  @FXML var canvas: Canvas = _
  @FXML var description: TextArea = _
  @FXML var cityIdentifierInput: TextField = _
  @FXML var connectButton: Button = _
  @FXML var disableAlarmButton: Button = _
  @FXML var simulationLabel: Label = _
  var systemSnapshot: Option[SnapshotView] = Option.empty
  var clickedEntity: Option[Drawable[_]] = Option.empty
  var viewActor: Option[ViewRef] = Option.empty

  /** Close the application. */
  def exit(): Unit = Platform.exit()
  /**
   * Displays the specified snapshot of a system.
   * @param snapshot the specified snapshot
   */
  def display(snapshot: Snapshot): Unit = display(SnapshotView(snapshot, this.canvas))
  private def display(snapshot: SnapshotView): Unit = Platform.runLater(() => {
    // the first time the system is displayed...
    if systemSnapshot.isEmpty then
      this.cityIdentifierInput.text = "Connection successful"
      this.simulationLabel.text = "Simulation"
    // refresh the clicked entity with the entity of the new snapshot with the same id
    if this.clickedEntity.isDefined then this.clickedEntity = snapshot.searchById(this.clickedEntity.get)
    this.displayClickedEntity()
    // draw each entity
    val painter = canvas.getGraphicsContext2D
    painter.refresh(snapshot.toList.foreach(d => painter.colored(d.borderColor, d.color)(painter.draw(d))))
    this.systemSnapshot = Option(snapshot)
  })
  /** Displays the current clicked entity. */
  private def displayClickedEntity(): Unit =
    this.disableAlarmButton.disable = this.clickedEntity match
      case Some(entity) =>
        this.description.text = entity.toString
        entity match
          case zone: ZoneView if viewActor.isDefined => State.fromOrdinal(zone.state) != State.Monitored
          case _ => true
      case _ =>
        this.description.text = ""
        true

  override def initialize(url: URL, rb: ResourceBundle): Unit =
    this.disableAlarmButton.disable = true
    this.simulationLabel.text = "Connect to a city to display its state..."
    canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (event: MouseEvent) =>
      val clickedPosition: (Double, Double) = (event.getX, event.getY)
      systemSnapshot match
        case Some(snapshot) =>
          snapshot.toList.foreach(d => if d.shape.contains(clickedPosition) then this.clickedEntity = Option(d))
          this.displayClickedEntity()
        case _ => println(s"View: $clickedPosition")
    )

  @FXML private def connectButtonHandler(event: ActionEvent): Unit = this.cityIdentifierInput.text.get() match
    case "" => throw new IllegalArgumentException("You cannot connect to a city without specifying its identifier.")
    case cityId =>
      this.simulationLabel.text = "Connecting..."
      cluster.join(ViewActor(this, cityId))   //todo delegate to thread worker
      this.cityIdentifierInput.disable = true
      this.connectButton.disable = true
  @FXML private def disableAlarmButtonHandler(event: ActionEvent): Unit =
    this.viewActor.get ! DisableAlarm(this.clickedEntity.get.id)

/**
 * Companion object of [[MainFXController]].
 */
object MainFXController:
  private val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"))
  extension (g: GraphicsContext)
    /**
     * Draws the shape of the specified drawable in this graphic context.
     * @param s the specified drawable
     * @tparam S the type of the shape of the specified drawable
     */
    def draw[S <: Shape](d: Drawable[S]): Unit = g.draw(d.shape)
    /**
     * Draws the specified shape in this graphic context.
     * @param s the specified shape
     * @tparam S the type of the specified shape
     */
    def draw[S <: Shape](s: S): Unit = s match
      case r: Rectangle =>
        g.fillRect(r.position.x, r.position.y, r.width, r.height)
        g.strokeRect(r.position.x, r.position.y, r.width, r.height)
      case c: Circle =>
        g.fillOval(c.position.x - c.radius, c.position.y - c.radius, c.diameter, c.diameter)
        g.strokeOval(c.position.x - c.radius, c.position.y - c.radius, c.diameter, c.diameter)
      case _ => throw Error(s"The canvas does not know how to draw the requested shape: $s")
    /**
     * Executes the specified paint procedure temporarily setting the specified colors in this graphic context.
     * @param borderColor     the specified stroke color
     * @param backgroundColor the specified fill color
     * @param paint           the paint procedure to be executed
     */
    def colored(borderColor: Color = Color.BLACK, backgroundColor: Color = Color.BLACK)(paint: => Unit): Unit =
      val (previousBorderColor, previousBackgroundColor) = (g.getStroke, g.getFill)
      g.setStroke(borderColor)
      g.setFill(backgroundColor)
      paint
      g.setStroke(previousBorderColor)
      g.setFill(previousBackgroundColor)
    /**
     * Clears the canvas and executes the specified paint procedure.
     * @param paint the specified paint procedure
     */
    def refresh(paint: => Unit): Unit =
      g.clearRect(0, 0, g.getCanvas.getWidth, g.getCanvas.getHeight)
      paint