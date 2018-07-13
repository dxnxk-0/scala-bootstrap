package myproject.web.server

import akka.http.scaladsl.server.Directives._
import myproject.web.controllers.AppController.AppRoute
import myproject.web.controllers.HelloController.HelloRoute
import myproject.web.controllers.JsonRPCApiController.JsonRPCRoute

object Routes {

  private val handleEncoding = decodeRequest | encodeResponse

  val httpRoutes = handleEncoding {
    JsonRPCRoute ~ HelloRoute ~ AppRoute
  }
}
