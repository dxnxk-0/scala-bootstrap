package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Channels.CRUD
import myproject.iam.Users

class GetChannels extends ApiFunction {
  override val name = "get_channels"
  override val description = "get all channels in the platform"

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    CRUD.getAllChannels map { channels =>
      channels.map(_.serialize)
    }
  }
}
