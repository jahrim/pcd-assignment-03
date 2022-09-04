import actor.CityActor
import actor.CityActor.City
import akka.actor.typed.scaladsl.Behaviors
import cluster.AkkaCluster
import com.typesafe.config.ConfigFactory

/**
 * Creates a new city in the cluster of this application.
 * Note that the cluster should be started before executing this procedure.
 */
object CreateCity:
  def main(args: Array[String]): Unit =
    val cluster: AkkaCluster = AkkaCluster(ConfigFactory.load("cluster"))
    if args.isEmpty then CityActor(cluster) else CityActor(cluster, City(), args.toSeq.map(_.toInt).head)
    println("City created!")