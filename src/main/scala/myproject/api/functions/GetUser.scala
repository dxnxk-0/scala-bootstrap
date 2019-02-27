package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users.{CRUD, User, UserAccessChecker}

class GetUser(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction {
  override val name = "get_user"
  override val doc = ApiSummaryDoc(
    description = "get an existing user's data (requires to be the user himself or to have higher privileges",
    `return` = "an object containing the requested user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User) = {
    val userId = required(p.uuid("user_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(userId) {
      CRUD.getUser(userId.get) map (_.serialize)
    }
  }
}
