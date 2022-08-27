package configuration

import util.Point2D
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
        val NUMBER_OF_ZONES: Int = 2
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
     * Model the configuration for the logs of this application.
     */
    object Log:
        /** How many decimals will have doubles when they are prettily formatted. */
        val PRETTY_DOUBLE_DECIMALS: Int = 2
        extension(d: Double) def pretty: String = String.format(Locale.US, s"%.${PRETTY_DOUBLE_DECIMALS}f", d)