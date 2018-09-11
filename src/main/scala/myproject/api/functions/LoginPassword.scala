package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Users.{CRUD, UserDAO}

class LoginPassword(implicit db: UserDAO with GroupDAO with ChannelDAO)  extends ApiFunction {
  override val name = "login"
  override val doc = ApiSummaryDoc(
    description = "get a JWT login token using a login and a password",
    `return` = "an object containing a jwt token and the user's data")
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = required(p.nonEmptyString("login"))
    val password = required(p.nonEmptyString("password"))

    checkParamAndProcess(login, password) flatMap { _ =>
      CRUD.loginPassword(login.get, password.get) map { authData =>
        Map("whoami" -> authData._1.toMap, "token" -> authData._2.toString)
      }
    }
  }
}
