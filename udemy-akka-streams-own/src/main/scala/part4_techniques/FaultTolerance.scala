package part4_techniques

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration._
import scala.util.Random


/*
Recap

Logging
myStream.log("my tag")

Recovering
myStream.recover {
  case ex: RuntimeException => 42
}
myStream.recoverWithRetries(3, {
  case ex: RuntimeException => anotherStream
})

Backoff supervision
RestartSource.onFailuresWithBackoff(
  minBackoff = 1 second,
  maxBackoff = 30 seconds,
  randomFactor = 0.2,
  maxRestarts = 10
)(createAnotherSourceInCaseOfFailure)

Supervision strategies
myStream.withAttributes(ActorAttributes.supervisionStrategy(decider))

 */
object FaultTolerance extends App {

  implicit val system = ActorSystem("FaultTolerance")
  implicit val materializer = ActorMaterializer()

  // 1 - logging
  val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
  faultySource.log("trackingElements").to(Sink.ignore)
    //.run()

  // 2 - gracefully terminating a stream
  faultySource.recover {
    case _: RuntimeException => Int.MinValue
  } .log("gracefulSource")
    .to(Sink.ignore)
//    .run()

  // 3 - recover with another stream
  faultySource.recoverWithRetries(3, {
    case _: RuntimeException => Source(90 to 99)
  })
    .log("recoverWithRetries")
    .to(Sink.ignore)
//    .run()

  // 4 - backoff supervision
  val restartSource = RestartSource.onFailuresWithBackoff(
    minBackoff = 1 second,
    maxBackoff = 30 seconds,
    randomFactor = 0.2,
  )(() => {
    val randomNumber = new Random().nextInt(20)
    Source(1 to 10).map(elem => if (elem == randomNumber) throw new RuntimeException else elem)
  })

  restartSource
    .log("restartBackoff")
    .to(Sink.ignore)
//    .run()

  // 5 - supervision strategy
  val numbers = Source(1 to 20).map(n => if (n == 13) throw new RuntimeException("bad luck") else n)
    .log("supervision")

  val supervisedNumbers = numbers.withAttributes(ActorAttributes.supervisionStrategy {
    /*
        Resume = skips the faulty element
        Stop = stop the stream
        Restart = resume + clears internal state
     */
    case _: RuntimeException => Resume
    case _ => Stop
  })

  supervisedNumbers.to(Sink.ignore).run()

}
