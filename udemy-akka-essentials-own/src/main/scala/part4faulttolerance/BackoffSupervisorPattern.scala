package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.actor.SupervisorStrategy._

import scala.concurrent.duration._
import java.io.File
import scala.io.Source
import scala.language.postfixOps

/*
Backoff Recap

Pain: the repeated restart of actors
- restarting immediately might be useless
- everyone attempting at the same time can kill resources again

Create backoff supervision for exponential delays between attempts
BackoffSupervisor.props(
    Backoff.onFailure( // controls when backoff kicks in
        Props[MyActor],
        "myActor",
        3 seconds, // min delay
        30 seconds, // max delay
        0.2 // randomness factor
    )
)
 */
object BackoffSupervisorPattern extends App {

    case object ReadFile
    class FileBasedPersistentActor extends Actor with ActorLogging {

        var dataSource: Source = null

        override def preStart(): Unit =
            log.info("Persistent actor starting")

        override def postStop(): Unit =
            log.warning("Persistent actor has stopped")

        override def preRestart(reason: Throwable, message: Option[Any]): Unit =
            log.warning("Persistent actor restarting")

        override def receive: Receive = {
            case ReadFile =>
                if (dataSource == null)
                    dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
                log.info("I've just read some IMPORTANT data: " + dataSource.getLines().toList)
        }
    }

    val system = ActorSystem("BackoffSupervisorDemo")
//    val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//    simpleActor ! ReadFile

    val simpleSupervisorProps = BackoffSupervisor.props(
        Backoff.onFailure(
            Props[FileBasedPersistentActor],
            "simpleBackoffActor",
            3 seconds, // then 6s, 12s, 24s,
            30 seconds,
            0.2 // if you have huge amount of actor, this parameter is for not to trying to access the resource at the same time
        )
    )

//    val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//    simpleBackoffSupervisor ! ReadFile
    /*
        simpleSupervisor
        - child called simpleBackoffActor (props of type FileBasedPersistentActor)
        - supervision strategy is the default one (restarting on everything)
            - first attempt after 3 seconds
            - next attempt is 2x the previous attempt
     */

    val stopSupervisorProps = BackoffSupervisor.props(
        Backoff.onStop(
            Props[FileBasedPersistentActor],
            "stopBackoffActor",
            3 seconds,
            30 seconds,
            0.2
        ).withSupervisorStrategy(
            OneForOneStrategy() {
                case _ => Stop
            }
        )
    )

//    val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//    stopSupervisor ! ReadFile
//    stopSupervisor ! ReadFile
//    stopSupervisor ! ReadFile
//    stopSupervisor ! ReadFile

    class EagerFBPActor extends FileBasedPersistentActor {
        override def preStart(): Unit = {
            log.info("Eager actor starting")
            dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
        }
    }

//    val eagerActor = system.actorOf(Props[EagerFBPActor])
    // ActorInitializationException => STOP

    val repeatedSupervisorProps = BackoffSupervisor.props(
        Backoff.onStop(
            Props[EagerFBPActor],
            "eagerActor",
            1 second,
            30 seconds,
            0.1
        )
    )

    val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

    /*
    eagerSupervisor
        - child eagerActor
            - will die on start with ActorInitializationException
            - trigger the supervision strategy in eagerSupervisor => STOP eagerActor
        - backoff will kick in after 1 second, 2s, 4, 8, 16

     */
}
