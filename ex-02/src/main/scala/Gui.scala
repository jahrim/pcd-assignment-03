import actor.CityActor.Snapshot
import javafx.{fxml as jfxf, scene as jfxs}
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import view.MainFXController

import java.io.IOException

/**
 * A graphical user interface used to interact with the system.
 */
object Gui extends JFXApp3:
  private val mainResource: String = "main.fxml"
  override def start(): Unit =
    val resource = getClass.getResource(mainResource)
    if (resource == null) { throw new IOException(s"Cannot load resource: $mainResource") }
    val loader = new jfxf.FXMLLoader(resource)
    val root: jfxs.Parent = loader.load[jfxs.Parent]
    val controller = loader.getController[MainFXController]

    //todo remove (start)
    controller.display(Snapshot.random)
    //todo remove (end)

    stage = new PrimaryStage() {
      title = "FXML GridPane Demo"
      scene = new Scene(root)
    }