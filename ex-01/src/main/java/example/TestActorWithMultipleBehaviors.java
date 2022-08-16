package example;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import example.TestActorWithMultipleBehaviors.ActorWithMultipleBehaviors.ActorWithMultipleBehaviorsBaseMsg;
import example.TestActorWithMultipleBehaviors.ActorWithMultipleBehaviors.MsgOne;
import example.TestActorWithMultipleBehaviors.ActorWithMultipleBehaviors.MsgTwo;
import example.TestActorWithMultipleBehaviors.ActorWithMultipleBehaviors.MsgZero;

/**
 * Test with a java actor that can assume multiple behaviors during his lifecycle.
 *
 * An actor is created so that is has an initial behavior (or better he is a behavior himself).
 * A behavior decides:
 * - what are the messages that can be handled by an actor;
 * - how those messages will be handled by that actor when received.
 * Actors are created inside an actor system. An actor system is a thread pool shared between all
 * the actors inside that system.
 * An actor system requires at least one actor to be created, called the user guardian actor.
 * The user guardian actor will be the root actor and should create other child actors needed to
 * accomplish a certain task. Child actors can create other child actors as well.
 * All actors in an actor system can be terminated through either the user guardian actor or the
 * system itself. In fact, in Akka the user guardian actor and the actor system are the same thing.
 *
 * In this example, an actor will be able to assume four behaviors:
 * - Behavior A: can only accept MsgZero; when received transitions to Behavior B;
 * - Behavior B: can only accept MsgOne; when received transitions to Behavior C;
 * - Behavior C: can only accept MsgTwo; when received transitions to Stopped;
 * - Stopped: standard behavior for stopping the execution of an actor.
 */
public class TestActorWithMultipleBehaviors {
	public static void main(String[] args) throws Exception  {
		/*
		 * STEP 0
		 * Here, an actor of type <ActorWithMultipleBehaviors> is created as the guardian actor of an actor system and
		 * only accepts messages of type <ActorWithMultipleBehaviorsMsg>.
		 */
		final ActorSystem<ActorWithMultipleBehaviorsBaseMsg> guardianActor = ActorSystem.create(
			ActorWithMultipleBehaviors.create(0), "myActor"
		);

		/*
		 * STEP 1
		 * At the beginning, <myActor> only accepts <MsgZero>.
		 * The next two messages will be unhandled, for they are not included in the initial behavior.
		 */
		System.out.println("sending messages that can't be handled by the actor in his current state...");
		guardianActor.tell(new MsgOne());
		guardianActor.tell(new MsgTwo());
		Thread.sleep(1000);	    			// wait for the output to be logged

		/*
		 * STEP 2
		 * Then, three messages are sent and handled in order. Each one makes <myActor> transitions
		 * to another state, where he will adopt a new behavior and accept a different set of messages.
		 */
		System.out.println("sending messages that will make the actor change his behavior...");
		guardianActor.tell(new MsgZero()); 	// when handled, <myActor> will only accept <MsgOne>
		guardianActor.tell(new MsgOne());  	// when handled, <myActor> will only accept <MsgTwo>
		guardianActor.tell(new MsgTwo());  	// when handled, <myActor> will stop his execution
		Thread.sleep(1000);	    			// wait for the output to be logged

		/*
		 * STEP 3
		 * Then, three messages are sent and handled in order. Each one makes <myActor> transitions
		 * to another state, where he will adopt a new behavior and accept a different set of messages.
		 */
		System.out.println("sending messages when the actor has stopped his execution...");
		guardianActor.tell(new MsgZero());
		guardianActor.tell(new MsgOne());
		guardianActor.tell(new MsgTwo());
		Thread.sleep(1000);	    			// wait for the output to be logged

		System.out.println("done");
	}

	/**
	 * Model an actor with multiple behaviours.
	 */
	public static class ActorWithMultipleBehaviors extends AbstractBehavior<ActorWithMultipleBehaviorsBaseMsg> {
		/* <Messages> */
		public interface ActorWithMultipleBehaviorsBaseMsg {}
		public static class MsgZero implements ActorWithMultipleBehaviorsBaseMsg {}
		public static class MsgOne implements ActorWithMultipleBehaviorsBaseMsg {}
		public static class MsgTwo implements ActorWithMultipleBehaviorsBaseMsg {}
		/* </Messages> */

		private final int initialState;

		public static Behavior<ActorWithMultipleBehaviorsBaseMsg> create(int initialState) {
			return Behaviors.setup(context -> new ActorWithMultipleBehaviors(context, initialState));
		}
		private ActorWithMultipleBehaviors(ActorContext<ActorWithMultipleBehaviorsBaseMsg> context, int initialState) {
			super(context);
			this.initialState = initialState;
		}

		/* <BehaviorA> */
		/* Defines what messages will be handled and how they will be handled when the actor has adopted this behavior */
		@Override
		public Receive<ActorWithMultipleBehaviorsBaseMsg> createReceive() {
			return newReceiveBuilder()
					.onMessage(MsgZero.class,this::onMsgZero)
					.build();
		}
		/* An example of a message handler */
		private Behavior<ActorWithMultipleBehaviorsBaseMsg> onMsgZero(MsgZero msg) {
			this.getContext().getLog().info("msgZero - state: " + initialState);
			return Behaviors.setup(context -> new BehaviorB(context, initialState + 1));  //adopt BehaviorB
		}
		/* </BehaviorA> */

		/* <BehaviorB> */
		private static class BehaviorB extends AbstractBehavior<ActorWithMultipleBehaviorsBaseMsg> {
			private final int localState;
			private BehaviorB(ActorContext<ActorWithMultipleBehaviorsBaseMsg> context, int localState) {
				super(context);
				this.localState = localState;
			}
			@Override
			public Receive<ActorWithMultipleBehaviorsBaseMsg> createReceive() {
				return newReceiveBuilder()
						.onMessage(MsgOne.class, this::onMsgOne)
						.build();
			}
			private Behavior<ActorWithMultipleBehaviorsBaseMsg> onMsgOne(MsgOne msg) {
				this.getContext().getLog().info("msgOne - state: " + localState);
				return Behaviors.setup(context -> new BehaviorC(context, localState + 1));  //adopt BehaviorC
			}
		}
		/* </BehaviorB> */

		/* <BehaviorC> */
		private static class BehaviorC extends AbstractBehavior<ActorWithMultipleBehaviorsBaseMsg> {
			private final int localState;
			private BehaviorC(ActorContext<ActorWithMultipleBehaviorsBaseMsg> context, int localState) {
				super(context);
				this.localState = localState;
			}
			@Override
			public Receive<ActorWithMultipleBehaviorsBaseMsg> createReceive() {
				return newReceiveBuilder()
						.onMessage(MsgTwo.class, this::onMsgTwo)
						.build();
			}
			private Behavior<ActorWithMultipleBehaviorsBaseMsg> onMsgTwo(MsgTwo msg) {
				this.getContext().getLog().info("msgTwo - state: " + localState);
				return Behaviors.stopped();  //adopt Stopped
			}
		}
		/* </BehaviorC> */
	}
}