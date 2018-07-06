package myproject.web.server

import akka.http.scaladsl.server.Directives._
import myproject.web.controllers.{HelloController, JsonRPCApiController}

trait Routes extends WebAuth with Rejections
  with JsonRPCApiController
  with HelloController {

  val handleEncoding = decodeRequest | encodeResponse

  val httpRoutes = handleEncoding {
    JsonRPCRoute ~ HelloRoute
  }
}
