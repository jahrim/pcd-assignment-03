package configuration

import mvc.domain.Point2D

import java.util.Locale

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
        /** The max number of pluviometers allowed inside the same zone of the city. */
        val MAX_PLUVIOMETERS_PER_ZONE: Int = 3
        /** The probability of a pluviometer measuring a value greater than its threshold. */
        val PLUVIOMETER_SIGNAL_PROBABILITY: Double = 0.1
        /** How much it takes for a fire station to be prepared to take care of an alarm. */
        val PREPARATION_DURATION_MS: Long = 10000l
        /** How much it takes for a fire station to take care of an alarm. */
        val INTERVENTION_DURATION_MS: Long = 10000l

    /**
     * Model the configuration for the logs of this application.
     */
    object Log:
        /** How many decimals will have doubles when they are prettily formatted. */
        val PRETTY_DOUBLE_DECIMALS: Int = 2
        extension(d: Double) def pretty: String = String.format(Locale.US, s"%.${PRETTY_DOUBLE_DECIMALS}f", d)