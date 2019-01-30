package myproject.web.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import myproject.Config
import myproject.common.FutureImplicits._
import myproject.database.{ApplicationDatabase, DataLoader, DatabaseType}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object WebServer extends App {

  private implicit val system = ActorSystem("actor-system")
  private implicit val materializer = ActorMaterializer()

  private implicit val logger = Logger(LoggerFactory.getLogger("web-server"))

  Config.dumpLog()

  implicit val db = ApplicationDatabase.currentDatabaseImpl

  val iface = Config.Server.interface
  val port = Config.Server.port

  if(db.dbType==DatabaseType.H2) {
    logger.info("H2 database detected")
    if(Config.Database.H2.startWebInterface) {
      val h2Server = org.h2.tools.Server.createWebServer()
      logger.info(s"started h2 web interface on ${h2Server.getURL}")
      h2Server.start()
    }

    if(Config.Database.H2.initAtStartup) {
      logger.info("cleaning database")
      db.clean.futureValue
      logger.info("done cleaning database")
      logger.info("loading data into h2")
      db.migrate.futureValue
      DataLoader.instanceFromConfig.load.futureValue
      logger.info("done loading data")
    }
  }

  if(Config.Server.migrateDbAtStartup) {
    logger.info("migrating database")
    db.migrate.futureValue
    logger.info("done migrating database")
  }

  Http().bindAndHandle(Routes.httpRoutes, iface, port)

  logger.info(s"started server on $iface:$port")

  Await.result(system.whenTerminated, Duration.Inf)
}