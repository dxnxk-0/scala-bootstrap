package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.{Authorization, Users}

class LoginPassword extends ApiFunction {
  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  val login = ApiParameter("login", ApiParameterType.String, "the user's login name")
  val password = ApiParameter("password", ApiParameterType.String, "the user's password")

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    for {
      authData <- Users.CRUD.loginPassword(login, password, u => Authorization.canLogin(u, _))
    } yield Map("whoami" -> authData._1.serialize, "token" -> authData._2)
  }
}
