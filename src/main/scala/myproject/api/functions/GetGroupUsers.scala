package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users
import myproject.iam.Users.{CRUD, User, UserAccessChecker}

class GetGroupUsers(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction {
  override val name = "get_group_users"
  override val doc = ApiSummaryDoc(
    description = "get all users in a given group (requires at least group administration capability or higher privileges)",
    `return` = "a list of objects containing the group's members data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User) = {
    val groupId = required(p.uuid("group_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId) {
      CRUD.getGroupUsers(groupId.get) map (_.serialize)
    }
  }
}
