package myproject.web.jsonrpc

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import myproject.common._
import myproject.common.serialization.JSONSerializer._
import myproject.common.serialization.{InvalidTypeException, JSONConfig, MissingKeyException, NullValueException}
import myproject.web.jsonrpc.JsonRPCErrorCodes.RPCCodes

import scala.util.{Success, Try}

trait JsonRPCResponseHandler {

  def rpcErrorCodeToHttpStatus(code: RPCCodes.Value) = code match {
    case RPCCodes.forbidden               => StatusCodes.Forbidden
    case RPCCodes.needAuthentication      => StatusCodes.Unauthorized
    case RPCCodes.internalError           => StatusCodes.InternalServerError
    case RPCCodes.invalidParams           => StatusCodes.BadRequest
    case RPCCodes.invalidRequest          => StatusCodes.BadRequest
    case RPCCodes.parseError              => StatusCodes.BadRequest
    case RPCCodes.serverError             => StatusCodes.InternalServerError
    case RPCCodes.methodNotFound          => StatusCodes.NotFound
    case RPCCodes.objectNotFound          => StatusCodes.NotFound
    case RPCCodes.locked                  => StatusCodes.Locked
    case RPCCodes.passwordPolicyViolation => StatusCodes.BadRequest
    case RPCCodes.importError             => StatusCodes.BadRequest
    case RPCCodes.tooManyLoginAttempts    => StatusCodes.TooManyRequests
  }

  def throwableToRPCResponse(req: RPCRequest): PartialFunction[Throwable, RPCResponse] = {
    case _ if req.id.isEmpty => RPCNotificationResponse() // The client does not care the error
    case InvalidTypeException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.invalidParams.id, msg))
    case MissingKeyException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.invalidParams.id, msg))
    case NullValueException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.invalidParams.id, msg))
    case ObjectNotFoundException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.objectNotFound.id, msg))
    case UnexpectedErrorException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.internalError.id, msg))
    case AuthenticationFailedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.needAuthentication.id, msg))
    case AuthenticationNeededException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.needAuthentication.id, msg))
    case AccessRefusedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.forbidden.id, msg))
    case e: Exception =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.serverError.id, e.getMessage))
    case unknown =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCodes.internalError.id, s"Cannot map RPC response from an unknown type ($unknown)"))
  }

  implicit final def jsonMarshaller[A: Manifest]: ToEntityMarshaller[A] =
    Marshaller.
      withFixedContentType(`application/json`) { v =>
        toJson(v)
      }

  def completeOpRpc(result: Try[RPCResponse])(implicit logger: Logger, jsonConfig: JSONConfig = JSONConfig(false)): Route = ctx => {
    result match {
      case Success(_: RPCNotificationResponse) => ctx.complete(StatusCodes.NoContent)
      case Success(obj: RPCResponseSuccess) => ctx.complete(obj)
      case Success(response: RPCResponseError) =>
        ctx.complete((rpcErrorCodeToHttpStatus(RPCCodes(response.error.code)), response))
      case e =>
        logger.error(s"Does not know how to handle $e (${e.getClass}}) in RPC response handler")
        ctx.complete((
          StatusCodes.InternalServerError,
          RPCResponseError(id = None, error = RPCErrorInfo(-32603, s"Does not know how to handle $e (${e.getClass}}) in RPC response handler"))))
    }
  }

  def completeOpRpcBatch(result: Try[List[RPCResponse]])(implicit logger: Logger, jsonConfig: JSONConfig = JSONConfig(false)): Route = { ctx =>
    result match {
      case Success(batchResponse) => ctx.complete(batchResponse.filterNot(r => r.isInstanceOf[RPCNotificationResponse]))
      case e => // A batch is always successful
        logger.error(s"Does not know how to handle $e (${e.getClass}}) in RPC response handler")
        ctx.complete((
          StatusCodes.InternalServerError,
          RPCResponseError(id = None, error = RPCErrorInfo(-32603, s"Does not know how to handle $e (${e.getClass}}) in RPC response handler"))))
    }
  }
}
