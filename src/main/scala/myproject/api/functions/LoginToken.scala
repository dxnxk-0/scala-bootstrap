package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Tokens.TokenDAO
import myproject.iam.Users.{CRUD, UserDAO}

class LoginToken(implicit db: UserDAO with GroupDAO with ChannelDAO with TokenDAO)  extends ApiFunction {
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
