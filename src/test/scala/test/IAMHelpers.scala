package test

import myproject.database.{DatabaseInterface, SlickProfile}
import myproject.iam.Channels.{Channel, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Groups.{Group, GroupAccessChecker, GroupDAO}
import myproject.iam.Users.{User, UserAccessChecker, UserDAO, UserLevel}
import myproject.iam.{Channels, Groups, Users}

object IAMHelpers {

  def createChannel(channel: Channel)(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface with SlickProfile) = {
    Channels.CRUD.createChannel(channel.id, Channels.Pure.toChannelUpdate(channel))
  }

  def createGroup(group: Group)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface) = {
    Groups.CRUD.createGroup(group.id, group.channelId, group.parentId, Groups.Pure.toGroupUpdate(group))
  }

  def createUser(user: User)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface) = user.level match {
    case UserLevel.Platform => Users.CRUD.createPlatformUser(user.id, Users.Pure.toUserUpdate(user))
    case UserLevel.Channel => Users.CRUD.createChannelUser(user.id, user.channelId.get, Users.Pure.toUserUpdate(user))
    case UserLevel.Group => Users.CRUD.createGroupUser(user.id, user.groupId.get, Users.Pure.toUserUpdate(user))
  }
}
