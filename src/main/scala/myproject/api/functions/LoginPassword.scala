package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Authorization
import myproject.iam.Users.CRUD

class LoginPassword extends ApiFunction {
  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val (login, password) = (p.nonEmptyString("login"), p.nonEmptyString("password"))
    CRUD.loginPassword(login, password, u => Authorization.canLogin(u, _)) map { authData =>
      Map("whoami" -> authData._1.toMap, "token" -> authData._2.toString)
    }
  }
}
