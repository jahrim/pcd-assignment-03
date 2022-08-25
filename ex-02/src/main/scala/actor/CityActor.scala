package actor

import actor.*
import actor.CityActor.Message.*
import actor.FireStationActor.FireStation
import actor.PluviometerActor.Pluviometer
import actor.ZoneActor.Zone
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import cluster.AkkaCluster
import configuration.C.City.*
import configuration.C.Pluviometer.*
import configuration.C.Zone.MAX_PLUVIOMETERS_PER_ZONE
import sun.tools.jconsole.ProxyClient.Snapshot
import util.Point2D

import scala.util.Random

/**
 * Model the actor for a city.
 */
object CityActor:
  /**
   * Service key used to access this city actor. Note that only a single instance of this
   * actor is meant to be up in the same cluster.
   */
  val serviceKey: ServiceKey[Message] = ServiceKey[Message]("CityActor")

  type CityRef = ActorRef[Message]
  type ZoneRef = ActorRef[ZoneActor.Message]
  type PluviometerRef = ActorRef[PluviometerActor.Message]
  type FireStationRef = ActorRef[FireStationActor.Message]

  //todo use CborSerializable
  /**
   * Model the messages of a city actor.
   */
  enum Message:
    /** Tells this city actor that the specified pluviometer has the specified state. */
    case NotifyPluviometerState(actorRef: PluviometerRef, state: Pluviometer)
    /** Tells this city actor that the specified fire-station has the specified state. */
    case NotifyFireStationState(actorRef: FireStationRef, state: FireStation)
    /** Tells this city actor that the specified zone has the specified state. */
    case NotifyZoneState(actorRef: ZoneRef, state: Zone)
    /** Tells this city actor to take a snapshot of the system. */
    private[CityActor] case TakeSnapshot

  /**
   * @param cluster the cluster used by this city actor to deploy itself and his children
   * @param city the initial state of this city actor
   * @param numberOfZones the number of zones this city should be partitioned into
   */
  def apply(cluster: AkkaCluster, city: City = City(), numberOfZones: Int = NUMBER_OF_ZONES): Behavior[Message] =
    var firstSnapshot: Snapshot = Snapshot()
    city.asZone.splitInto(numberOfZones) foreach { zone =>
      val pluviometerActors = List.fill(Random.nextInt(MAX_PLUVIOMETERS_PER_ZONE - 1) + 1)(
        cluster.join(PluviometerActor(Pluviometer.withRandomMeasurements(zone.randomPosition, PLUVIOMETER_SIGNAL_PROBABILITY)))
      )
      val fireStationActors = List.fill(1)(cluster.join(FireStationActor(FireStation.random(zone))))
      val zoneActor = cluster.join(ZoneActor(zone, pluviometerActors, fireStationActors))
      pluviometerActors foreach { p => firstSnapshot = firstSnapshot.updatePluviometer(p) }
      fireStationActors foreach { f => firstSnapshot = firstSnapshot.updateFireStation(f) }
      firstSnapshot = firstSnapshot.updateZone(zoneActor)
    }
    Behaviors.setup { context =>
      context.system.receptionist ! Register(serviceKey, context.self)
      Active(city, firstSnapshot)
    }

  /** Behavior where the city takes a snapshot of itself periodically, notifying the view in the process. */
  private[CityActor] object Active:
    def apply(city: City, snapshot: Snapshot): Behavior[Message] =
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(TakeSnapshot, TakeSnapshot, SNAPSHOT_PERIOD)
        Behaviors.receiveMessage {
          case TakeSnapshot =>
            //todo send snapshot to view
            snapshot.pluviometers.keys.foreach(_ ! PluviometerActor.Message.TakeSnapshot)
            snapshot.fireStations.keys.foreach(_ ! FireStationActor.Message.TakeSnapshot)
            snapshot.zones.keys.foreach(_ ! ZoneActor.Message.TakeSnapshot)
            Behaviors.same
          case NotifyPluviometerState(actor, state) => Active(city, snapshot.updatePluviometer(actor, Option(state)))
          case NotifyFireStationState(actor, state) => Active(city, snapshot.updateFireStation(actor, Option(state)))
          case NotifyZoneState(actor, state) => Active(city, snapshot.updateZone(actor, Option(state)))
        }
      }

  /**
   * Model the state of a city actor.
   *
   * @param position the position of the city
   * @param width    the width of the city
   * @param height   the height of the city
   */
  case class City(position: Point2D = (0D, Size.HEIGHT), width: Double = Size.WIDTH, height: Double = Size.HEIGHT):
    def asZone: Zone = Zone(position, width, height)

  /**
   * Model a snapshot of all the actors inside a city with their state.
   *
   * @param pluviometers a map from the pluviometer actors to their state
   * @param fireStations a map from the firestation actors to their state
   * @param zones        a map from the zone actors to their state
   */
  case class Snapshot(
    pluviometers: Map[PluviometerRef, Option[Pluviometer]] = Map(),
    fireStations: Map[FireStationRef, Option[FireStation]] = Map(),
    zones: Map[ZoneRef, Option[Zone]] = Map()
  ):
    /**
     * @param actorRef the specified pluviometer actor
     * @param state    the specified state
     * @return a new snapshot of the system where the specified pluviometer actor is bound to the specified state
     */
    def updatePluviometer(actorRef: PluviometerRef, state: Option[Pluviometer] = Option.empty): Snapshot =
      Snapshot(this.pluviometers + (actorRef -> state), this.fireStations, this.zones)
    /**
     * @param actorRef the specified pluviometer actor
     * @param state    the specified state
     * @return a new snapshot of the system where the specified fire-station actor is bound to the specified state
     */
    def updateFireStation(actorRef: FireStationRef, state: Option[FireStation] = Option.empty): Snapshot =
      Snapshot(this.pluviometers, this.fireStations + (actorRef -> state), this.zones)

    /**
     * @param actorRef the specified pluviometer actor
     * @param state    the specified state
     * @return a new snapshot of the system where the specified zone actor is bound to the specified state
     */
    def updateZone(actorRef: ZoneRef, state: Option[Zone] = Option.empty): Snapshot =
      Snapshot(this.pluviometers, this.fireStations, this.zones + (actorRef -> state))