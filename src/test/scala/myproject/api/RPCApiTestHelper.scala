package myproject.api

import java.nio.charset.StandardCharsets
import java.util.UUID

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import myproject.common.serialization.JSONSerializer._
import myproject.web.controllers.JsonRPCApiController
import myproject.web.jsonrpc.RPCRequest
import test.DatabaseSpec

trait RPCApiTestHelper extends DatabaseSpec with ScalatestRouteTest with JsonRPCApiController {

  def postRpc(payload: String, token: Option[String] = None, euid: Option[UUID] = None): RouteTestResult = {
    val post = Post("/api/rpc" + euid.map(i => s"?euid=$i").getOrElse(""), HttpEntity(MediaTypes.`application/json`, payload.getBytes(StandardCharsets.UTF_8)))

    val postWithAuth = token match {
      case None => post
      case Some(t) => post ~> addCredentials(OAuth2BearerToken(t))
    }

    postWithAuth ~> Route.seal(JsonRPCRoute)
  }

  def generateRPCPayload(method: String, params: AnyRef, id: Option[Int] = Some(1)) = toJson(RPCRequest(method, params, id))
}
