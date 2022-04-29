package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props, Stash, SupervisorStrategy}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.language.postfixOps

object AkkaRecap extends App {
    class SimpleActor extends Actor with ActorLogging with Stash {
        override def receive: Receive = {
            case "createChild" =>
                val childActor = context.actorOf(Props[SimpleActor], "myChild")
                childActor ! "hello"
            case "stashThis" =>
                stash()
            case "change handler NOW" =>
                unstashAll()
                context.become(anotherHandler)
            case "change" => context.become(anotherHandler)
            case message => println(s"I received: $message")
        }

        def anotherHandler: Receive = {
            case message => println(s"In another receiver handler: $message")
        }

        override def preStart(): Unit = {
            log.info("starting")
        }

        override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
            case _: RuntimeException => Restart
            case _ => Stop
        }
    }
    val system = ActorSystem("AkkaRecap")
    val actor = system.actorOf(Props[SimpleActor], "simpleActor")
    actor ! "hello"

    import scala.concurrent.duration._
    import system.dispatcher
    system.scheduler.scheduleOnce(2 seconds){
        actor ! "delayed happy birthday"
    }

    import akka.pattern.ask
    implicit val timeout = Timeout(3 seconds)
//    val future = actor ? "question"

    import akka.pattern.pipe
    val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
//    future.mapTo[String].pipeTo(anotherActor)
}
