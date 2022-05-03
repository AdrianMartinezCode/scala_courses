package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

/*
Recap:
Synchronous tests: all messages are handled in the calling thread
"the magic happens when the all runs are in the same thread"

Option 1: TestActorRef
val syncActor = TestActorRef[MyActor](Props[MyActor]) <-- needs an implicit ActorSystem
assert(syncActor.underlyingActor.member == 1)
syncActor.receive(Tick)

Option 2: CallingThreadDispatcher
val syncActor = system.actorOf(Props[MyActor].withDispatcher(CallingThreadDispatcher.Id))

 */
class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll {

    implicit val system = ActorSystem("SynchronousTestingSpec")

    override protected def afterAll(): Unit = {
        system.terminate()
    }

    import SynchronousTestingSpec._
    "A counter" should {
        "synchronously increase its counter" in {
            val counter = TestActorRef[Counter](Props[Counter])
            counter ! Inc // counter has ALREADY received the message

            assert(counter.underlyingActor.count == 1)
        }

        "synchronously increase its counter at the call of the receive function" in {
            val counter = TestActorRef[Counter](Props[Counter])
            counter.receive(Inc)
            assert(counter.underlyingActor.count == 1)
        }

        "work on the calling thread dispatcher" in {
            val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id)) // this forces to the actor runs at the same thread of the test
            val probe = TestProbe()

            probe.send(counter, Read)
            probe.expectMsg(Duration.Zero, 0) // probe has ALREADY received the message 0
        }
    }
}

object SynchronousTestingSpec {

    case object Inc
    case object Read

    class Counter extends Actor {
        var count = 0

        override def receive: Receive = {
            case Inc => count += 1
            case Read => sender() ! count
        }
    }
}