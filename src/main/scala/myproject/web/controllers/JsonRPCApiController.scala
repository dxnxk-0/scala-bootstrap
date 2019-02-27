package myproject.web.controllers

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import com.typesafe.scalalogging.Logger
import myproject.api.ApiMapper
import myproject.common.Runtime.ec
import myproject.common._
import myproject.common.serialization.AkkaHttpMarshalling._
import myproject.common.serialization.JSONSerializer.JSONConfig
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.database.ApplicationDatabase
import myproject.iam.Users.UserGeneric
import myproject.web.jsonrpc.JsonRPC._
import myproject.web.server.WebAuth
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Success, Try}

object JsonRPCApiController extends Controller {

  private implicit val rpcSerializer = getJsonUnmarshaller[RPCRequest]
  private implicit val rpcBatchSerializer = getJsonUnmarshaller[Seq[RPCRequest]]
  private implicit val uuidUnmarshaller = getUUIDFromStringUnmarshaller

  private implicit val db = ApplicationDatabase.fromConfig

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
      decodeRequest {
        encodeResponse {
          corsHandler {
            handleRejections(RejectionHandler.default) { // As soon as the URL match we don't want to evaluate other URLs
              extractClientIP { ip =>
                authenticateOAuth2Async(realm = "rpc-api", authenticator = WebAuth.jwtAuthenticator) { user =>
                  post {
                    entity(as[RPCRequest]) { req =>
                      onComplete(processRpcRequest(user, req, ip.toOption.map(_.getHostAddress))) { res =>
                        completeOpRpc(res)
                      }
                    } ~
                    entity(as[Seq[RPCRequest]]) { batch =>
                      onComplete(processBatchRpcRequest(user, batch, ip.toOption.map(_.getHostAddress))) { res =>
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
      case _ if Option(req.params).isEmpty || !(Try(req.params.asInstanceOf[List[Any]]).isSuccess || Try(req.params.asInstanceOf[Map[String, Any]]).isSuccess) =>
        logger.info(s"[${execTime}ms] invalid RPC call with malformed parameters (method:${Option(req.method).getOrElse("undefined")})")
        Future.successful(RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.InvalidRequest.id, s"parameters missing or malformed")))

      /* Calling method */
      case method => callRpcMethod(req, method, user, clientIp).recover(throwableToRPCResponse(req)).andThen {
        case Success(res: RPCResponseError) =>
          logger.error(s"[${clientIp.getOrElse("-")}] [${user.login}] [${execTime}ms] [failed] [${req.method}] error:${res.error.message}")
        case Success(_: RPCResponseSuccess) =>
          logger.info(s"[${clientIp.getOrElse("-")}] [${user.login}] [${execTime}ms] [success] [${req.method}]")
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
    case RPCCode.TooManyLoginAttempts    => StatusCodes.TooManyRequests
    case RPCCode.LoginAlreadyExists      => StatusCodes.BadRequest
    case RPCCode.EmailAlreadyExists      => StatusCodes.BadRequest
    case RPCCode.ValidationError         => StatusCodes.BadRequest
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
    case DatabaseErrorException(msg) =>
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
    case EmailAlreadyExistsException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.EmailAlreadyExists.id, msg))
    case LoginAlreadyExistsException(msg) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.LoginAlreadyExists.id, msg))
    case ValidationErrorException(msg, errors) =>
      RPCResponseError(id = req.id, error = RPCErrorInfo(RPCCode.ValidationError.id, msg + ": " + errors.map(_.toString).mkString(",")))
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
