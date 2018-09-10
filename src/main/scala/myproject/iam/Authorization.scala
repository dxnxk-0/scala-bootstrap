package myproject.iam

import myproject.common.Authorization.{AuthorizationCheck, AuthzData, _}
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.{GroupRole, User, UserLevel}

object Authorization {

  case class IAMAuthzData(
      user: Option[User] = None,
      group: Option[Group] = None,
      channel: Option[Channel] = None,
      parentGroups: List[Group] = Nil,
      childrenGroups: List[Group] = Nil)
    extends AuthzData

  private def isPlatformAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Platform) grant else refuse
  private def isChannelAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Channel && (requester.channelId==data.channel.map(_.id) || requester.channelId==data.group.map(_.channelId))) grant else refuse
  private def isGroupAdmin(implicit requester: User, data: IAMAuthzData) = if(requester.level==UserLevel.Group && requester.groupRole.contains(GroupRole.Admin) && (requester.groupId==data.group.map(_.id)) || requester.groupId==data.user.flatMap(_.groupId)) grant else refuse
  private def isParentGroupAdmin(implicit requester: User, data: IAMAuthzData) = if(data.parentGroups.exists(g => requester.groupId.contains(g.id)) && requester.groupRole.contains(GroupRole.Admin)) grant else refuse
  private def isUserHimself(implicit requester: User, data: IAMAuthzData) = if(data.user.exists(_.id==requester.id)) grant else refuse
  private def isInTheSameGroup(implicit requester: User, data: IAMAuthzData) = if(data.user.exists(_.groupId==requester.groupId)) grant else refuse
  private def belongToTheGroup(implicit requester: User, data: IAMAuthzData) = if(requester.groupId==data.group.map(_.id)) grant else refuse
  private def belongToTheOrganization(implicit requester: User, data: IAMAuthzData) = if((data.parentGroups ++ data.childrenGroups).exists(g => requester.groupId.contains(g.id))) grant else refuse

  type IAMAuthzChecker = IAMAuthzData => AuthorizationCheck

  def voidIAMAuthzChecker = (_: IAMAuthzData) => grant

  def canReadUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin orElse isUserHimself orElse isInTheSameGroup orElse belongToTheOrganization
  def canCreateUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin
  def canUpdateUser(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin orElse isUserHimself
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
  def canReadGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin orElse belongToTheGroup
  def canListGroupUsers(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin
  def canUpdateGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin
  def canAdminGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin
  def canDeleteGroup(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin
  def canGetHierarchy(implicit requester: User, data: IAMAuthzData) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isParentGroupAdmin
}
