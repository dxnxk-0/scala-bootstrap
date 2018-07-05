package myproject.web.controllers

import java.util.UUID

import akka.http.scaladsl.server.RejectionHandler
import com.typesafe.scalalogging.Logger
import myproject.common.serialization.AkkaHttpMarshalling
import myproject.web.jsonrpc.{JsonRPCRequestHandler, JsonRPCResponseHandler, RPCRequest}
import myproject.web.server.{Rejections, WebAuth}
import org.slf4j.LoggerFactory

trait JsonRPCController extends JsonRPCRequestHandler with JsonRPCResponseHandler with AkkaHttpMarshalling with Rejections with WebAuth {

  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route


  private implicit val rpcSerializer = getJsonUnmarshaller[RPCRequest]
  private implicit val rpcBatchSerializer = getJsonUnmarshaller[Seq[RPCRequest]]
  private implicit val uuidFromStringUnmarshaller = getUuidFromStringUnmarshaller

  implicit private val logger = Logger(LoggerFactory.getLogger("jsonrpc-api"))

  val JsonRPCRoute: Route =
    path("api" / "rpc") {
      handleRejections(RejectionHandler.default) { // As soon as the URL match we don't want to evaluate other URLs
        optionalHeaderValueByName("Remote-Address") { ip =>
          parameters('euid.as[UUID].?) { euid =>
            respondWithJsonContentType {
              authenticateOAuth2PFAsync(realm = "rpc-api", authenticator = jwtAuthenticator) { user =>
                post {
                  entity(as[RPCRequest]) { req =>
                    onComplete(processRpcRequest(user, req, ip, euid)) { res =>
                      completeOpRpc(res)
                    }
                  } ~
                  entity(as[Seq[RPCRequest]]) { batch =>
                    onComplete(processBatchRpcRequest(user, batch, ip, euid)) { res =>
                      completeOpRpc(res)
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
