package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

/*
A small distinction

Actor instance
- has methods
- may have internal state

Actor reference aka incarnation
- created with actorOf
- has mailbox and can receive messages
- contains one actor instance
- contains a UUID

Actor path
- may or may not have an ActorRef inside


Actor Lifecycle

Actors can be
- started
- suspended
- resumed
- restarted
- stopped

Start = create a new ActorRef with a UUID at a given path
Suspend = the actor ref will enqueue but NOT process more messages
Resume = the actor ref will continue processing more messages

Restarting is trickier
- suspend
- swap the actor instance
    - old instance calls preRestart
    - replace actor instance
    - new instance calls postRestart
- resume

Internal state is destroyed on restart

Stopped frees the actor ref within a path
- call postStop
- all watching actors receive Terminated(ref)

After stopping, another actor may be created at the same path
- different UUID, so different ActorRef
 */
object ActorLifecycle extends App {

    object StartChild
    class LifecycleActor extends Actor with ActorLogging {

        override def preStart(): Unit = log.info("I am starting")
        override def postStop(): Unit = log.info("I have stopped")
        override def receive: Receive = {
            case StartChild =>
                context.actorOf(Props[LifecycleActor], "child")
        }
    }

    val system = ActorSystem("LifecycleDemo")
    val parent = system.actorOf(Props[LifecycleActor], "parent")
    parent ! StartChild
    parent ! PoisonPill

    /**
     * restart
     */
    object Fail
    object FailChild
    object CheckChild
    object Check

    class Parent extends Actor {
        private val child = context.actorOf(Props[Child], "supervisedChild")

        override def receive: Receive = {
            case FailChild => child ! Fail
            case CheckChild => child ! Check
        }
    }
    class Child extends Actor with ActorLogging {

        override def preStart(): Unit = log.info("supervised child started")
        override def postStop(): Unit = log.info("supervised child stopped")

        override def preRestart(reason: Throwable, message: Option[Any]): Unit =
            log.info(s"supervised actor restarting because of ${reason.getMessage}")

        override def postRestart(reason: Throwable): Unit =
            log.info(s"supervised actor restarted")

        override def receive: Receive = {
            case Fail =>
                log.warning("child will fail now")
                throw new RuntimeException("I failed")
            case Check =>
                log.info("alive and kicking")
        }
    }

    val supervisor = system.actorOf(Props[Parent], "supervisor")
    supervisor ! FailChild
    supervisor ! CheckChild

    // supervision strategy
    // the mailbox is untouched!!!
}
