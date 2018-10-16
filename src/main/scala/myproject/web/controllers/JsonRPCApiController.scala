package myproject.web.controllers

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import com.typesafe.scalalogging.Logger
import myproject.common.serialization.AkkaHttpMarshalling._
import myproject.database.ApplicationDatabase
import myproject.web.jsonrpc.JsonRPC._
import myproject.web.server.WebAuth
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.HttpMethods._

object JsonRPCApiController extends Controller {

  private implicit val rpcSerializer = getJsonUnmarshaller[RPCRequest]
  private implicit val rpcBatchSerializer = getJsonUnmarshaller[Seq[RPCRequest]]
  private implicit val uuidUnmarshaller = getUUIDFromStringUnmarshaller

  private implicit val db = ApplicationDatabase.currentDatabaseImpl

  implicit private val logger = Logger(LoggerFactory.getLogger("jsonrpc-api"))

  private def addAccessControlHeaders = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`(HttpOriginRange.*),
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
    )
  }

  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }

  private def corsHandler(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }

  val JsonRPCRoute: Route =
    path("api" / "rpc") {
      corsHandler {
        handleRejections(RejectionHandler.default) { // As soon as the URL match we don't want to evaluate other URLs
          optionalHeaderValueByName("Remote-Address") { ip =>
            authenticateOAuth2Async(realm = "rpc-api", authenticator = WebAuth.jwtAuthenticator) { user =>
              respondWithHeaders(`Access-Control-Allow-Origin`(HttpOriginRange.*)) {
                post {
                  entity(as[RPCRequest]) { req =>
                    onComplete(processRpcRequest(user, req, ip)) { res =>
                      completeOpRpc(res)
                    }
                  } ~
                  entity(as[Seq[RPCRequest]]) { batch =>
                    onComplete(processBatchRpcRequest(user, batch, ip)) { res =>
                      completeOpRpcBatch(res)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
}
