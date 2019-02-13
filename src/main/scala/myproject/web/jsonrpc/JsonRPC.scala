package myproject.web.jsonrpc

import com.fasterxml.jackson.annotation.JsonInclude
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

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
}
