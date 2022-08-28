package actor

import actor.*
import actor.CityActor.*
import actor.FireStationActor.{FireStation, FireStationData}
import actor.PluviometerActor.{Pluviometer, PluviometerData}
import actor.ZoneActor.{Zone, ZoneData}
import akka.actor.typed.receptionist.Receptionist.*
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior}
import cluster.AkkaCluster
import cluster.message.CborSerializable
import configuration.C.City.*
import configuration.C.Log.pretty
import configuration.C.Pluviometer.*
import configuration.C.Zone.{MAX_PLUVIOMETERS_PER_ZONE, RANDOM_POSITION_PADDING}
import util.{Id, Point2D}

import scala.util.Random

/**
 * Model the actor for a city.
 */
object CityActor:
  type CityRef = ActorRef[Message]
  type ZoneRef = ActorRef[ZoneActor.Message]
  type PluviometerRef = ActorRef[PluviometerActor.Message]
  type FireStationRef = ActorRef[FireStationActor.Message]

  /**
   * Model the messages of a city actor.
   */
  trait Message extends CborSerializable
  /** Tells this city actor to update the state of the specified pluviometer. */
  case class NotifyPluviometerState(state: PluviometerData) extends Message
  /** Tells this city actor to update the state of the specified fire-station. */
  case class NotifyFireStationState(state: FireStationData) extends Message
  /** Tells this city actor to update the state of the specified zone. */
  case class NotifyZoneState(state: ZoneData) extends Message
  /** Tells this city actor to take a snapshot of the system. */
  private[CityActor] case object TakeSnapshot extends Message

  /**
   * @param cluster       the cluster used by this city actor to deploy itself and his children
   * @param city          the initial state of this city actor
   * @param numberOfZones the number of zones this city should be partitioned into
   */
  def apply(cluster: AkkaCluster, city: City = City(), numberOfZones: Int = NUMBER_OF_ZONES): Unit =
    val snapshot: Snapshot = Snapshot(city.data)
    city.asZone.splitInto(numberOfZones) foreach { zone =>
      val pluviometers = List.fill(Random.nextInt(MAX_PLUVIOMETERS_PER_ZONE) + 1)(
        Pluviometer.withRandomMeasurements(zone.randomPosition(RANDOM_POSITION_PADDING), PLUVIOMETER_SIGNAL_PROBABILITY)
      )
      val fireStations = List.fill(1)(FireStation.random(zone))
      pluviometers foreach { p =>
        snapshot.pluviometerDatas = snapshot.pluviometerDatas + (p.id -> p.data)
        cluster.join(PluviometerActor(p, city.id))
      }
      fireStations foreach { f =>
        snapshot.fireStationDatas = snapshot.fireStationDatas + (f.id -> f.data)
        cluster.join(FireStationActor(f, city.id))
      }
      snapshot.zoneDatas = snapshot.zoneDatas + (zone.id -> zone.data)
      cluster.join(ZoneActor(zone, city.id, pluviometers.map(_.id), fireStations.map(_.id)))
    }
    cluster.join(
      Behaviors.setup[Message] { context =>
        context.system.receptionist ! Register(ServiceKey[Message](city.id), context.self)
        val children: CityChildren = CityChildren()
        children.pluviometers = snapshot.pluviometerDatas.keys.map(p => context.spawnAnonymous(Routers.group(ServiceKey[PluviometerActor.Message](p)))).toList
        children.fireStations = snapshot.fireStationDatas.keys.map(f => context.spawnAnonymous(Routers.group(ServiceKey[FireStationActor.Message](f)))).toList
        children.zones = snapshot.zoneDatas.keys.map(z => context.spawnAnonymous(Routers.group(ServiceKey[ZoneActor.Message](z)))).toList
        Active(city, children, snapshot)
      }
    )

  /** Behavior where the city takes a snapshot of itself periodically, notifying the view in the process. */
  private[CityActor] object Active:
    def apply(city: City, children: CityChildren, snapshot: Snapshot): Behavior[Message] =
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(TakeSnapshot, TakeSnapshot, SNAPSHOT_PERIOD)
        Behaviors.receiveMessage {
          case TakeSnapshot =>
            println("###"); snapshot.foreach(println) //todo send snapshot to view
            children.pluviometers.foreach(_ ! PluviometerActor.TakeSnapshot)
            children.fireStations.foreach(_ ! FireStationActor.TakeSnapshot)
            children.zones.foreach(_ ! ZoneActor.TakeSnapshot)
            Behaviors.same
          case NotifyPluviometerState(state) =>
            snapshot.pluviometerDatas = snapshot.pluviometerDatas + (state.id -> state)
            Active(city, children, snapshot)
          case NotifyFireStationState(state) =>
            snapshot.fireStationDatas = snapshot.fireStationDatas + (state.id -> state)
            Active(city, children, snapshot)
          case NotifyZoneState(state) =>
            snapshot.zoneDatas = snapshot.zoneDatas + (state.id -> state)
            Active(city, children, snapshot)
        }
      }

  /**
   * Model the state of a city actor.
   * @param position the position of the city
   * @param width    the width of the city
   * @param height   the height of the city
   * @param id       the identifier of the city
   */
  case class City(
    position: Point2D = (0D, Size.HEIGHT),
    width: Double = Size.WIDTH,
    height: Double = Size.HEIGHT,
    id: String = Id.newId
  ) extends Id:
    /** @return a zone covering the whole city. */
    def asZone: Zone = Zone(position, width, height)
    /** @return the data representing this city. */
    def data: CityData = CityData(position, width, height, id)
    override def toString: String = s"City(#$id,$position,${width.pretty},${height.pretty})"

  /**
   * Model the data representing a city.
   */
  case class CityData(position: Point2D, width: Double, height: Double, id: String) extends CborSerializable:
    /** @return the city represented by this data. */
    def city: City = City(position, width, height, id)

  /**
   * Model a snapshot of all the entities inside a city with their state.
   *
   * @param pluviometerDatas a map from the pluviometers to their state
   * @param fireStationDatas a map from the fire-stations to their state
   * @param zoneDatas        a map from the zones to their state
   */
  case class Snapshot(
     var cityData: CityData,
     var pluviometerDatas: Map[String, PluviometerData] = Map(),
     var fireStationDatas: Map[String, FireStationData] = Map(),
     var zoneDatas: Map[String, ZoneData] = Map()
  ) extends CborSerializable:
    /**
     * Consumes the city, the zones, the pluviometers and the fire-stations of this snapshot with the specified consumer.
     * @param consumer the specified consumer
     */
    def foreach(consumer: CityData | PluviometerData | FireStationData | ZoneData => Unit): Unit =
      consumer(cityData)
      zoneDatas.values.foreach(consumer)
      pluviometerDatas.values.foreach(consumer)
      fireStationDatas.values.foreach(consumer)
  /**
   * Companion object of [[Snapshot]].
   */
  object Snapshot:
    /** @return a random snapshot. Useful for debugging. */
    def random: Snapshot =
      val snapshot: Snapshot = Snapshot(City().data)
      snapshot.zoneDatas = Map.from(snapshot.cityData.city.asZone.splitInto(NUMBER_OF_ZONES).map(z => {
        z.become(Zone.State.random)
        (z.id, z.data)
      }))
      snapshot.fireStationDatas = Map.from(snapshot.zoneDatas.values.map(z =>
        FireStation(z.asZone.randomPosition(RANDOM_POSITION_PADDING))).map(f => {
        f.become(FireStation.State.random)
        (f.id, f.data)
      })
      )
      snapshot.pluviometerDatas = Map.from(
        snapshot.zoneDatas.values.flatMap(z =>
          (0 until Random.nextInt(MAX_PLUVIOMETERS_PER_ZONE) + 1).map(_ =>
            Pluviometer.withRandomMeasurements(z.asZone.randomPosition(RANDOM_POSITION_PADDING), 0.1)
          )
        ).map(p => {
          p.measure()
          (p.id, p.data)
        })
      )
      snapshot

  /**
   * Model a collection of the actors known by a city.
   *
   * @param pluviometers a list of the pluviometer actors known by the city
   * @param fireStations a list of the fire-station actors known by the city
   * @param zones        a list of the zone actors known by the city
   */
  case class CityChildren(
    var pluviometers: List[PluviometerRef] = List(),
    var fireStations: List[FireStationRef] = List(),
    var zones: List[ZoneRef] = List()
  )