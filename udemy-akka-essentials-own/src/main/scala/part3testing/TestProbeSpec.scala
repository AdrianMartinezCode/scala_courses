package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
/*
TestProbes are useful for interactions with multiple actors
val slave = TestProbe("Slave")
Can send messages or reply
probe.send(actorUnderTest, "a message")
probe.reply("a message")
Has the same assertions as the testActor

can watch other actors


 */

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
{

    override protected def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }

    import TestProbeSpec._

    "A master actor" should {
        "register a slave" in {
            val master = system.actorOf(Props[Master])
            val slave = TestProbe("Slave") // special actor with some capabilities

            master ! Register(slave.ref)
            expectMsg(RegistrationAck)
        }

        "send the work to the slave actor" in {
            val master = system.actorOf(Props[Master])
            val slave = TestProbe("Slave")
            master ! Register(slave.ref)
            expectMsg(RegistrationAck)

            val workloadString = "I love Akka"
            master ! Work(workloadString)

            // the interaction between the master and the slave actor
            slave.expectMsg(SlaveWork(workloadString, testActor))
            slave.reply(WorkCompleted(3, testActor))

            expectMsg(Report(3)) // testActor receives the Report(3)
        }

        "aggregate data correctly" in {
            val master = system.actorOf(Props[Master])
            val slave = TestProbe("Slave")
            master ! Register(slave.ref)
            expectMsg(RegistrationAck)

            val workloadString = "I love Akka"
            master ! Work(workloadString)
            master ! Work(workloadString)

            // in the meantime I don't have a slave actor
            slave.receiveWhile() {
                case SlaveWork(`workloadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
            }
            expectMsg(Report(3))
            expectMsg(Report(6))
        }
    }

}

object TestProbeSpec {
    // scenario
    /*
    word counting actor hierarchy master-slave
    send some work to the master
        - master sends the slave the piece of work
        - slave processes the work and replies to master
        - master aggregates the result
    master sends the total count to the original requester
     */

    case class Work(text: String)
    case class SlaveWork(text: String, originalRequester: ActorRef)
    case class WorkCompleted(count: Int, originalRequester: ActorRef)
    case class Register(slaveRef: ActorRef)
    case class Report(totalCount: Int)
    case object RegistrationAck
    class Master extends Actor {
        override def receive: Receive = {
            case Register(slaveRef) =>
                sender() ! RegistrationAck
                context.become(online(slaveRef, 0))
        }

        def online(slaveRef: ActorRef, totalWordCount: Int) : Receive = {
            case Work(text) => slaveRef ! SlaveWork(text, sender())
            case WorkCompleted(count, originalRequester) =>
                val newTotalWordCount = totalWordCount + count
                originalRequester ! Report(newTotalWordCount)
                context.become(online(slaveRef, newTotalWordCount))
        }
    }

    // class Slave extends Actor ....
}
