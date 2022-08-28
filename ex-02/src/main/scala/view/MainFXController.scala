package view

import actor.CityActor.Snapshot
import actor.ZoneActor.Zone.State
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.{Button, TextArea}
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import scalafx.Includes.*
import util.Point2D
import view.MainFXController.*
import view.View.*
import java.net.URL
import java.util.ResourceBundle

/**
 * Main controller of ths graphical user interface of this application.
 */
class MainFXController extends Initializable:
  @FXML var canvas: Canvas = _
  @FXML var description: TextArea = _
  @FXML var disableAlarmButton: Button = _
  var systemSnapshot: Option[SnapshotView] = Option.empty
  var clickedEntity: Option[Drawable[_]] = Option.empty

  /**
   * Displays the specified snapshot of a system.
   * @param snapshot the specified snapshot
   */
  def display(snapshot: Snapshot): Unit = display(SnapshotView(snapshot, this.canvas))
  private def display(snapshot: SnapshotView): Unit = Platform.runLater(() => {
    val painter = canvas.getGraphicsContext2D
    snapshot.foreach(d => painter.colored(d.borderColor, d.color)(painter.draw(d)))
    this.systemSnapshot = Option(snapshot)
    //todo test that the description of a clicked entity will be updated on the next snapshot (start)
    this.clickedEntity match
      case Some(entity) =>
        this.clickedEntity = snapshot.searchById(entity)
        this.description.text = entity.toString
      case _ => this.description.text = ""
    //todo test that the description of a clicked entity will be updated on the next snapshot (end)
  })

  override def initialize(url: URL, rb: ResourceBundle): Unit =
    this.disableAlarmButton.disable = true
    canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, (event: MouseEvent) => systemSnapshot match {
      case Some(snapshot) =>
        val clickedPosition = (event.getX, event.getY)
        snapshot.foreach(d => if d.shape.contains(clickedPosition) then this.clickedEntity = Option(d))
        this.disableAlarmButton.disable = this.clickedEntity match
          case Some(entity) =>
            this.description.text = entity.toString
            entity match
              case zone: ZoneView => State.fromOrdinal(zone.state) != State.Alarmed
              case _ => true
          case _ =>
            this.description.text = ""
            true
      case _ => println(s"View: ${(event.getX, event.getY)}")
    })
  @FXML private def disableAlarmButtonHandler(event: ActionEvent): Unit = println(event) // todo tell alarmed zone to consider its alarm as solved

/**
 * Companion object of [[MainFXController]].
 */
object MainFXController:
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