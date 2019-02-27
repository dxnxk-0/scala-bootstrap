package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users.{CRUD, User, UserAccessChecker}

class DeleteUser(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction {
  override val name = "delete_user"
  override val doc = ApiSummaryDoc("delete a user permanently", "nothing is returned")


  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: User) = {
    val userId = required(p.uuid("user_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(userId) {
      CRUD.deleteUser(userId.get) map (_.serialize)
    }
  }
}
