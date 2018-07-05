package myproject.web.server

import akka.http.scaladsl.server.Directives.{decodeRequest, encodeResponse}
import myproject.web.controllers.JsonRPCController

trait Routes extends JsonRPCController with WebAuth with Rejections {

  val handleEncoding = decodeRequest | encodeResponse

  val httpRoutes = handleEncoding { JsonRPCRoute }
}
