package myproject.api.functions

import java.util.UUID

import myproject.api.ApiFunction
import myproject.api.Serializers._
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
  override val description = "Create a new platform user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val user = User(UUID.randomUUID, UserLevel.Platform, login, email, password, None, None, None, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}

class NewChannelUser extends ApiFunction with NewUserParameters {
  override val name = "new_channel_user"
  override val description = "Create a new channel user"

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
  override val description = "Create a new group user"

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
  override val description = "Create a new simple user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val now = getCurrentDateTime
    val (login, password, email) = getCommonParameters
    val user = User(UUID.randomUUID, UserLevel.NoLevel, login, email, password, None, None, None, now, now)
    CRUD.createUser(user, Authorization.canCreateUser(user, _)) map (_.toMap)
  }
}
