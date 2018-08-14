package myproject.api.functions

import java.util.UUID

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.TimeManagement
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.{CRUD, Channel}
import myproject.iam.{Authorization, Users}

class CreateChannel extends ApiFunction {
  override val name = "create_channel"
  override val doc = ApiSummaryDoc(
    description = "create a new channel (a channel is a group of groups) (requires high level privileges such as platform administrator)",
    `return` = "an object containing the newly created channel's data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val now = TimeManagement.getCurrentDateTime
    val channelName = required(p.nonEmptyString("name"))

    checkParamAndProcess(channelName) flatMap { _ =>
      val channel = Channel(UUID.randomUUID, channelName.get, None, None)
      CRUD.createChannel(channel, Authorization.canCreateChannel(user, _)) map (_.toMap)
    }
  }
}
