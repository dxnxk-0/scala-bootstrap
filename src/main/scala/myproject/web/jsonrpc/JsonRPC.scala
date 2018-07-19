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
import myproject.common.serialization.OpaqueData.{InvalidTypeException, MissingKeyException, NullValueException, ReifiedDataWrapper}
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
    val parseError              = Value(-32700, "parse_error")
    val invalidRequest          = Value(-32600, "invalid_request")
    val methodNotFound          = Value(-32601, "method_not_found")
    val invalidParams           = Value(-32602, "invalid_params")
    val internalError           = Value(-32603, "internal_error")
    // Private codes above code -32000
    val serverError             = Value(-32099, "server_error")
    val forbidden               = Value(-32098, "forbidden")
    val needAuthentication      = Value(-32097, "need_authentication")
    val objectNotFound          = Value(-32096, "object_not_found")
    val locked                  = Value(-32095, "locked")
    val passwordPolicyViolation = Value(-32094, "password_policy_violation")
    val importError             = Value(-32093, "import_error")
    val tooManyLoginAttempts    = Value(-32092, "too_many_login_attempts")
  }

  def processRpcRequest(user: UserGeneric, req: RPCRequest, clientIp: Option[String])(implicit logger: Logger): Future[RPCResponse] = {

    /* Computing request execution time */
    val start = System.currentTimeMillis
    def execTime = System.currentTimeMillis - start

    req.method match {

      /* No request method */
      case _ if Option(req.method).isEmpty /* null */ || req.method.isEmpty =>
        logger.info(s"[${execTime}ms] invalid RPC call with an undefined method")
        Future.successful(RPCResponseError(id = None, error = RPCErrorInfo(RPCCode.invalidRequest.id, s"null or empty request method")))

      /* Parameters undefined or invalid */
      case _ if Option(req.params).isEmpty /* null */ || !(Try(req.params.asInstanceOf[List[Any]]).isSuccess || Try(req.params.asInstanceOf[Map[String, Any]]).isSuccess) =>
        logger.info(s"[${execTime}ms] invalid RPC call with malformed parameters (method:${Option(req.method).getOrElse("undefined")})")
        Future.successful(RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.invalidRequest.id, s"parameters missing or malformed")))

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

  def processBatchRpcRequest(user: UserGeneric, batch: Seq[RPCRequest], clientIp: Option[String]): Future[Seq[RPCResponse]] = ???

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
    case RPCCode.forbidden               => StatusCodes.Forbidden
    case RPCCode.needAuthentication      => StatusCodes.Unauthorized
    case RPCCode.internalError           => StatusCodes.InternalServerError
    case RPCCode.invalidParams           => StatusCodes.BadRequest
    case RPCCode.invalidRequest          => StatusCodes.BadRequest
    case RPCCode.parseError              => StatusCodes.BadRequest
    case RPCCode.serverError             => StatusCodes.InternalServerError
    case RPCCode.methodNotFound          => StatusCodes.NotFound
    case RPCCode.objectNotFound          => StatusCodes.NotFound
    case RPCCode.locked                  => StatusCodes.Locked
    case RPCCode.passwordPolicyViolation => StatusCodes.BadRequest
    case RPCCode.importError             => StatusCodes.BadRequest
    case RPCCode.tooManyLoginAttempts    => StatusCodes.TooManyRequests
  }

  private def throwableToRPCResponse(req: RPCRequest): PartialFunction[Throwable, RPCResponse] = {
    case _ if req.id.isEmpty => RPCNotificationResponse() // The client does not care the error
    case InvalidTypeException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.invalidParams.id, msg))
    case MissingKeyException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.invalidParams.id, msg))
    case NullValueException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.invalidParams.id, msg))
    case ObjectNotFoundException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.objectNotFound.id, msg))
    case UnexpectedErrorException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.internalError.id, msg))
    case AuthenticationFailedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.needAuthentication.id, msg))
    case AuthenticationNeededException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.needAuthentication.id, msg))
    case AccessRefusedException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.forbidden.id, msg))
    case e: Exception =>
      logger.error(s"Cannot map RPC response from an unknown type $e: " + e.getClass + ": " + e.getMessage)
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.serverError.id, "An error as occurred"))
    case unknown =>
      logger.error(s"Cannot map RPC response from an unknown type ($unknown)")
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.internalError.id, "An error as occured"))
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
