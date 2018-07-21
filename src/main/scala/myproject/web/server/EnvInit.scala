package myproject.web.server

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.database.DB
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, User, UserLevel}
import uk.gov.hmrc.emailaddress.EmailAddress

object EnvInit {

  def initEnv() = {
    val now = getCurrentDateTime
    val root = User(UUID.fromString("00000000-0000-0000-0000-000000000000"), UserLevel.Platform, "root", EmailAddress("root@nowhere"), "Kondor_123", None, None, None, now, now)
    val channel = Channel(UUID.randomUUID, "demo channel", now, now)
    val channelAdmin = User(UUID.randomUUID, UserLevel.Channel, "channel-admin", EmailAddress("admin@channel.com"), "Kondor_123", Some(channel.id), None, None, now, now)
    val group = Group(UUID.randomUUID, "demo group", channel.id, now, now)
    val groupAdmin = User(UUID.randomUUID, UserLevel.Group, "group-admin", EmailAddress("admin@group.com"), "Kondor_123", None, Some(group.id), Some(GroupRole.Admin), now, now)
    val groupUser1 = User(UUID.randomUUID, UserLevel.Group, "group-user-1", EmailAddress("user-1@group.com"), "Kondor_123", None, Some(group.id), None, now, now)
    val groupUser2 = User(UUID.randomUUID, UserLevel.Group, "group-user-2", EmailAddress("user-2@group.com"), "Kondor_123", None, Some(group.id), None, now, now)

    val initFuture = for {
      _ <- DB.reset
      _ <- createUser(root)
      _ <- createChannel(channel)
      _ <- createUser(channelAdmin)
      _ <- createGroup(group)
      _ <- createUser(groupAdmin)
      _ <- createUser(groupUser1)
      _ <- createUser(groupUser2)
    } yield Unit

    initFuture.futureValue
  }
}
