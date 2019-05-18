package myproject.api.functions

import java.util.UUID

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users._

trait CreateUserParameters {
  def getCommonParameters(implicit p: ReifiedDataWrapper) = {
    val login = required(p.nonEmptyString("login"))
    val password = required(p.nonEmptyString("password"))
    val email = required(p.email("email"))
    val fn = required(p.email("first_name"))
    val ln = required(p.email("last_name"))

    UserUpdate(Some(login.get), Some(email.get), Some(fn.get), Some(ln.get), Some(password.get), Some(UserStatus.Active))
  }
}

class CreatePlatformUser(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction with CreateUserParameters {
  override val name = "create_platform_user"
  override val doc = ApiSummaryDoc(
    description = "create a new platform user (platform users are defined at the highest possible level which is the platform) - "
      + "a platform represents the whole application instance - it contains channels, which in turn contain groups",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User) = {

    implicit val checker = authz(user)
    val update = getCommonParameters
    CRUD.createPlatformUser(UUID.randomUUID, update) map (_.serialize)
  }
}

class CreateChannelUser(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction with CreateUserParameters {
  override val name = "create_channel_user"
  override val doc = ApiSummaryDoc(
    description = "create a new channel user (channel users are users defined at the channel level (platform>channels>groups>users)) ; "
                + "they generally represents administrators of groups of users, as a channel contains groups",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId) {
      val update = getCommonParameters
      CRUD.createChannelUser(UUID.randomUUID, channelId.get, update) map (_.serialize)
    }
  }
}

class CreateGroupUser(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction with CreateUserParameters {
  override val name = "create_group_user"
  override val doc = ApiSummaryDoc(
    description = "create a new group user (group users are users defined at the group level (platform>channels>groups>users)) ; "
      + "they generally represents end users of the application",
    `return` = "an object containing the newly created user's data")

  override def process(implicit p: ReifiedDataWrapper, user: User) = {

    val (groupId, groupRole) = (required(p.uuid("group_id")), nullable(p.enumString("group_role", GroupRole)))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId, groupRole) {
      val update = getCommonParameters.copy(groupRole = Some(groupRole.get))
      CRUD.createGroupUser(UUID.randomUUID, groupId.get, update) map (_.serialize)
    }
  }
}
