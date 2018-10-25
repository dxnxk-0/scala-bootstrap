package myproject.web.jsonrpc

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.fasterxml.jackson.annotation.JsonInclude
import com.typesafe.scalalogging.Logger
import myproject.api.ApiMapper
import myproject.common.Runtime.ec
import myproject.common._
import myproject.common.serialization.AkkaHttpMarshalling._
import myproject.common.serialization.JSONSerializer._
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.UserGeneric
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Success, Try}

object JsonRPC {

  private val logger = Logger(LoggerFactory.getLogger("jsonrpc-module"))

  case class RPCRequest(method: String, params: AnyRef, id: Option[Int], jsonrpc: String = "2.0")

  trait RPCResponse

  case class RPCNotificationResponse()
    extends RPCResponse

  case class RPCResponseSuccess(
      jsonrpc: String = "2.0",
      id: Option[Int],
      @JsonInclude(JsonInclude.Include.ALWAYS)
      result: Option[Any])
    extends RPCResponse

  case class RPCResponseError(jsonrpc: String = "2.0", error: RPCErrorInfo, id: Option[Int], data: Option[Map[String, _]] = None)
    extends RPCResponse

  case class RPCErrorInfo(code: Int, message: String)

  object RPCCode extends Enumeration {
    type RPCCode                = Value
    val ParseError              = Value(-32700, "parse_error")
    val InvalidRequest          = Value(-32600, "invalid_request")
    val MethodNotFound          = Value(-32601, "method_not_found")
    val InvalidParams           = Value(-32602, "invalid_params")
    val InternalError           = Value(-32603, "internal_error")
    // Private codes above code -32000
    val ServerError             = Value(-32099, "server_error")
    val Forbidden               = Value(-32098, "forbidden")
    val NeedAuthentication      = Value(-32097, "need_authentication")
    val ObjectNotFound          = Value(-32096, "object_not_found")
    val Locked                  = Value(-32095, "locked")
    val PasswordPolicyViolation = Value(-32094, "password_policy_violation")
    val ImportError             = Value(-32093, "import_error")
    val TooManyLoginAttempts    = Value(-32092, "too_many_login_attempts")
  }

  def processRpcRequest(user: UserGeneric, req: RPCRequest, clientIp: Option[String])(implicit logger: Logger): Future[RPCResponse] = {

    /* Computing request execution time */
    val start = System.currentTimeMillis
    def execTime = System.currentTimeMillis - start

    req.method match {

      /* No request method */
      case _ if Option(req.method).isEmpty /* null */ || req.method.isEmpty =>
        logger.info(s"[${execTime}ms] invalid RPC call with an undefined method")
        Future.successful(RPCResponseError(id = None, error = RPCErrorInfo(RPCCode.InvalidRequest.id, s"null or empty request method")))

      /* Parameters undefined or invalid */
      case _ if Option(req.params).isEmpty /* null */ || !(Try(req.params.asInstanceOf[List[Any]]).isSuccess || Try(req.params.asInstanceOf[Map[String, Any]]).isSuccess) =>
        logger.info(s"[${execTime}ms] invalid RPC call with malformed parameters (method:${Option(req.method).getOrElse("undefined")})")
        Future.successful(RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, s"parameters missing or malformed")))

      /* Calling method */
      case method => callRpcMethod(req, method, user, clientIp).recover(throwableToRPCResponse(req)).andThen { //TODO: Implement effective user
        case Success(res: RPCResponseError) =>
          logger.error(s"[${clientIp.getOrElse("-")}] [${user.login}] [${execTime}ms] [failed] [${req.method}] error:${res.error.message}")
        case Success(_: RPCResponseSuccess) =>
          //logger.info(s"[${clientIp.getOrElse("-")}] [${user.login}${effectiveUser.map(eu => s" as ${eu.login}").getOrElse("")}] [${execTime}ms] [success] [${req.method}]")
          logger.info(s"[${clientIp.getOrElse("-")}] [${user.login})] [${execTime}ms] [success] [${req.method}]")
      }
    }
  }

  def processBatchRpcRequest(user: UserGeneric, batch: Seq[RPCRequest], clientIp: Option[String])(implicit logger: Logger): Future[Seq[RPCResponse]] = {
    Future.sequence {
      batch.map(req => processRpcRequest(user, req, clientIp))
    }
  }

  private def callRpcMethod(req: RPCRequest, methodName: String, user: UserGeneric, clientIp: Option[String]): Future[RPCResponse] = {
    ApiMapper.dispatchRequest(user, methodName, clientIp)(new ReifiedDataWrapper(req.params)) map { result =>
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

  def rpcErrorCodeToHttpStatus(code: RPCCode.Value) = code match {
    case RPCCode.Forbidden               => StatusCodes.Forbidden
    case RPCCode.NeedAuthentication      => StatusCodes.Unauthorized
    case RPCCode.InternalError           => StatusCodes.InternalServerError
    case RPCCode.InvalidParams           => StatusCodes.BadRequest
    case RPCCode.InvalidRequest          => StatusCodes.BadRequest
    case RPCCode.ParseError              => StatusCodes.BadRequest
    case RPCCode.ServerError             => StatusCodes.InternalServerError
    case RPCCode.MethodNotFound          => StatusCodes.NotFound
    case RPCCode.ObjectNotFound          => StatusCodes.NotFound
    case RPCCode.Locked                  => StatusCodes.Locked
    case RPCCode.PasswordPolicyViolation => StatusCodes.BadRequest
    case RPCCode.ImportError             => StatusCodes.BadRequest
    case RPCCode.TooManyLoginAttempts    => StatusCodes.TooManyRequests
  }

  private def throwableToRPCResponse(req: RPCRequest): PartialFunction[Throwable, RPCResponse] = {
    case _ if req.id.isEmpty => RPCNotificationResponse() // The client does not care the error
    case InvalidTypeException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidParams.id, msg))
    case MissingKeyException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidParams.id, msg))
    case NullValueException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidParams.id, msg))
    case InvalidParametersException(msg, errors) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidParams.id, msg), data = Some(Map("errors" -> errors)))
    case ObjectNotFoundException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.ObjectNotFound.id, msg))
    case UnexpectedErrorException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InternalError.id, msg))
    case AuthenticationFailedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.NeedAuthentication.id, msg))
    case AuthenticationNeededException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.NeedAuthentication.id, msg))
    case AccessRefusedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.Forbidden.id, msg))
    case NotImplementedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InternalError.id, msg))
    case InvalidContextException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, msg))
    case TokenExpiredException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, msg))
    case IllegalOperationException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, msg))
    case UniquenessCheckException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, msg))
    case ValidationErrorException(msg, errors) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, msg + ": " + errors.map(_.toString).mkString(",")))
    case e: Exception =>
      logger.error(s"Cannot map RPC response from an unknown type $e: " + e.getClass + ": " + e.getMessage)
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.ServerError.id, "An error as occurred"))
    case unknown =>
      logger.error(s"Cannot map RPC response from an unknown type ($unknown)")
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InternalError.id, "An error as occured"))
  }

  implicit val respondJsonSingle = getJsonMarshaller[RPCResponse]
  implicit val respondJsonBatch = getJsonMarshaller[Seq[RPCResponse]]

  def completeOpRpc(result: Try[RPCResponse])(implicit logger: Logger, jsonConfig: JSONConfig = JSONConfig(false)): Route = ctx => {
    result match {
      case Success(_: RPCNotificationResponse) => ctx.complete(StatusCodes.NoContent)
      case Success(obj: RPCResponseSuccess) => ctx.complete(obj)
      case Success(response: RPCResponseError) =>
        ctx.complete((rpcErrorCodeToHttpStatus(RPCCode(response.error.code)), response))
      case e =>
        logger.error(s"Does not know how to handle $e (${e.getClass}}) in RPC response handler")
        ctx.complete((
          StatusCodes.InternalServerError,
          RPCResponseError(id = None, error = RPCErrorInfo(-32603, s"Does not know how to handle $e (${e.getClass}}) in RPC response handler"))))
    }
  }

  def completeOpRpcBatch(result: Try[Seq[RPCResponse]])(implicit logger: Logger, jsonConfig: JSONConfig = JSONConfig(false)): Route = { ctx =>
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
