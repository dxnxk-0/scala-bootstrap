package myproject.web.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import myproject.Config
import myproject.common.FutureImplicits._
import myproject.database.{ApplicationDatabase, DataLoader, DatabaseType}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object WebServer extends App {

  private implicit val system = ActorSystem("actor-system")
  private implicit val materializer = ActorMaterializer()

  private implicit val logger = Logger(LoggerFactory.getLogger("web-server"))
  implicit val db = ApplicationDatabase.currentDatabaseImpl

  val iface = Config.Server.interface
  val port = Config.Server.port

  // H2 DB creation
  if(db.dbType==DatabaseType.H2) {
    db.init

    if(Config.Database.H2.startWebInterface) {
      val h2Server = org.h2.tools.Server.createWebServer()
      logger.info(s"started h2 web interface on ${h2Server.getURL}")
      h2Server.start()
    }

    // Data initialization if required
    if (Config.DataLoading.enabled) {
      DataLoader.instanceFromConfig.load.futureValue
    }
  }

  Http().bindAndHandle(Routes.httpRoutes, iface, port)

  Config.dumpLog

  logger.info(s"started server on $iface:$port")

  Await.result(system.whenTerminated, Duration.Inf)
}