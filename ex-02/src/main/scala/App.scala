import _root_.cluster.AkkaCluster
import akka.actor.ProviderSelection.cluster
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import configuration.C
import actor.CityActor
import util.Point2D
import scala.util.Random

/**
 * The main application.
 */
object App:
    /** Starts the application. */
    @main def main(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start()
        CityActor(cluster)

    /** Starts the cluster of this application. */
    @main def startCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start();

    /** Creates a new node in the cluster of this application. */
    @main def joinCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"))
        cluster.join(Behaviors.empty)