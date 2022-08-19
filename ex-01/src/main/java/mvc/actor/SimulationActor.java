package mvc.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import mvc.model.Body;
import mvc.model.Boundary;
import util.math.V2d;
import mvc.actor.SimulationDirector.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Model an actor for a simulation.
 */
public class SimulationActor {
    /** Messages */
    public interface SimulationActorMessage {}
        private static abstract class WithSender {
            public final ActorRef<SimulationDirectorMessage> sender;
            protected WithSender(ActorRef<SimulationDirectorMessage> sender) { this.sender = sender; }
        }
        public static class UpdateVelocitiesMessage extends WithSender implements SimulationActorMessage {
            public final List<Body> allBodies;
            public final double dt;
            public final int fromInclusive;
            public final int toExclusive;

            public UpdateVelocitiesMessage(ActorRef<SimulationDirectorMessage> sender, List<Body> allBodies, double dt, int from, int to) {
                super(sender);
                this.allBodies = allBodies;
                this.dt = dt;
                this.fromInclusive = from;
                this.toExclusive = to;
            }
        }
        public static class UpdatePositionsMessage extends WithSender implements SimulationActorMessage {
            public final List<Body> bodiesToUpdate;
            public final double dt;

            public UpdatePositionsMessage(ActorRef<SimulationDirectorMessage> sender, List<Body> bodiesToUpdate, double dt) {
                super(sender);
                this.bodiesToUpdate = bodiesToUpdate;
                this.dt = dt;
            }
        }
        public static class CheckCollisionsMessage extends WithSender implements SimulationActorMessage {
            public final List<Body> bodiesToUpdate;
            public final Boundary boundary;

            public CheckCollisionsMessage(ActorRef<SimulationDirectorMessage> sender, List<Body> bodiesToUpdate, Boundary boundary) {
                super(sender);
                this.bodiesToUpdate = bodiesToUpdate;
                this.boundary = boundary;
            }
        }

    public static Behavior<SimulationActorMessage> create() { return Behaviors.logMessages(Behaviors.setup(Listening::new)); }
    private SimulationActor() {}

    /** Model the behavior where the simulation actor is accepting messages. */
    private static class Listening extends AbstractBehavior<SimulationActorMessage> {
        private Listening(ActorContext<SimulationActorMessage> context) { super(context); }
        @Override
        public Receive<SimulationActorMessage> createReceive() {
            return newReceiveBuilder()
                    .onMessage(UpdateVelocitiesMessage.class, (message) -> this.send(
                        message.sender,
                        new ResultMessage(
                            message.allBodies
                                   .stream()
                                   .skip(message.fromInclusive)
                                   .limit(message.toExclusive - message.fromInclusive)
                                   .peek(b -> {
                                       V2d totalForce = b.computeTotalForceOnSelf(message.allBodies);
                                       b.updateAcceleration(totalForce);
                                       b.updateVelocity(message.dt);
                                   })
                                   .map(Body::copyOf)
                                   .collect(Collectors.toList())
                        )
                    ))
                    .onMessage(UpdatePositionsMessage.class, (message) -> this.send(
                        message.sender,
                        new ResultMessage(
                            message.bodiesToUpdate
                                   .stream()
                                   .peek(b -> b.updatePosition(message.dt))
                                   .map(Body::copyOf)
                                   .collect(Collectors.toList())
                        )
                    ))
                    .onMessage(CheckCollisionsMessage.class, (message) -> this.send(
                        message.sender,
                        new ResultMessage(
                            message.bodiesToUpdate
                                   .stream()
                                   .peek(b -> b.checkAndSolveBoundaryCollision(message.boundary))
                                   .map(Body::copyOf)
                                   .collect(Collectors.toList())
                        )
                    ))
                    .build();
        }
        /**
         * Sends the specified message to the specified receiver.
         * @param receiver the specified receiver
         * @param message the specified message
         * @return the current behaviour of this actor
         */
        private Behavior<SimulationActorMessage> send(ActorRef<SimulationDirectorMessage> receiver, SimulationDirectorMessage message){
            receiver.tell(message);
            return Behaviors.same();
        }
    }
}