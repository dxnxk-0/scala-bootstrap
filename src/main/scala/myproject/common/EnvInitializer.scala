package myproject.common

import java.time.LocalDateTime
import java.util.UUID

import myproject.Config
import myproject.common.Runtime.ec
import myproject.common.security.BCrypt
import myproject.database.ApplicationDatabase
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
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
      User(
        id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        UserLevel.Platform,
        login = "root",
        firstName = "",
        lastName = "",
        email = EmailAddress("root@nowhere"),
        password = BCrypt.hashPassword("Kondor_123"),
        created = Some(LocalDateTime.parse("1970-01-01T00:00:00")))

    val channel = Channel(UUID.fromString("14526b8d-050a-4b7b-a70b-903a1eb025cc"), "demo channel", created = Some(TimeManagement.getCurrentDateTime))

    val channelAdmin =
      User(
        id = UUID.randomUUID,
        level = UserLevel.Channel,
        login = "channel-admin",
        firstName = "bob",
        lastName = "the admin",
        email = EmailAddress("admin@channel.com"),
        password = BCrypt.hashPassword("Kondor_123"),
        channelId = Some(channel.id),
        created = Some(TimeManagement.getCurrentDateTime))

    val group =
      Group(UUID.fromString("e1f4579e-fc83-4741-b533-772830f84cf9"), "demo group", channel.id, created = Some(TimeManagement.getCurrentDateTime))

    val groupAdmin =
      User(
        id = UUID.fromString("a3a7cff3-dd24-4ef9-b408-e074dec2b550"),
        level = UserLevel.Group,
        login = "group-admin",
        firstName = "John",
        lastName = "Doe1",
        email = EmailAddress("admin@group.com"),
        password = BCrypt.hashPassword("Kondor_123"),
        groupId = Some(group.id),
        groupRole = Some(GroupRole.Admin),
        created = Some(TimeManagement.getCurrentDateTime))

    val groupUser1 =
      User(
        id = UUID.fromString("db9a9f93-eabd-4923-8a59-da5653f0a626"),
        level = UserLevel.Group,
        login = "group-user-1",
        firstName = "John",
        lastName = "Doe2",
        email = EmailAddress("user-1@group.com"),
        password = BCrypt.hashPassword("Kondor_123"),
        groupId = Some(group.id),
        created = Some(TimeManagement.getCurrentDateTime))

    val groupUser2 =
      User(
        id = UUID.fromString("096f2116-f3b6-4d26-be26-d000d67806b9"),
        level = UserLevel.Group,
        login = "group-user-2",
        firstName = "John",
        lastName = "Doe3",
        email = EmailAddress("user-2@group.com"),
        password = BCrypt.hashPassword("Kondor_123"),
        groupId = Some(group.id),
        created = Some(TimeManagement.getCurrentDateTime))

    implicit val authz = VoidIAMAccessChecker

    val initFuture = for {
      _ <- db.insert(root)
      _ <- db.insert(channel)
      _ <- db.insert(channelAdmin)
      _ <- db.insert(group)
      _ <- db.insert(groupAdmin)
      _ <- db.insert(groupUser1)
      _ <- db.insert(groupUser2)
    } yield Done

    initFuture
  }
}