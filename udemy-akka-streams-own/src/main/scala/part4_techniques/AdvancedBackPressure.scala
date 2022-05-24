package part4_techniques

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import java.util.Date

object AdvancedBackPressure extends App {

  implicit val system = ActorSystem("AdvancedBackPressure")
  implicit val materializer = ActorMaterializer()

  /*
  Drop head -> will drop the oldest elements from the buffer
  Drop tail -> will drop the newest elements to make room for the incoming element
  Drop new -> will drop the incoming element
  Drop buffer -> will drop the entire buffer
  Fail -> will just tear down the entire stream
  Back pressure -> will send a signal upstream so we can pick just one
   */
  // control backpressure
  val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)

  case class PagerEvent(description: String, date: Date, nInstances: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("Service discovery failed", new Date),
    PagerEvent("Illegal elements in the data pipeline", new Date),
    PagerEvent("Number of HTTP 500 spiked", new Date),
    PagerEvent("A service stopped responding", new Date)
  )
  val eventSource = Source(events)

  val oncallEngineer = "daniel@rockthejvm.com" // a fast service for fetching oncall emails

  def sendEmail(notification: Notification) =
    println(s"Dear ${notification.email}, you have an event: ${notification.pagerEvent}") // actually send an email

  val notificationSink = Flow[PagerEvent]
    .map(event => Notification(oncallEngineer, event))
    .to(Sink.foreach[Notification](sendEmail))

  // standard
//  eventSource
//    .to(notificationSink)
//    .run()

  /*
    un-backpressurable source
   */
  def sendEmailSlow(notification: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, you have an event: ${notification.pagerEvent}")
  }

  val aggregateNotificationFlow = Flow[PagerEvent]
    .conflate((event1, event2) => {
      val nInstances = event1.nInstances + event2.nInstances
//      println(s"Event1: $event1")
//      println(s"Event2: $event2")
      PagerEvent(s"You have $nInstances events that required your attention", new Date, nInstances)
    })
    .map(resultingEvent => Notification(oncallEngineer, resultingEvent))

//  eventSource.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()
  // alternative to backpressure

  /*
    Slow producers: extrapolate/expand
   */
  import scala.concurrent.duration._
  val slowCounter = Source(Stream.from(1)).throttle(1, 1 second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))
  val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))

  slowCounter.via(repeater).to(hungrySink).run()

  val expander = Flow[Int].expand(element => Iterator.from(element))
}
