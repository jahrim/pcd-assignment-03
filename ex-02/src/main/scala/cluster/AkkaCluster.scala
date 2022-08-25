package cluster

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Address, AddressFromURIString}
import akka.cluster.ClusterEvent.*
import akka.cluster.typed.{Cluster, Subscribe}
import com.typesafe.config.{Config, ConfigFactory}

import scala.jdk.CollectionConverters.*

/**
 * Model an akka cluster with the specified configuration.
 * @param config the specified configuration
 */
class AkkaCluster(private val config: Config):
    private val seedNodes: List[Address] = config.getStringList("akka.cluster.seed-nodes").asScala.toList.map(AddressFromURIString.parse)

    /**
     * Starts the seed nodes of this cluster, creating the cluster.
     * @throws AkkaCluster.ClusterConfigurationException if the address of a seed node could not be parsed from the configuration of this cluster
     * @return this
     */
    @throws(classOf[ClusterConfigurationException])
    def start(): AkkaCluster =
        this.seedNodes.foreach { case Address(_, _, Some(host), Some(port)) => this.join(ClusterLogger(), host, port); case _ => throw ClusterConfigurationException() }
        this

    /**
     * Joins the specified actor as a node of this cluster, deploying it in the specified host.
     * Note that the cluster should be started before performing this operation.
     * @param behavior the specified actor
     * @param hostName the name of the specified host
     * @param port the port of the specified host
     * @tparam A the type of messages that can be handled by the specified actor
     * @return a reference to the specified actor
     */
    def join[A](behavior: Behavior[A], hostName: String = "127.0.0.1", port: Int = 0): ActorRef[A] =
        ActorSystem[A](
            behavior,
            this.seedNodes.head.system,
            ConfigFactory.parseString(
                s"""
                   | akka.remote.artery.canonical: {
                   |    hostname: "$hostName",
                   |    port: $port
                   | }
                   |""".stripMargin.replace("\n", " ")
            ).withFallback(this.config)
        );

    /**
     * Model an actor that logs cluster events.
     */
    object ClusterLogger:
        import Event.*
        enum Event:
            case ReachabilityChange(reachabilityEvent: ReachabilityEvent)
            case MemberChange(event: MemberEvent)

        def apply(): Behavior[Event] = Behaviors.setup { ctx =>
            val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MemberChange.apply)
            Cluster(ctx.system).subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

            val reachabilityAdapter: ActorRef[ReachabilityEvent] = ctx.messageAdapter(ReachabilityChange.apply)
            Cluster(ctx.system).subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

            Behaviors.receiveMessage { message =>
                message match {
                    case ReachabilityChange(reachabilityEvent) =>
                        reachabilityEvent match {
                            case UnreachableMember(member) => ctx.log.info("Member detected as unreachable: {}", member)
                            case ReachableMember(member) => ctx.log.info("Member back to reachable: {}", member)
                        }
                    case MemberChange(changeEvent) =>
                        changeEvent match {
                            case MemberUp(member) => ctx.log.info("Member is Up: {}", member.address)
                            case MemberRemoved(member, previousStatus) => ctx.log.info("Member is Removed: {} after {}", member.address, previousStatus)
                            case _: MemberEvent =>
                        }
                }
                Behaviors.same
            }
        }

    /**
     * Model an exception to be thrown if a cluster configuration is not correct.
     */
    class ClusterConfigurationException() extends RuntimeException