package myproject.web.server

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.database.DB
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, User, UserLevel}
import uk.gov.hmrc.emailaddress.EmailAddress

object EnvInit {

  def initEnv() = {
    val root = User(UUID.fromString("00000000-0000-0000-0000-000000000000"), UserLevel.Platform, "root", "", "", EmailAddress("root@nowhere"), "Kondor_123")
    val channel = Channel(UUID.fromString("14526b8d-050a-4b7b-a70b-903a1eb025cc"), "demo channel")
    val channelAdmin = User(UUID.randomUUID, UserLevel.Channel, "channel-admin", "bob", "the admin", EmailAddress("admin@channel.com"), "Kondor_123", Some(channel.id))
    val group = Group(UUID.fromString("e1f4579e-fc83-4741-b533-772830f84cf9"), "demo group", channel.id)
    val groupAdmin = User(UUID.fromString("a3a7cff3-dd24-4ef9-b408-e074dec2b550"), UserLevel.Group, "group-admin", "John", "Doe1", EmailAddress("admin@group.com"), "Kondor_123", groupId = Some(group.id), groupRole = Some(GroupRole.Admin))
    val groupUser1 = User(UUID.fromString("db9a9f93-eabd-4923-8a59-da5653f0a626"), UserLevel.Group, "group-user-1", "John", "Doe2", EmailAddress("user-1@group.com"), "Kondor_123", None, Some(group.id))
    val groupUser2 = User(UUID.fromString("096f2116-f3b6-4d26-be26-d000d67806b9"), UserLevel.Group, "group-user-2", "John", "Doe3", EmailAddress("user-2@group.com"), "Kondor_123", None, Some(group.id))

    implicit val authz = VoidIAMAccessChecker

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
