package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Channels.CRUD
import myproject.iam.{Authorization, Users}

class GetGroups extends ApiFunction{
  override val name = "get_groups"
  override val description = "get all groups in a given channel"

  val channelId = ApiParameter("channel_id", ApiParameterType.UUID, "the target channel id")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    CRUD.getChannelGroups(channelId, Authorization.canListChannelGroups(user, _)) map { groups =>
      groups.map(_.serialize)
    }
  }
}
