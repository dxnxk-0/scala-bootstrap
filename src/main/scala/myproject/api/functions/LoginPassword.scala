package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization
import myproject.iam.Users.CRUD

class LoginPassword extends ApiFunction {
  override val name = "login"
  override val description = "get a JWT login token using a login and a password"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = required(p.nonEmptyString("login"))
    val password = required(p.nonEmptyString("password"))

    CRUD.loginPassword(login, password, u => Authorization.canLogin(u, _)) map { authData =>
      Map("whoami" -> authData._1.toMap, "token" -> authData._2.toString)
    }
  }
}
