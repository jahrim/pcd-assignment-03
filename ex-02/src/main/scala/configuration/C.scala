package configuration

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
        /** The number of pluviometers in the city. */
        val NUMBER_OF_PLUVIOMETER: Int = 10
        /** How much rain a pluviometer can detect before sending an alarm signal. */
        val RAIN_TOLERANCE: Double = 100d
        /** How much it takes for a fire station to take care of an alarm. */
        val INTERVENTION_DURATION_MS: Long = 10000l