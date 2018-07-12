package myproject.web.server

import akka.http.scaladsl.server.Directives._
import myproject.web.controllers.{AppController, HelloController, JsonRPCApiController}

trait Routes extends WebAuth
  with JsonRPCApiController
  with HelloController
  with AppController {

  val handleEncoding = decodeRequest | encodeResponse

  val httpRoutes = handleEncoding {
    JsonRPCRoute ~ HelloRoute ~ AppRoute
  }
}
