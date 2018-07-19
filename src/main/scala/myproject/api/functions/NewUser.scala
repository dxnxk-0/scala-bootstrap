package myproject.api.functions

import java.util.UUID

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.asOpt
import myproject.iam.Serializers._
import myproject.iam.Users
import myproject.iam.Users.{GroupRole, User, UserLevel}
import uk.gov.hmrc.emailaddress.EmailAddress

protected trait NewUserFunction extends ApiFunction {
  case class CommonParams(login: String, password: String, email: EmailAddress)
  def getCommonParams(p: ReifiedDataWrapper): CommonParams = {
    CommonParams(
      p.string("login"),
      p.string("password"),
      p.email("email"))
  }
}

object NewPlatformUser extends NewUserFunction {
  override val name = "new_platform_user"
  override val description = "Create a new platform user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val cp = getCommonParams(p)
    val user = User(UUID.randomUUID, UserLevel.Platform, cp.login, cp.email, cp.password, None, None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

object NewChannelUser extends NewUserFunction {
  override val name = "new_channel_user"
  override val description = "Create a new channel user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val cp = getCommonParams(p)
    val channelId = p.uuid("channel_id")

    val user = User(UUID.randomUUID, UserLevel.Channel, cp.login, cp.email, cp.password, Some(channelId), None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

object NewGroupUser extends NewUserFunction {
  override val name = "new_group_user"
  override val description = "Create a new group user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val cp = getCommonParams(p)
    val groupId = p.uuid("group_id")
    val groupRole = asOpt(p.enumString("group_role", GroupRole))

    val user = User(UUID.randomUUID, UserLevel.Group, cp.login, cp.email, cp.password, None, Some(groupId), groupRole)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

object NewSimpleUser extends NewUserFunction {
  override val name = "new_simple_user"
  override val description = "Create a new simple user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val cp = getCommonParams(p)

    val user = User(UUID.randomUUID, UserLevel.NoLevel, cp.login, cp.email, cp.password, None, None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}
