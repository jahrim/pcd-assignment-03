import akka.actor.ProviderSelection.cluster
import akka.actor.typed.scaladsl.Behaviors
import _root_.cluster.AkkaCluster
import com.typesafe.config.ConfigFactory
import mvc.Model
import mvc.Model.*
import mvc.domain.{FireStation, Pluviometer, Point2D}
import configuration.C

import scala.util.Random

/**
 * The main application.
 */
object App:
    /** Starts the application. */
    @main def main(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start()
        cluster.join(Behaviors.empty)

    /** Starts the cluster of this application. */
    @main def startCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster")).start();

    /** Creates a new node in the cluster of this application. */
    @main def joinCluster(): Unit =
        val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"))
        cluster.join(Behaviors.empty)

    /** Creates and print a possible instance of the model. */
    @main def modelSetup(): Unit =
        val city: City = City((0D, C.City.Size.HEIGHT), C.City.Size.WIDTH, C.City.Size.HEIGHT, C.City.NUMBER_OF_ZONES)
        println(s"$city\n")
        city.subZones.foreach(zone =>
            println(zone)
            println(zone.pluviometers)
            println(s"${zone.fireStations}\n")
        )