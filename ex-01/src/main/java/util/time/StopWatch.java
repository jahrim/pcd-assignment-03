package util.time;

/**
 * Models a stop watch used to keep track of the duration of certain activities.
 */
public class StopWatch {
    /** Store the time in milliseconds when this timer was last started. */
    private long startTimeMS;

    public StopWatch(){ this.startTimeMS = 0; }

    /**
     * Start measuring the duration of the next ongoing activity.
     * @return this
     */
    public StopWatch next() {
        this.startTimeMS = this.currentTime();
        return this;
    }

    /** @return the current duration of the ongoing activity in milliseconds. */
    public long getDuration() {
        return this.currentTime() - this.startTimeMS;
    }

    /** @return the current time in milliseconds. */
    private long currentTime() {
        return System.currentTimeMillis();
    }
}