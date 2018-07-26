package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.CRUD
import myproject.iam.{Authorization, Users}

class GetGroups extends ApiFunction{
  override val name = "get_groups"
  override val description = "get all groups in a given channel"

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val channelId = required(p.uuid("channel_id"))
    CRUD.getChannelGroups(channelId, Authorization.canListChannelGroups(user, _)) map { groups =>
      groups.map(_.toMap)
    }
  }
}
