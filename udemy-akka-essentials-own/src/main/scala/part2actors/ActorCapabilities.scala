package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

    class SimpleActor extends Actor {
        val name = self
        override def receive: Receive = {
            case "Hi!" => sender() ! "Hello, there!"
            case message: String => println(s"[$name] I have received $message")
            case number: Int => println(s"[$name] I have received a NUMBER: $number")
            case SpecialMessage(contents) => println(s"[$name] I have received something SPECIAL: $contents")
            case SendMessageToYourself(content) =>
                self ! content
            case SayHiTo(ref) => ref ! "Hi!"
            case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // i keep the original sender of the WPM
        }
    }

    val system = ActorSystem("actorCapabilitiesDemo")
    val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

    simpleActor ! "hello, actor"

    // 1 - messages can be of any type
    // a) messages must be IMMUTABLE
    // b) messages must be SERIALIZABLE
    // in practice use case classes and case objects

    simpleActor ! 42 // who is the sender?!

    case class SpecialMessage(context: String)
    simpleActor ! SpecialMessage("some special content")

    // 2 - actors have information about their context and about themselves
    // context.self === `this` in OOP

    case class SendMessageToYourself(content: String)
    simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

    // 3 - actors can REPLY to messages
    val alice = system.actorOf(Props[SimpleActor], "alice")
    val bob = system.actorOf(Props[SimpleActor], "bob")

    case class SayHiTo(ref: ActorRef)
    alice ! SayHiTo(bob)

    // 4 - dead letters
//    alice ! "Hi!"

    // 5 - forwarding messages
    // D -> A -> B
    // forwarding = sending a message with the ORIGINAL sender

    case class WirelessPhoneMessage(context: String, ref: ActorRef)
    alice ! WirelessPhoneMessage("Hi", bob) // no sender

    /**
     * Exercices
     *
     * 1. a Counter actor
     *  - Increment
     *  - Decrement
     *  - Print
     *

     */

    object CounterActor {
        def props(initialCount: Int) = Props(new CounterActor(initialCount))
    }
    class CounterActor(var count: Int) extends Actor {

        override def receive: Receive = {
            case CounterIncrementMessage() => count += 1
            case CounterDecrementMessage() => count -= 1
            case CounterPrintMessage() => println(s"[$self] The current value is: $count")
        }
    }
    case class CounterIncrementMessage()
    case class CounterDecrementMessage()
    case class CounterPrintMessage()

    val counterActor = system.actorOf(CounterActor.props(0), "counter")
    counterActor ! CounterIncrementMessage()
    counterActor ! CounterIncrementMessage()
    counterActor ! CounterDecrementMessage()
    counterActor ! CounterPrintMessage()
    /**
     * 2. a Bank account as an actor
     *  - Deposit an amount
     *  - Withdraw an amount
     *  - Statement
     *  replies with
     *  - Success
     *  - Failure
     *
     *  interact with some other kind of actor
     */

    object BankActor {
        def props(initialCount: Int) = Props(new BankActor(initialCount))
    }
    class BankActor(var amount: Int) extends Actor {
        override def receive: Receive = {
            case BankDepositMessage(amount) =>
                this.amount += amount
                sender() ! BankSuccess(s"Amount increased to ${this.amount}")
            case BankWithdrawMessage(amount) =>
                if (this.amount < amount) sender() ! BankFailure(s"Trying to withdraw $amount having ${this.amount}")
                else {
                    this.amount -= amount
                    sender() ! BankSuccess(s"Amount withdrawed to ${this.amount}")
                }
            case BankStatementMessage() => sender() ! BankSuccess(s"Current amount: ${this.amount}")
        }
    }
    case class BankDepositMessage(amount: Int)
    case class BankWithdrawMessage(amount: Int)
    case class BankStatementMessage()

    case class BankSuccess(reply: Any)
    case class BankFailure(reply: Any)

    object BankAttendantActor {
        def props(bankActor: ActorRef) = Props(new BankAttendantActor(bankActor))
    }
    class BankAttendantActor(val bankActor: ActorRef) extends Actor {
        override def receive: Receive = {
            case BankSuccess(reply) => println(s"[$self] Success received: $reply")
            case BankFailure(reply) => println(s"[$self] Failure received: $reply")
            case message => bankActor ! message
        }
    }

    val bankActor = system.actorOf(BankActor.props(0), "bankActor")
    val bankAttActor = system.actorOf(BankAttendantActor.props(bankActor), "bankAttendantActor")

    bankAttActor ! BankDepositMessage(100)
    bankAttActor ! BankDepositMessage(200)
    bankAttActor ! BankWithdrawMessage(300)
    bankAttActor ! BankWithdrawMessage(5)
    bankAttActor ! BankDepositMessage(400)
    bankAttActor ! BankStatementMessage()
}
