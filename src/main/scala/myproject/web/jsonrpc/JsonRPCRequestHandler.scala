package myproject.web.jsonrpc

import java.util.UUID

import com.typesafe.scalalogging.Logger
import myproject.common.serialization.ReifiedDataWrapper
import myproject.identity.User
import myproject.web.api.ApiMapper
import myproject.web.jsonrpc.JsonRPCErrorCodes.RPCCodes

import scala.concurrent.Future
import scala.util.{Success, Try}

trait JsonRPCRequestHandler extends ApiMapper with JsonRPCResponseHandler {

  def processRpcRequest(user: User, req: RPCRequest, clientIp: Option[String], euid: Option[UUID])(implicit logger: Logger): Future[RPCResponse] = {

    /* Computing request execution time */
    val start = System.currentTimeMillis
    def execTime = System.currentTimeMillis - start

    req.method match {

      /* No request method */
      case _ if Option(req.method).isEmpty /* null */ || req.method.isEmpty =>
        logger.info(s"[${execTime}ms] invalid RPC call with an undefined method")
        Future.successful(RPCResponseError(id = None, error = RPCErrorInfo(RPCCodes.invalidRequest.id, s"null or empty request method")))

      /* Parameters undefined or invalid */
      case _ if Option(req.params).isEmpty /* null */ || !(Try(req.params.asInstanceOf[List[Any]]).isSuccess || Try(req.params.asInstanceOf[Map[String, Any]]).isSuccess) =>
        logger.info(s"[${execTime}ms] invalid RPC call with malformed parameters (method:${Option(req.method).getOrElse("undefined")})")
        Future.successful(RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.invalidRequest.id, s"parameters missing or malformed")))

      /* Calling method */
      case method => callRpcMethod(req, method, user, None, clientIp) recover throwableToRPCResponse(req) andThen { //TODO: Implement effective user
        case Success(res: RPCResponseError) =>
          logger.error(s"[${clientIp.getOrElse("-")}] [${user.login}] [${execTime}ms] [failed] [${req.method}] error:${res.error.message}")
        case Success(_: RPCResponseSuccess) =>
          //logger.info(s"[${clientIp.getOrElse("-")}] [${user.login}${effectiveUser.map(eu => s" as ${eu.login}").getOrElse("")}] [${execTime}ms] [success] [${req.method}]")
          logger.info(s"[${clientIp.getOrElse("-")}] [${user.login})] [${execTime}ms] [success] [${req.method}]")
      }
    }
  }

  def processBatchRpcRequest(user: User, batch: Seq[RPCRequest], clientIp: Option[String], euid: Option[UUID]): Future[RPCResponse] = ???

  private def callRpcMethod(req: RPCRequest, methodName: String, realUser: User, effectiveUser: Option[User], clientIp: Option[String]) = {
    dispatchRequest(realUser, effectiveUser, methodName, clientIp)(new ReifiedDataWrapper(req.params)) map { result =>
      if (req.id.isEmpty)
        RPCNotificationResponse()
      else
        RPCResponseSuccess(
          id = req.id,
          result = result match {
            case Unit => None
            case _ => Some(result)
          })
    }
  }


}
