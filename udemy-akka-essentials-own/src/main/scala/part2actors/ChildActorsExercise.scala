package part2actors

import akka.actor.{Actor, ActorRef, Props}
import part2actors.ChildActorsExercise.WordCounterMaster.WordCountTask

object ChildActorsExercise extends App {

    // Distributed Word counting

    object WordCounterMaster {
        case class Initialize(nChildren: Int)
        case class WordCountTask(originalSender: ActorRef, text: String)
        case class WordCountReply(originalSender: ActorRef, count: Int)
    }
    class WordCounterMaster extends Actor {
        import WordCounterMaster._
        override def receive: Receive = notInitialized

        def notInitialized : Receive = {
            case Initialize(workers) =>
                val workerRefs = (1 to workers).map(id => (context.actorOf(Props[WordCounterWorker], s"worker$id") -> 0))
//                val workersRefs = for (i <- 1 to workers) yield context.actorOf(Props[WordCounterWorker], )
                context.become(working(Map() ++ workerRefs))
        }

        // (worker -> number of tasks)
        def working(workers: Map[ActorRef, Int]) : Receive = {
            case WordCountReply(originalSender, count) =>
                val (worker, tasks) = workers.find(k => k._1 == sender()).getOrElse(throw new Exception())
                context.become(working(workers + (worker -> (tasks - 1))))
                originalSender ! count
            case countPhrase@String =>
                val (worker, tasks) = workers.min
                context.become(working(workers + (worker -> (tasks + 1))))
                worker ! WordCountTask(sender(), countPhrase)
        }
    }
    class WordCounterWorker extends Actor {
        import WordCounterMaster._
        override def receive: Receive = {
            case WordCountTask(originalSender, text) => sender() ! WordCountReply(originalSender, text.split(' ').length)
        }
    }

    /*
    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Akka is awesome" to wordCounterMaster
        wcm will send a WordCountTask("...") to one of its children
            child replies with a WordCountReply(3) to the master
        master replies with 3 to the sender.

    requester -> wcm -> wcw
            r <- wcm <-

     */
    // round robin logic
    // 1,2,3,4,5 and 7 tasks
    // 1,2,3,4,5,1,2
}
