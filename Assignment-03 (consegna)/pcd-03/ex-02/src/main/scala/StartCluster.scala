import cluster.AkkaCluster
import actor.CityActor
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

/**
 * Start the city cluster of this application.
 */
object StartCluster:
  def main(args: Array[String]): Unit =
    AkkaCluster(ConfigFactory.load("cluster")).start()
    println("Cluster is up!")