package configuration

import javafx.scene.paint.Color
import java.util.Locale
import scala.concurrent.duration.*

/**
 * Model the configuration for this application.
 */
object C:
    /**
     * Model the configuration for the city in this application.
     */
    object City:
        /** The size of the city. */
        object Size:
            /** The height of the city. */
            val HEIGHT: Double = 100d
            /** The width of the city. */
            val WIDTH: Double = 100d
            /** The area of the city. */
            val AREA: Double = HEIGHT * WIDTH
        /** The number of zones that the city is composed of. */
        val NUMBER_OF_ZONES: Int = 6
        /** The number of fire stations that are present in the city. */
        val NUMBER_OF_FIRE_STATIONS: Int = NUMBER_OF_ZONES
        /** How much time passes between two snapshots taken by the city actor. */
        val SNAPSHOT_PERIOD: FiniteDuration = 1.second

    /**
     * Model the configuration for the zones in this application.
     */
    object Zone:
        /** The max number of pluviometers allowed inside the same zone of the city. */
        val MAX_PLUVIOMETERS_PER_ZONE: Int = 3
        /** How much time passes between two measurement requests from the same zone. */
        val MEASUREMENT_PERIOD: FiniteDuration = 1.second
        /** How much time passes between two alerts to the fire-stations of a zone when that zone is under alarm. */
        val ALERT_PERIOD: FiniteDuration = 2.second
        /** The percentage padding of the zone where a random point is chosen, relative to the zone where a random point is requested. */
        val RANDOM_POSITION_PADDING: Double = 0.2D
        /** The default percentage padding of the zone where the split point is chosen, relative to the zone to be split. */
        val DEFAULT_SPLIT_POINT_PADDING: Double = 0.2D

    /**
     * Model the configuration for the pluviometers in this application.
     */
    object Pluviometer:
        /** The probability of a pluviometer measuring a value greater than its threshold. */
        val PLUVIOMETER_SIGNAL_PROBABILITY: Double = 0.1

    /**
     * Model the configuration for the fire-stations in this application.
     */
    object FireStation:
        /** How much it takes for a fire station to be prepared to take care of an alarm. */
        val PREPARATION_DURATION: FiniteDuration = 5.second
        /** How much it takes for a fire station to take care of an alarm. */
        val INTERVENTION_DURATION: FiniteDuration = 10.second

    /**
     * Model the configuration for the view of this application.
     */
    object View:
        /**
         * Model the configuration for the colors of the view of this application.
         */
        object Colors:
            /** The color of the border of the entities in the city. */
            val ENTITY_BORDER: Color = Color.BLACK
            /** The color of the pluviometers in the city. */
            val PLUVIOMETER: Color = Color.SKYBLUE
            /** The color of the fire-stations in the city, when they are available. */
            val FIRESTATION_AVAILABLE: Color = Color.LIGHTGREEN
            /** The color of the fire-stations in the city, when they are busy. */
            val FIRESTATION_BUSY: Color = Color.ORANGE
            /** The color of the border of the zones in the city. */
            val ZONE_BORDER: Color = Color.BLACK
            /** The color of the zones in the city, when they are calm. */
            val ZONE_CALM: Color = Color.WHITE
            /** The color of the zones in the city, when they are alarmed. */
            val ZONE_ALARMED: Color = Color.RED.deriveColor(0, 1, 1, 0.2)
            /** The color of the zones in the city, when they are monitored. */
            val ZONE_MONITORED: Color = Color.ORANGE.deriveColor(0, 1, 1, 0.2)
        /** How much a view actor waits before stopping retrying to register himself to his city. */
        val CONNECTION_TIMEOUT: FiniteDuration = 30.second
        /** How much a view actor waits before retrying to register himself to his city. */
        val RECONNECTION_PERIOD: FiniteDuration = 2.second
        /** How large are the circles used to display the positions of the entities in the city. */
        val ENTITY_RADIUS_PX: Double = 5D
    /**
     * Model the configuration for the logs of this application.
     */
    object Log:
        /** How many decimals will have doubles when they are prettily formatted. */
        val PRETTY_DOUBLE_DECIMALS: Int = 2
        extension(d: Double) def pretty: String = String.format(Locale.US, s"%.${PRETTY_DOUBLE_DECIMALS}f", d)