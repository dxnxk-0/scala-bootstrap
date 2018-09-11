package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Channels.{CRUD, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class GetChannels(implicit authz: User => ChannelAccessChecker, db: ChannelDAO) extends ApiFunction {
  override val name = "get_channels"
  override val doc = ApiSummaryDoc(
    description = "get all the existing channels on the platform (requires high privileges such as platform administrator's",
    `return` = "a list of objects containing the requested channel's data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    implicit val checker = authz(user)

    CRUD.getAllChannels map { channels =>
      channels.map(_.toMap)
    }
  }
}
