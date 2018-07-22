package myproject.iam

import myproject.common.Authorization.{AuthorizationCheck, AuthzData, _}
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.{GroupRole, User, UserLevel}

object Authorization {

  case class IAMAuthzData(user: Option[User] = None, group: Option[Group] = None, channel: Option[Channel] = None)
    extends AuthzData

  private def isPlatformAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Platform) grant else refuse
  private def isChannelAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Channel && (requester.channelId==data.channel.map(_.id) || requester.channelId==data.group.map(_.channelId))) grant else refuse
  private def isGroupAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Group && requester.groupRole.contains(GroupRole.Admin) && (requester.groupId==data.group.map(_.id)) || requester.groupId==data.user.flatMap(_.groupId)) grant else refuse
  private def isUserHimself(implicit requester: User, data: IAMAuthzData) = if(data.user.exists(_.id==requester.id)) grant else refuse
  private def isInTheSameGroup(implicit requester: User, data: IAMAuthzData) = if(requester.groupId==requester.groupId) grant else refuse
  private def belongToTheGroup(implicit requester: User, data: IAMAuthzData) = if(requester.groupId==data.group.map(_.id)) grant else refuse
  private def belongToTheChannel(implicit requester: User, data: IAMAuthzData) = if(requester.groupId==data.group.map(_.id)) grant else refuse

  type IAMAuthzChecker = IAMAuthzData => AuthorizationCheck

  def voidIAMAuthzChecker = (_: IAMAuthzData) => grant
  def canLogin(implicit requester: User, data: IAMAuthzData) = grant
  def canReadUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isUserHimself orElse isInTheSameGroup
  def canCreateUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin
  def canUpdateUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isUserHimself
  def canAdminUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canDeleteUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin

  def canCreateChannel(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin
  def canListChannels(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin
  def canReadChannel(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canListChannelGroups(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canUpdateChannel(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canAdminChannel(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canDeleteChannel(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin

  def canCreateGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canReadGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse belongToTheGroup
  def canListGroupUsers(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin
  def canUpdateGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin
  def canAdminGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canDeleteGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin
}
