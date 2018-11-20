package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.iam.Users
import myproject.iam.Users.{CRUD, User, UserAccessChecker, UserDAO}

class GetPlatformUsers(implicit authz: User => UserAccessChecker, db: UserDAO) extends ApiFunction {
  override val name = "get_platform_users"
  override val doc = ApiSummaryDoc("get platform level users", "an array of object containing the user's data")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {

    implicit val checker = authz(user)

    CRUD.getPlatformUsers map (_.serialize)
  }
}
