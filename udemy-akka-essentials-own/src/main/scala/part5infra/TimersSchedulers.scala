package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Kill, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

/*
Things to bear in mind

don't use unstable references inside scheduled actions
all scheduled tasks execute when the system is terminated
schedulers are not the best at precision and long-term planning


Recap

Schedule an action at a defined point in the future
val schedule = system.scheduler.scheduleOnce(1 seconds) { code here }

Repeated action
val schedule = system.scheduler.schedule(1 second, 2 seconds) { code here }
schedule.cancel()

Timers: schedule messages to self, from within
timers.startSingleTimer(MyTimerKey, MyMessage, 2 seconds)
timers.startPeriodicTimer(MyTimerKey, MyMessage, 2 seconds)
timers.cancel(MyTimerKey)
 */

object TimersSchedulers extends App {

    class SimpleActor extends Actor with ActorLogging {
        override def receive: Receive = {
            case message => log.info(message.toString)
        }
    }

    val system = ActorSystem("SchedulersTimersDemo")
    val simpleActor = system.actorOf(Props[SimpleActor])

    system.log.info("Scheduling reminder for simpleActor")

//    implicit val executionContext = system.dispatcher
    import system.dispatcher
    system.scheduler.scheduleOnce(1 second){
        simpleActor ! "reminder"
    }

    val routine: Cancellable = system.scheduler.schedule(
        1 second, // initial delay
        2 seconds
    ) {
        simpleActor ! "heartbeat"
    }
    system.scheduler.scheduleOnce(5 seconds) {
        routine.cancel()
    }

    /**
     * Exercise: implement a self-closing actor
     *
     * - if the actor receives a message (anything), you have 1 second to send it another message
     * - if the time window expires, the actor will stop itself
     * - if you send another message, the time window is reset
     */

    class SelfClosingActor extends Actor with ActorLogging {

        override def preStart(): Unit = log.info("Starting actor")

        override def postStop(): Unit = log.info("Finished actor")

        override def receive: Receive = startState

        def startState : Receive = {
            case message => {
                log.info(s"Message received ${message}")
                context.become(closing(scheduleStop()))
            }
        }
        def closing(routine: Cancellable) : Receive = {
            case message => {
                log.info(s"Message received ${message}")
                routine.cancel()
                context.become(closing(scheduleStop()))
            }
        }

        def scheduleStop() : Cancellable = system.scheduler.scheduleOnce(1 second) {
            context.stop(self)
        }
    }

    val selfClosingActor = system.actorOf(Props[SelfClosingActor], "SelfClosingActor")
    val routineCancel = system.scheduler.schedule(0 seconds, 0.2 seconds) {
        selfClosingActor ! "Don't close please!"
    }

    system.scheduler.scheduleOnce(1 seconds) {
        routineCancel.cancel()
    }

    /**
     * Timer
     */
    case object TimerKey
    case object Start
    case object Reminder
    case object Stop
    class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
        timers.startSingleTimer(TimerKey, Start, 500 millis)

        override def receive: Receive = {
            case Start =>
                log.info("Bootstrapping")
                timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
            case Reminder =>
                log.info("I am alive")
            case Stop =>
                log.warning("Stopping!")
                timers.cancel(TimerKey)
                context.stop(self)
        }
    }

    val timerHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timerActor")
    system.scheduler.scheduleOnce(5 seconds) {
        timerHeartbeatActor ! Stop
    }
}
