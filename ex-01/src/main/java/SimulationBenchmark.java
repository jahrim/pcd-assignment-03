import akka.actor.typed.ActorSystem;
import mvc.actor.SimulationBuilder;
import mvc.actor.SimulationDirector;
import mvc.actor.SimulationDirector.SimulationDirectorMessage;
import util.time.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Executes a concurrent simulation without view for all the possible combinations
 * of the specified arguments.
 */
public class SimulationBenchmark {
    private static final double DT = 0.01D;
    private static final int MAX_ACTORS = Runtime.getRuntime().availableProcessors() + 1;

    /** The set of #bodies arguments to test. */
    private final static List<Integer> nBodiesArgs = List.of(100, 1000, 5000);
    /** The set of #iterations arguments to test. */
    private final static List<Integer> nIterationsArgs = List.of(1000, 5000, 10000);
    /** The set of #actors arguments to test. */
    private final static List<Integer> nActorsArgs = List.of(1, 2, 4, 8, MAX_ACTORS);
    /** A map from a specified tuple of arguments to the time of execution of the correspondent simulation. */
    private final static Map<SimulationArgs, Long> timeMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Semaphore simulationEnded = new Semaphore(0);
        for (Integer nBodies: nBodiesArgs) {
            for (Integer nIterations: nIterationsArgs) {
                for (Integer nActors: nActorsArgs) {
                    SimulationArgs simArgs = new SimulationArgs(nBodies, nIterations, nActors);
                    System.out.println("Simulating " + simArgs + "...");
                    StopWatch timer = new StopWatch().next();
                    ActorSystem<SimulationDirectorMessage> guardianActor =
                        ActorSystem.create(
                            SimulationDirector.create(
                                    new SimulationBuilder().setNumberOfSimulationActors(nActors)
                                            .setMaxIterations(nIterations)
                                            .setDeltaTime(DT)
                                            .testCustomBodySet(nBodies)
                                            .build()
                            ),
                            "SimulationDirector"
                        );
                    guardianActor.tell(new SimulationDirector.StartMessage());
                    guardianActor.getWhenTerminated()
                                 .thenAccept(__ -> {
                                     long time = timer.getDuration();
                                     timeMap.put(simArgs, time);
                                     simulationEnded.release();
                                 });
                    try { simulationEnded.acquire(); } catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
        }
        timeMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> System.out.println(entry.getKey().toString() + " => " + String.format("%.2f", entry.getValue()/1000f) + "s"));
        System.exit(0);
    }

    /**
     * Models a tuple of arguments for a certain simulation.
     */
    public static class SimulationArgs implements Comparable<SimulationArgs> {
        public final int nBodies;
        public final int nIterations;
        public final int nActors;

        public SimulationArgs(int nBodiesArg, int nIterationsArg, int nActorsArg) {
            this.nBodies = nBodiesArg;
            this.nIterations = nIterationsArg;
            this.nActors = nActorsArg;
        }

        @Override
        public String toString() {
            return "SimulationArgs("+ nBodies + ", " + nIterations + ", " + nActors + ')';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimulationArgs that = (SimulationArgs) o;
            return nBodies == that.nBodies && nIterations == that.nIterations && nActors == that.nActors;
        }
        @Override
        public int hashCode() {
            return Objects.hash(nBodies, nIterations, nActors);
        }

        @Override
        public int compareTo(SimulationArgs o) {
            if (this.nBodies > o.nBodies) { return 1; } else if (this.nBodies < o.nBodies) { return -1; } else
            if (this.nIterations > o.nIterations) { return 1; } else if (this.nIterations < o.nIterations) { return -1; } else
            { return Integer.compare(this.nActors, o.nActors); }
        }
    }
}