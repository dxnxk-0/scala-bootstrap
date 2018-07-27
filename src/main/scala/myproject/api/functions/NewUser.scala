package myproject.api.functions

import java.util.UUID

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit.AuditData
import myproject.common.TimeManagement._
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization
import myproject.iam.Users.{CRUD, GroupRole, User, UserLevel}

trait NewUserParameters {
  def getCommonParameters(implicit p: ReifiedDataWrapper) = (
    required(p.nonEmptyString("login")),
    required(p.nonEmptyString("password")),
    required(p.email("email"))
  )
}

class NewPlatformUser extends ApiFunction with NewUserParameters {
  override val name = "new_platform_user"
  override val doc = ApiSummaryDoc(
    description = "create a new platform user (platform users are defined at the highest possible level which is the platform) - "
      + "a platform represents the whole application instance - it contains channels, which in turn contain groups",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val user = User(UUID.randomUUID, UserLevel.Platform, login, email, password, None, None, None, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}

class NewChannelUser extends ApiFunction with NewUserParameters {
  override val name = "new_channel_user"
  override val doc = ApiSummaryDoc(
    description = "create a new channel user (channel users are users defined at the channel level (platform>channels>groups>users)) ; "
                + "they generally represents administrators of groups of users, as a channel contains groups",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val channelId = required(p.uuid("channel_id"))
    val user = User(UUID.randomUUID, UserLevel.Channel, login, email, password, Some(channelId), None, None, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}

class NewGroupUser extends ApiFunction with NewUserParameters {
  override val name = "new_group_user"
  override val doc = ApiSummaryDoc(
    description = "create a new group user (group users are users defined at the group level (platform>channels>groups>users)) ; "
      + "they generally represents end users of the application",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val (groupId, groupRole) = (required(p.uuid("group_id")), nullable(p.enumString("group_role", GroupRole)))
    val user = User(UUID.randomUUID, UserLevel.Group, login, email, password, None, Some(groupId), groupRole, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}

class NewSimpleUser extends ApiFunction with NewUserParameters {
  override val name = "new_simple_user"
  override val doc = ApiSummaryDoc(
    description = "create a new simple user (TBD)",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val user = User(UUID.randomUUID, UserLevel.NoLevel, login, email, password, None, None, None, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}
