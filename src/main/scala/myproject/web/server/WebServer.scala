package myproject.web.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import myproject.Config
import myproject.common.DefaultExecutionContext
import myproject.web.controllers.JsonRPCController
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object WebServer extends App with Routes with JsonRPCController with DefaultExecutionContext with EnvInit {

  private implicit val system = ActorSystem("actor-system")
  private implicit val materializer = ActorMaterializer()

  private implicit val logger = Logger(LoggerFactory.getLogger("web-server"))

  val iface = Config.server.interface
  val port = Config.server.port

  initEnv()

  Http().bindAndHandle(httpRoutes, iface, port)

  logger.info(s"Started server on $iface:$port")

  Await.result(system.whenTerminated, Duration.Inf)
}
