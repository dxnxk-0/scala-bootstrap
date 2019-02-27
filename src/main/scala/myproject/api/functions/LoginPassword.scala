package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users.CRUD

class LoginPassword(implicit db: ApplicationDatabase)  extends ApiFunction {
  override val name = "login"
  override val doc = ApiSummaryDoc(
    description = "get a JWT login token using a login and a password",
    `return` = "an object containing a jwt token and the user's data")
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper) = {
    val login = required(p.nonEmptyString("login"))
    val password = required(p.nonEmptyString("password"))

    checkParamAndProcess(login, password) {
      CRUD.loginPassword(login.get, password.get) map { authData =>
        Map("whoami" -> authData._1.serialize, "token" -> authData._2.serialize)
      }
    }
  }
}
