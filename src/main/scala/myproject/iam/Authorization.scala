package myproject.iam

import myproject.common.Authorization._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.User

object Authorization {

  trait IAMAccessChecker extends AccessChecker {
    implicit val requester: Option[User]

    def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User): AuthorizationCheck
    def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck

    def canOperateChannelUser(implicit channel: Channel, target: User): AuthorizationCheck
    def canOperatePlatformUser(implicit target: User): AuthorizationCheck

    def canCreateChannel(implicit target: Channel): AuthorizationCheck
    def canListChannels: AuthorizationCheck
    def canReadChannel(implicit target: Channel): AuthorizationCheck
    def canListChannelGroups(implicit target: Channel): AuthorizationCheck
    def canUpdateChannel(implicit target: Channel): AuthorizationCheck
    def canAdminChannel(implicit target: Channel): AuthorizationCheck
    def canDeleteChannel(implicit target: Channel): AuthorizationCheck

    def canCreateGroup(implicit channel: Channel, target: Group): AuthorizationCheck
    def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canAdminGroup(implicit channel: Channel, target: Group) : AuthorizationCheck
    def canDeleteGroup(implicit channel: Channel, target: Group): AuthorizationCheck
  }
  
  class DefaultIAMAccessChecker(requestor: Option[User]) extends IAMAccessChecker {

    implicit val requester = requestor

    def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User) = isPlatformAdmin orElse isChannelAdmin orElse isInTheSameGroup orElse belongToOneGroup(group :: children++parents)
    def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isAdminOfOneGroup(parents)
    def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isUserHimself orElse isAdminOfOneGroup(parents)
    def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin orElse isChannelAdmin
    def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin

    def canOperateChannelUser(implicit channel: Channel, target: User) = isPlatformAdmin orElse isChannelAdmin
    def canOperatePlatformUser(implicit target: User) = isPlatformAdmin
    
    def canCreateChannel(implicit target: Channel) = isPlatformAdmin
    def canListChannels = isPlatformAdmin
    def canReadChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canListChannelGroups(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canUpdateChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canAdminChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canDeleteChannel(implicit target: Channel) = isPlatformAdmin

    def canCreateGroup(implicit channel: Channel, target: Group) = isPlatformAdmin orElse isChannelAdmin
    def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse belongToTheGroup
    def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isAdminOfOneGroup(parents)
    def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isAdminOfOneGroup(parents)
    def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin orElse isChannelAdmin orElse isGroupAdmin orElse isAdminOfOneGroup(parents)
    def canAdminGroup(implicit channel: Channel, target: Group) = isPlatformAdmin orElse isChannelAdmin
    def canDeleteGroup(implicit channel: Channel, target: Group) = isPlatformAdmin
  }

  object VoidIAMAccessChecker extends IAMAccessChecker {
    implicit val requester = None

    def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User) = grant
    def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant

    def canOperateChannelUser(implicit channel: Channel, target: User) = grant
    def canOperatePlatformUser(implicit target: User) = grant

    def canCreateChannel(implicit target: Channel) = grant
    def canListChannels = grant
    def canReadChannel(implicit target: Channel) = grant
    def canListChannelGroups(implicit target: Channel) = grant
    def canUpdateChannel(implicit target: Channel) = grant
    def canAdminChannel(implicit target: Channel) = grant
    def canDeleteChannel(implicit target: Channel) = grant

    def canCreateGroup(implicit channel: Channel, target: Group) = grant
    def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    def canAdminGroup(implicit channel: Channel, target: Group)  = grant
    def canDeleteGroup(implicit channel: Channel, target: Group) = grant
  }
}
