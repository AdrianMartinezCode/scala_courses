package part2_lowlevelserver

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

// step 1
import spray.json._

case class Guitar(make: String, model: String, inventory: Int)

object GuitarDB {
    case class CreateGuitar(guitar: Guitar)
    case class GuitarCreated(id: Int)
    case class FindGuitar(id: Int)
    case object FindAllGuitars
    case class FindAllGuitarsOnStock(inStock: Boolean)
    case class IncrementStockGuitar(id: Int, stock: Int)
    case object GuitarNotFound
    case class CurrentGuitarStock(guitar: Guitar)
}

/*
Recap

Marshalling: convert data to a "wire" format

case class Guitar(make: string, model: String)

import spray.json._
trait GuitarJsonProtocol extends DefaultJsonProtocol {
    implicit val guitarFormat = jsonFormat2(Guitar)
}

Pass a JSON string in an HTTP response's payload
Guitar("Fender", "Stratocaster").toJson.prettyPrint

Extract data from a JSON in an HTTP request's payload:
val guitarAsJson = strictEntity.data.utf8String
val guitar = guitarAsJson.parseJson.convertTo[Guitar]


 */
class GuitarDB extends Actor with ActorLogging {
    import GuitarDB._

    var guitars: Map[Int, Guitar] = Map()
    var currentGuitarId: Int = 0

    override def receive: Receive = {
        case FindAllGuitars =>
            log.info("Searching for all guitars")
            sender() ! guitars.values.toList

        case FindAllGuitarsOnStock(inStock) =>
            log.info(s"Searching for all guitars inStock: ${inStock}")
            sender() ! guitars.filter(pair => {
                if (inStock) pair._2.inventory > 0
                else pair._2.inventory == 0
            }).toList

        case FindGuitar(id) =>
            log.info(s"Searching guitar by $id")
            sender() ! guitars.get(id)

        case CreateGuitar(guitar) =>
            log.info(s"Adding guitar $guitar with id $currentGuitarId")
            guitars = guitars + (currentGuitarId -> guitar)
            sender() ! GuitarCreated(currentGuitarId)
            currentGuitarId += 1

        case IncrementStockGuitar(id, quantity) =>
            log.info(s"Adding stock to guitar $id in $quantity")
            guitars.get(id).map(guitar => {
                val guitarUpdated = Guitar(guitar.make, guitar.model, guitar.inventory + quantity)
                guitars + (id -> guitarUpdated)
                guitarUpdated
            }).fold(
                sender() ! GuitarNotFound
            )(
                guitar => sender() ! CurrentGuitarStock(guitar)
            )
    }
}

// step 2
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
    import GuitarDB._
    // step 3
    implicit val guitarFormat = jsonFormat3(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {
    import GuitarDB._

    implicit val system = ActorSystem("LowLevelRest")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    /*
        GET on /api/guitar => ALL the guitars in the store
        POST on /api/guitar => insert the guitar into the store
        GET on /api/guitar?id=X => fetches the guitar associated with id X
     */

    // JSON -> marshalling
    val simpleGuitar = Guitar("Fender", "Stratocaster", 1)
    println(simpleGuitar.toJson.prettyPrint)

    // unmarshalling
    val simpleGuitarJsonString =
        """
          |{
          |"make": "Fender",
          |"model": "Stratocaster",
          |"inventory": 2
          |}
          |""".stripMargin
    println(simpleGuitarJsonString.parseJson.convertTo[Guitar])
    /*
        setup
     */

    val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
    val guitarList = List(
        Guitar("Fender", "Stratocaster", 0),
        Guitar("Gibson", "Les Paul", 1),
        Guitar("Martin", "LX1", 1)
    )

    guitarList.foreach { guitar =>
        guitarDb ! CreateGuitar(guitar)
    }

    /*
        server code
     */
    implicit val defaultTimeout = Timeout(2 seconds)

    def getGuitar(query: Query): Future[HttpResponse] = {

        val guitarId = query.get("id").map(_.toInt) // Option[Int]
        guitarId match {
            case None => Future(HttpResponse(StatusCodes.NotFound)) // /api/guitar?id=
            case Some(id) =>
                val guitarFuture = (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]
                guitarFuture.map {
                    case None => HttpResponse(StatusCodes.NotFound)
                    case Some(guitar) =>
                        HttpResponse(
                            entity = HttpEntity(
                                ContentTypes.`application/json`,
                                guitar.toJson.prettyPrint
                            )
                        )
                }
        }
    }


    val requestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
            /*
                query parameter handling code
             */
            val query = uri.query() // query object <=> Map[String, String]
            if (query.isEmpty) {
                val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
                guitarsFuture.map { guitars =>
                    HttpResponse(
                        entity = HttpEntity(
                            ContentTypes.`application/json`,
                            guitars.toJson.prettyPrint
                        )
                    )
                }
            } else {
                // fetch guitar associated to the guitar id
                // /api/guitar/?id=45
                getGuitar(query)
            }


        case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
            // entities are a Source[ByteString]
            val strictEntityFuture = entity.toStrict(3 seconds)
            strictEntityFuture.flatMap { strictEntity =>
                val guitarJsonString = strictEntity.data.utf8String
                val guitar = guitarJsonString.parseJson.convertTo[Guitar]

                val guitarCreatedFuture: Future[GuitarCreated] = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
                guitarCreatedFuture.map { _ =>
                    HttpResponse(StatusCodes.OK)
                }

            }

        // - GET to /api/guitar/inventory?inStock=true/false which returns the guitars in stock as a JSON
        case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
            val inStock = uri.query().get("inStock").map(_.toBoolean)
            inStock match {
                case None => Future(HttpResponse(status = StatusCodes.NotFound))
                case Some(inStock) => (guitarDb ? FindAllGuitarsOnStock(inStock))
                    .mapTo[List[Guitar]]
                    .map(guitars =>
                        HttpResponse(
                            entity = HttpEntity(
                                ContentTypes.`application/json`,
                                guitars.toJson.prettyPrint
                            )
                        )
                    )
            }

        // - POST to /api/guitar/inventory?id=X&quantity=Y which adds Y guitars to the stock for guitar with id X
        case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
            val query = uri.query()
            val id = query.get("id").map(_.toInt)
            val quantityToAdd = query.get("quantity").map(_.toInt)
            // sequenceT the options
            val opt = for { id <- id; quantity <- quantityToAdd } yield (id, quantity)
            opt match {
                case None => Future(HttpResponse(status = StatusCodes.BadRequest))
                case Some((id, quantity)) => (guitarDb ? IncrementStockGuitar(id, quantity))
                  .map {
                      case GuitarNotFound => HttpResponse(status = StatusCodes.NotFound)
                      case CurrentGuitarStock(guitar) => HttpResponse(
                          entity = HttpEntity(
                              ContentTypes.`application/json`,
                              guitar.toJson.prettyPrint
                          )
                      )
                  }
            }


        case request: HttpRequest =>
            request.discardEntityBytes()
            Future {
                HttpResponse(status = StatusCodes.NotFound)
            }

    }

    Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

    /**
     * Exercise: enhance the Guitar case class ith a quantity field, by default 0
     * - GET to /api/guitar/inventory?inStock=true/false which returns the guitars in stock as a JSON
     * - POST to /api/guitar/inventory?id=X&quantity=Y which adds Y guitars to the stock for guitar with id X
     */
}
