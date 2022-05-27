package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import part2_lowlevelserver.LowLevelHttps.getClass

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsContext {
  // STEPS FOR A SELF-SIGNED CERTIFICATE, TO OPERATE IN BROWSER WE NEED A CERTS FROM CERTIFICATE AUTHORITY

  // Step 1: key store
  val ks: KeyStore = KeyStore.getInstance("PKCS12")
  val keystoreFile: InputStream = getClass.getClassLoader.getResourceAsStream("myfile.p12")
  // alternative: new FileInputStream(new File("src/main/resources/keystore.pkcs12"))
  val password = "123456".toCharArray // fetch the password from a secure place!
  ks.load(keystoreFile, password)

  // Step 2: initialize a key manager
  // format of certificates based on the so-called X509 public key infrastructure
  // PKI = public key infraestructure
  val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)
  // key manager factory manages the Https certificates within a key store

  // Step 3: initialize a trust manager
  val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(ks)
  // trust manager factory manager who signed those certificates

  // Step 4: initialize an SSL context
  val sslContext: SSLContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)
  // tls comes to transport layer security

  // Step 5: return the https connection context
  val httpsConnectionContext: HttpsConnectionContext = ConnectionContext.https(sslContext)
}


object LowLevelHttps extends App {

  implicit val system = ActorSystem("LowLevelHttps")
  implicit val materializer = ActorMaterializer()



  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, uri, headers, entity, protocol) =>
      HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |     OOPS! The resource can't be found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpsBinding = Http().bindAndHandleSync(requestHandler, "localhost", 8443, HttpsContext.httpsConnectionContext)

}
