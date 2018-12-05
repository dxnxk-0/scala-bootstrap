package myproject.web.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import myproject.Config
import myproject.common.DataInitializer
import myproject.database.{ApplicationDatabase, SlickConfig}
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object WebServer extends App {

  private implicit val system = ActorSystem("actor-system")
  private implicit val materializer = ActorMaterializer()

  private implicit val logger = Logger(LoggerFactory.getLogger("web-server"))
  implicit val db = ApplicationDatabase.currentDatabaseImpl

  val iface = Config.server.interface
  val port = Config.server.port

  // H2 DB creation
  if(SlickConfig.driver==H2Profile) {
    db.reset

    // Data initialization if required
    if (Config.datainit.enabled) {
      DataInitializer.Instance.initialize
    }
  }

  Http().bindAndHandle(Routes.httpRoutes, iface, port)

  logger.info(s"Started server on $iface:$port")

  Await.result(system.whenTerminated, Duration.Inf)
}
