package test

import myproject.database.DatabaseInterface
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Users
import myproject.iam.Users._

object IAMHelpers {

  def createUser(user: User)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface) = user.level match {
    case UserLevel.Platform => Users.CRUD.createPlatformUser(user.id, Pure.toUserUpdate(user))
    case UserLevel.Channel => Users.CRUD.createChannelUser(user.id, user.channelId.get, Pure.toUserUpdate(user))
    case UserLevel.Group => Users.CRUD.createGroupUser(user.id, user.groupId.get, Pure.toUserUpdate(user))
  }
}
