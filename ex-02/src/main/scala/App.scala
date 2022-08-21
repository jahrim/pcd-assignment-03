import akka.actor.ProviderSelection.cluster
import akka.actor.typed.scaladsl.Behaviors
import _root_.cluster.AkkaCluster
import com.typesafe.config.ConfigFactory

/**
 * The main application.
 */
object App:
    /** Starts the application. */
    @main def main(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start();
        cluster.join(Behaviors.empty)

    /** Starts the cluster of this application. */
    @main def startCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start();

    /** Creates a new node in the cluster of this application. */
    @main def joinCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"));
        cluster.join(Behaviors.empty)