package myproject.web.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import com.typesafe.scalalogging.Logger
import myproject.common.serialization.AkkaHttpMarshalling._
import myproject.web.jsonrpc.JsonRPC._
import myproject.web.server.WebAuth
import org.slf4j.LoggerFactory

object JsonRPCApiController extends Controller {

  private implicit val rpcSerializer = getJsonUnmarshaller[RPCRequest]
  private implicit val rpcBatchSerializer = getJsonUnmarshaller[Seq[RPCRequest]]
  private implicit val uuidUnmarshaller = getUUIDFromStringUnmarshaller

  implicit private val logger = Logger(LoggerFactory.getLogger("jsonrpc-api"))

  val JsonRPCRoute: Route =
    path("api" / "rpc") {
      handleRejections(RejectionHandler.default) { // As soon as the URL match we don't want to evaluate other URLs
        optionalHeaderValueByName("Remote-Address") { ip =>
          authenticateOAuth2Async(realm = "rpc-api", authenticator = WebAuth.jwtAuthenticator) { user =>
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
