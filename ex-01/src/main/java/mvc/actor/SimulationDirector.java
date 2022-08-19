package mvc.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import mvc.actor.SimulationBuilder.Simulation;
import mvc.actor.SimulationActor.*;
import mvc.model.Body;
import mvc.model.Boundary;
import util.data.ListUtil;
import util.math.IntRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Model a coordinator for a simulation.
 */
public class SimulationDirector {
    /** Messages */
    public interface SimulationDirectorMessage {}
        public static class StartMessage implements SimulationDirectorMessage {}
        public static class StopMessage implements SimulationDirectorMessage {}
        public static class ResultMessage implements SimulationDirectorMessage {
            public final Collection<Body> updatedBodies;
            public ResultMessage(Collection<Body> updatedBodies) { this.updatedBodies = updatedBodies; }
        }

    public static Behavior<SimulationDirectorMessage> create(Simulation simulation) {
        return Behaviors.setup(context -> new Awaiting(context, simulation.getSnapshot()));
    }
    private SimulationDirector() {}

    /** Model a behavior with a reference to a simulation. */
    private static abstract class WithSimulation extends AbstractBehavior<SimulationDirectorMessage> {
        protected final Simulation simulation;
        public WithSimulation(ActorContext<SimulationDirectorMessage> context, Simulation simulation) {
            super(context);
            this.simulation = simulation;
        }
    }
    /** Model a behavior with the references to a certain number of child actors. */
    private static abstract class SimulationBehavior extends WithSimulation {
        protected final List<ActorRef<SimulationActorMessage>> childActors;
        protected int expectedMessages;
        protected SimulationBehavior(ActorContext<SimulationDirectorMessage> context, Simulation simulation) {
            super(context, simulation);
            this.childActors = new ArrayList<>();
            IntStream.range(0, this.simulation.numberOfSimulationActors)
                     .forEach(i -> this.childActors.add(context.spawnAnonymous(SimulationActor.create())));
        }
        /**
         * Distributes the messages produced by the specified supplier to the children of this actor.
         * @param messageSupplier a supplier that produces a message to be sent to a child of this actor,
         *                        knowing the partition of the simulation that has been assigned to that child
         */
        protected void distributeToChildren(Function<IntRange, SimulationActorMessage> messageSupplier){
            List<IntRange> partitions = ListUtil.partition(new ArrayList<>(this.simulation.bodies.values()), this.childActors.size());
            for (int i = 0; i < partitions.size(); i++) {
                this.childActors.get(i).tell(messageSupplier.apply(partitions.get(i)));
            }
        }
    }
    /** Model the behavior where the coordinator is waiting for a message before starting the simulation. */
    private static class Awaiting extends WithSimulation {
        private Awaiting(ActorContext<SimulationDirectorMessage> context, Simulation simulation) { super(context, simulation); }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(StartMessage.class, (message) -> Behaviors.setup(context -> new UpdatingVelocities(context, simulation.getSnapshot())))
                    .build();
        }
    }
    /** Model the behavior where the coordinator updates the velocity of the bodies in the simulation. */
    private static class UpdatingVelocities extends SimulationBehavior {
        private UpdatingVelocities(ActorContext<SimulationDirectorMessage> context, Simulation simulation) {
            super(context, simulation);
            this.distributeToChildren(childPartition ->
                new UpdateVelocitiesMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().map(Body::copyOf).collect(Collectors.toList()),
                    this.simulation.dt,
                    childPartition.from,
                    childPartition.to
                )
            );
            this.expectedMessages = this.childActors.size();
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        message.updatedBodies.forEach(body -> this.simulation.bodies.put(body.getId(), body));
                        return --this.expectedMessages > 0 ? Behaviors.same() : Behaviors.setup(context -> new UpdatingPositions(context, this.simulation.getSnapshot()));
                    })
                    .build();
        }
    }
    /** Model the behavior where the coordinator updates the position of the bodies in the simulation. */
    private static class UpdatingPositions extends SimulationBehavior {
        private UpdatingPositions(ActorContext<SimulationDirectorMessage> context, Simulation simulation) {
            super(context, simulation);
            this.distributeToChildren(childPartition ->
                new UpdatePositionsMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().skip(childPartition.from).limit(childPartition.to - childPartition.from).map(Body::copyOf).collect(Collectors.toList()),
                    this.simulation.dt
                )
            );
            this.expectedMessages = this.childActors.size();
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        message.updatedBodies.forEach(body -> this.simulation.bodies.put(body.getId(), body));
                        return --this.expectedMessages > 0 ? Behaviors.same() : Behaviors.setup(context -> new CheckingCollisions(context, this.simulation.getSnapshot()));
                    })
                    .build();
        }
    }
    /** Model the behavior where the coordinator check for collisions inside the simulation. */
    private static class CheckingCollisions extends SimulationBehavior {
        private CheckingCollisions(ActorContext<SimulationDirectorMessage> context, Simulation simulation) {
            super(context, simulation);
            this.distributeToChildren(childPartition ->
                new CheckCollisionsMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().skip(childPartition.from).limit(childPartition.to - childPartition.from).map(Body::copyOf).collect(Collectors.toList()),
                    Boundary.copyOf(this.simulation.bounds)
                )
            );
            this.expectedMessages = this.childActors.size();
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        message.updatedBodies.forEach(body -> this.simulation.bodies.put(body.getId(), body));
                        if (--this.expectedMessages > 0) {
                            return Behaviors.same();
                        } else {
                            this.simulation.completeIteration();
                            if (this.simulation.viewer != null){ this.simulation.viewer.display(this.simulation); }
                            return this.simulation.getCurrentIteration() < this.simulation.maxIterations 
                                   ? Behaviors.setup(context -> new UpdatingVelocities(context, this.simulation.getSnapshot()))
                                   : Behaviors.stopped();
                        }
                    })
                    .build();
        }
    }
}