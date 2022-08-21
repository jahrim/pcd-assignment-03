package mvc.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.internal.receptionist.ReceptionistMessages.*;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.ServiceKey;
import mvc.actor.SimulationBuilder.Simulation;
import mvc.actor.SimulationActor.*;
import mvc.model.Body;
import mvc.model.Boundary;
import scala.Option;
import util.data.ListUtil;
import util.math.IntRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Model a coordinator for a simulation.
 */
public class SimulationDirector {
    /** Service key to the private service used by this actor to delegate tasks to his children. */
    private static final ServiceKey<SimulationActorMessage> serviceKey =
        ServiceKey.create(SimulationActorMessage.class, "SimulationDirectorDelegates");

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

    /** Model a behavior with the references to a certain number of child actors. */
    private static abstract class SimulationBehavior extends AbstractBehavior<SimulationDirectorMessage> {
        protected final Simulation simulation;
        protected final ActorRef<SimulationActorMessage> delegates;
        protected int expectedMessages;
        protected SimulationBehavior(ActorContext<SimulationDirectorMessage> context, Simulation simulation){
            super(context);
            this.simulation = simulation;
            this.delegates = spawnDelegates();
        }
        protected SimulationBehavior(ActorContext<SimulationDirectorMessage> context, ActorRef<SimulationActorMessage> delegates, Simulation simulation) {
            super(context);
            this.simulation = simulation;
            this.delegates = delegates;
        }
        /**
         * @return true if this director is expecting more messages from his delegates.
         */
        protected boolean isExpectingMoreMessages(){ return this.expectedMessages > 0; }
        /**
         * Distributes the messages produced by the specified supplier to the children of this actor.
         * @param messageSupplier a supplier that produces a message to be sent to a child of this actor,
         *                        knowing the partition of the simulation that has been assigned to that child
         */
        protected void distributeToChildren(Function<IntRange, SimulationActorMessage> messageSupplier){
            ListUtil.partition(new ArrayList<>(this.simulation.bodies.values()), this.simulation.numberOfSimulationActors)
                    .stream()
                    .map(messageSupplier)
                    .forEach(message -> {
                        this.delegates.tell(message);
                        this.expectedMessages++;
                    });
        }
        /**
         * @return a router actor which routes messages to the simulation actors used by this simulation director.
         */
        private ActorRef<SimulationActorMessage> spawnDelegates(){
            IntStream.range(0, this.simulation.numberOfSimulationActors)
                     .mapToObj(i -> this.getContext().spawnAnonymous(SimulationActor.create()))
                     .forEach(worker ->
                         this.getContext().getSystem().receptionist().tell(
                             new Register<>(SimulationDirector.serviceKey, worker, Option.empty())
                         )
                     );
            return this.getContext().spawn(Routers.group(SimulationDirector.serviceKey), "SimulationActorRouter");
        }
    }
    /** Model the behavior where the coordinator is waiting for a message before starting the simulation. */
    private static class Awaiting extends SimulationBehavior {
        private Awaiting(ActorContext<SimulationDirectorMessage> context, Simulation simulation) { super(context, simulation); }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(StartMessage.class, (message) -> Behaviors.setup(context -> new UpdatingVelocities(context, this.delegates, simulation.getSnapshot())))
                    .build();
        }
    }
    /** Model the behavior where the coordinator updates the velocity of the bodies in the simulation. */
    private static class UpdatingVelocities extends SimulationBehavior {
        private UpdatingVelocities(ActorContext<SimulationDirectorMessage> context, ActorRef<SimulationActorMessage> delegates, Simulation simulation) {
            super(context, delegates, simulation);
            this.distributeToChildren(childPartition ->
                new UpdateVelocitiesMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().map(Body::copyOf).collect(Collectors.toList()),
                    this.simulation.dt,
                    childPartition.from, childPartition.to
                )
            );
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        this.expectedMessages--;
                        this.simulation.updateBodies(message.updatedBodies);
                        return this.isExpectingMoreMessages()
                               ? Behaviors.same()
                               : Behaviors.setup(context -> new UpdatingPositions(context, this.delegates, this.simulation.getSnapshot()));
                    })
                    .build();
        }
    }
    /** Model the behavior where the coordinator updates the position of the bodies in the simulation. */
    private static class UpdatingPositions extends SimulationBehavior {
        private UpdatingPositions(ActorContext<SimulationDirectorMessage> context, ActorRef<SimulationActorMessage> delegates, Simulation simulation) {
            super(context, delegates, simulation);
            this.distributeToChildren(childPartition ->
                new UpdatePositionsMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().skip(childPartition.from).limit(childPartition.to - childPartition.from).map(Body::copyOf).collect(Collectors.toList()),
                    this.simulation.dt
                )
            );
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        this.expectedMessages--;
                        this.simulation.updateBodies(message.updatedBodies);
                        return this.isExpectingMoreMessages()
                               ? Behaviors.same()
                               : Behaviors.setup(context -> new CheckingCollisions(context, this.delegates, this.simulation.getSnapshot()));
                    })
                    .build();
        }
    }
    /** Model the behavior where the coordinator check for collisions inside the simulation. */
    private static class CheckingCollisions extends SimulationBehavior {
        private CheckingCollisions(ActorContext<SimulationDirectorMessage> context, ActorRef<SimulationActorMessage> delegates, Simulation simulation) {
            super(context, delegates, simulation);
            this.distributeToChildren(childPartition ->
                new CheckCollisionsMessage(
                    this.getContext().getSelf(),
                    this.simulation.bodies.values().stream().skip(childPartition.from).limit(childPartition.to - childPartition.from).map(Body::copyOf).collect(Collectors.toList()),
                    Boundary.copyOf(this.simulation.bounds)
                )
            );
        }
        @Override
        public Receive<SimulationDirectorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(StopMessage.class, (message) -> Behaviors.stopped())
                    .onMessage(ResultMessage.class, (message) -> {
                        this.expectedMessages--;
                        this.simulation.updateBodies(message.updatedBodies);
                        return this.isExpectingMoreMessages()
                               ? Behaviors.same()
                               : this.simulation.completeIteration().updateView().isRunning()
                               ? Behaviors.setup(context -> new UpdatingVelocities(context, this.delegates, this.simulation.getSnapshot()))
                               : Behaviors.stopped();
                    })
                    .build();
        }
    }
}