package myproject.api.functions

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers._
import myproject.iam.Users
import myproject.iam.Authorization._
import myproject.common.FutureImplicits._

case object LoginPassword extends ApiFunction {
  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = p.string("login")
    val candidate = p.string("password")

    for {
      authData <- Users.CRUD.loginPassword(login, candidate)
      _ <- authzLoginAccess(authData._1).toFuture
    } yield Map("whoami" -> authData._1.serialize, "token" -> authData._2)
  }
}
