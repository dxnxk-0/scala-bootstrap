package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.User

import scala.concurrent.Future

class Welcome extends ApiFunction {
  override val name = "welcome"
  override val doc = ApiSummaryDoc(
    description = "A bit of kindness in this cold world",
    `return` = "a string saying a welcome message")

  override def process(implicit p: ReifiedDataWrapper, user: User) = Future {
    s"Welcome ${user.login} !"
  }
}
