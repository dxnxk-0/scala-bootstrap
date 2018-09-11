package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Channels.CRUD
import myproject.iam.Users

class GetGroups extends ApiFunction{
  override val name = "get_groups"
  override val doc = ApiSummaryDoc(
    description = "get all groups in a given channel (requires at least channel admin rights)",
    `return` = "a list of objects containing the requested channel's groups data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val authz = new DefaultIAMAccessChecker(Some(user))

    checkParamAndProcess(channelId) flatMap { _ =>
      CRUD.getChannelGroups(channelId.get) map { groups =>
        groups.map(_.toMap)
      }
    }
  }
}
