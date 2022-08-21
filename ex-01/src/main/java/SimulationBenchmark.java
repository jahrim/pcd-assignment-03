import akka.actor.typed.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
    private static final int MAX_ACTORS = Runtime.getRuntime().availableProcessors() + 1;
    private static final double DT = 0.01D;

    /** The set of #bodies arguments to test. */
    private final static List<Integer> nBodiesArgs = List.of(100, 1000, 5000);
    /** The set of #iterations arguments to test. */
    private final static List<Integer> nIterationsArgs = List.of(1000, 5000, 10000);
    /** The set of #actors arguments to test. */
    private final static List<Integer> nActorsArgs = List.of(1, 2, 4, 8, MAX_ACTORS);
    /** A map from a specified tuple of arguments to the time of execution of the correspondent simulation. */
    private final static Map<SimulationArgs, Long> timeMap = new ConcurrentHashMap<>();

    private static final Config disableLoggingConfig = ConfigFactory.load("disable-logging");

    public static void main(String[] args) throws InterruptedException {
        Semaphore simulationEnded = new Semaphore(0);
        for (Integer nBodies: nBodiesArgs) {
            for (Integer nIterations: nIterationsArgs) {
                for (Integer nActors: nActorsArgs) {
                    //Creating simulation...
                    SimulationArgs simArgs = new SimulationArgs(nBodies, nIterations, nActors);
                    SimulationBuilder.Simulation simulation =
                        new SimulationBuilder()
                            .setNumberOfSimulationActors(nActors)
                            .setMaxIterations(nIterations)
                            .setDeltaTime(DT)
                            .testCustomBodySet(nBodies)
                            .build();
                    simulation.onIterationComplete((iteration) -> printProgress(iteration, nIterations));
                    //Running simulation...
                    System.out.println("Simulating " + simArgs + "...");
                    StopWatch timer = new StopWatch().next();
                    ActorSystem<SimulationDirectorMessage> guardianActor =
                        ActorSystem.create(SimulationDirector.create(simulation),"SimulationDirector", disableLoggingConfig);
                    guardianActor.tell(new SimulationDirector.StartMessage());
                    guardianActor.getWhenTerminated()
                                 .thenAccept(__ -> {
                                     long time = timer.getDuration();
                                     timeMap.put(simArgs, time);
                                     System.out.println();
                                     printSimulationResult(simArgs, time);
                                     System.out.println();
                                     simulationEnded.release();
                                 });
                    try { simulationEnded.acquire(); } catch (InterruptedException e) { e.printStackTrace(); }
                }
            }
        }
        timeMap.entrySet()
               .stream()
               .sorted(Map.Entry.comparingByKey())
               .forEach(entry -> printSimulationResult(entry.getKey(), entry.getValue()));
        System.exit(0);
    }
    /**
     * Formats and prints the progress of the specified simulation.
     * @param iteration the current iteration of the specified simulation
     * @param maxIteration the iterations to be run in the specified simulation
     */
    private static void printProgress(long iteration, long maxIteration){
        System.out.print("\r" + iteration + "/" + maxIteration + "\t" + String.format("%.2f", ((double) iteration) / maxIteration * 100) + "%");
    }
    /**
     * Formats and prints the specified simulation with the specified time.
     * @param simulation the specified simulation
     * @param time the specified time
     */
    private static void printSimulationResult(SimulationArgs simulation, long time){
        System.out.println(simulation + " => " + String.format("%.2f", time/1000f) + "s");
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