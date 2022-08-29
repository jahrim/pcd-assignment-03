import cluster.AkkaCluster
import actor.CityActor
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

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
    /** Creates a new city in the cluster of this application. */
    @main def joinCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"))
        CityActor(cluster)