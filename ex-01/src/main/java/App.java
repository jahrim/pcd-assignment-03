import mvc.actor.SimulationBuilder;
import mvc.actor.SimulationDirector;
import akka.actor.typed.ActorSystem;
import mvc.actor.SimulationDirector.*;
import mvc.view.SimulationView;

public class App {
    private static final int NUMBER_OF_SIMULATION_ACTORS = 10;
    private static final long MAX_ITERATIONS = 3000L;
    private static final double DT = 0.01D;

    public static void main(String[] args) {
        SimulationView view = new SimulationView(620, 620);
        ActorSystem<SimulationDirectorMessage> guardianActor = ActorSystem.create(
            SimulationDirector.create(
                new SimulationBuilder().setView(view)
                                       .setNumberOfSimulationActors(NUMBER_OF_SIMULATION_ACTORS)
                                       .setMaxIterations(MAX_ITERATIONS)
                                       .setDeltaTime(DT)
                                       .testBodySet4_many_bodies()
                                       .build()
            ),
            "SimulationDirector"
        );
        view.attachSimulationDirector(guardianActor);
    }
}