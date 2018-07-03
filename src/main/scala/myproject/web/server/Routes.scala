package myproject.web.server

import akka.http.scaladsl.server.Directives.{decodeRequest, encodeResponse}
import myproject.web.jsonrpc.JsonRPCRoute

trait Routes extends JsonRPCRoute with WebAuth with Rejections {

  val handleEncoding = decodeRequest | encodeResponse

  val httpRoutes = handleEncoding { JsonRPCRoute }
}
