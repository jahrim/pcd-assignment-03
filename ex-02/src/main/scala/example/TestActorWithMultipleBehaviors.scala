package example

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import example.ActorWithMultipleBehaviors.Message

/**
 * Test with a scala actor that can assume multiple behaviors during his lifecycle.
 */
object TestActorWithMultipleBehaviors extends App:
    import ActorWithMultipleBehaviors.*
    val guardianActor = ActorSystem[Message](ActorWithMultipleBehaviors(0), "myActor")

    println("sending messages that can't be handled by the actor in his current state...")
    guardianActor ! One             // same as guardianActor.tell(Message.One)
    guardianActor ! Two
    Thread.sleep(1000)	    // wait for the output to be logged

    println("sending messages that will make the actor change his behavior...")
    guardianActor ! Zero 	        // when handled, <myActor> will only accept <MsgOne>
    guardianActor ! One  	        // when handled, <myActor> will only accept <MsgTwo>
    guardianActor ! Two  	        // when handled, <myActor> will stop his execution
    Thread.sleep(1000)	    // wait for the output to be logged

    println("sending messages when the actor has stopped his execution...")
    guardianActor ! Zero
    guardianActor ! One
    guardianActor ! Two
    Thread.sleep(1000)	    // wait for the output to be logged

    println("done")

/**
 * Model an actor with multiple behaviors.
 */
object ActorWithMultipleBehaviors:
    export Message.*

    def apply(localState: Int): Behavior[Message] = BehaviorA(localState)

    /** Messages */
    enum Message { case Zero, One, Two }
    /** Behavior A */
    object BehaviorA:
        def apply(localState: Int): Behavior[Message] =
            Behaviors.receive((context, message) =>
                message match {
                    case Zero =>
                        context.log.info(s"msgZero -state: $localState")
                        BehaviorB(localState+1)
                    case _ => ignore(context, message)
                }
            )
    /** Behavior B */
    object BehaviorB:
        def apply(localState: Int): Behavior[Message] =
            Behaviors.receive((context, message) =>
                message match {
                    case One =>
                        context.log.info(s"msgOne -state: $localState")
                        BehaviorC(localState+1)
                    case _ => ignore(context, message)
                }
            )
    /** Behavior C */
    object BehaviorC:
        def apply(localState: Int): Behavior[Message] =
            Behaviors.receive((context, message) =>
                message match {
                    case Two =>
                        context.log.info(s"msgTwo -state: $localState")
                        Behaviors.stopped
                    case _ => ignore(context, message)
                }
            )
    /**
     * Ignores the specified message in the specified context.
     * @param context the specified context
     * @param message the specified message
     * @return the current behavior
     */
    private def ignore(context: ActorContext[Message], message: Message): Behavior[Message] =
        context.log.warn(s"ignored message: $message")
        Behaviors.same