package myproject.web.jsonrpc

object JsonRPCErrorCodes {

  object RPCCodes extends Enumeration {
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
}
