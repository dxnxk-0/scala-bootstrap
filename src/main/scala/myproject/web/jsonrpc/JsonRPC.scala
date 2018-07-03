package myproject.web.jsonrpc

import com.fasterxml.jackson.annotation.JsonInclude

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
