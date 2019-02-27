package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users.CRUD

class LoginToken(implicit db: ApplicationDatabase)  extends ApiFunction {
  override val name = "login_token"
  override val doc = ApiSummaryDoc(
    description = "get a JWT login token using an authentication token uuid",
    `return` = "an object containing a jwt token and the user's data")
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper) = {
    val token = required(p.uuid("token"))

    checkParamAndProcess(token) {
      CRUD.loginToken(token.get) map { authData =>
        Map("whoami" -> authData._1.serialize, "token" -> authData._2.serialize)
      }
    }
  }
}
