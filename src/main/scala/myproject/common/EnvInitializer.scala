package myproject.common

import java.util.UUID

import myproject.Config
import myproject.common.Runtime.ec
import myproject.database.ApplicationDatabase
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD.createChannel
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD.createGroup
import myproject.iam.Groups.Group
import myproject.iam.Users.CRUD.createUser
import myproject.iam.Users._
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future

trait DataInitializer {
  def initialize(implicit db: ApplicationDatabase): Future[Done]
}

object DataInitializer {
  lazy val Instance = Class.forName(Config.datainit.clazz).newInstance.asInstanceOf[DataInitializer]
}

class DefaultDataInitializer extends DataInitializer {

  def initialize(implicit db: ApplicationDatabase) = {
    val root =
      User(UUID.fromString("00000000-0000-0000-0000-000000000000"), UserLevel.Platform, "root", "", "", EmailAddress("root@nowhere"), "Kondor_123")

    val channel = Channel(UUID.fromString("14526b8d-050a-4b7b-a70b-903a1eb025cc"), "demo channel")

    val channelAdmin =
      User(UUID.randomUUID, UserLevel.Channel, "channel-admin", "bob", "the admin", EmailAddress("admin@channel.com"), "Kondor_123", Some(channel.id))

    val group =
      Group(UUID.fromString("e1f4579e-fc83-4741-b533-772830f84cf9"), "demo group", channel.id)

    val groupAdmin =
      User(
        id = UUID.fromString("a3a7cff3-dd24-4ef9-b408-e074dec2b550"),
        level = UserLevel.Group,
        login = "group-admin",
        firstName = "John",
        lastName = "Doe1",
        email = EmailAddress("admin@group.com"),
        password = "Kondor_123",
        groupId = Some(group.id),
        groupRole = Some(GroupRole.Admin))

    val groupUser1 =
      User(
        id = UUID.fromString("db9a9f93-eabd-4923-8a59-da5653f0a626"),
        level = UserLevel.Group,
        login = "group-user-1",
        firstName = "John",
        lastName = "Doe2",
        email = EmailAddress("user-1@group.com"),
        password = "Kondor_123",
        groupId = Some(group.id))

    val groupUser2 =
      User(
        id = UUID.fromString("096f2116-f3b6-4d26-be26-d000d67806b9"),
        level = UserLevel.Group,
        login = "group-user-2",
        firstName = "John",
        lastName = "Doe3",
        email = EmailAddress("user-2@group.com"),
        password = "Kondor_123",
        groupId = Some(group.id))

    implicit val authz = VoidIAMAccessChecker

    val initFuture = for {
      _ <- createUser(root)
      _ <- createChannel(channel)
      _ <- createUser(channelAdmin)
      _ <- createGroup(group)
      _ <- createUser(groupAdmin)
      _ <- createUser(groupUser1)
      _ <- createUser(groupUser2)
    } yield Done

    initFuture
  }
}