package test

import java.util.UUID

import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.{GroupRole, User, UserLevel}
import test.RandomFactory.randomString
import uk.gov.hmrc.emailaddress.EmailAddress

object IAMTestDataFactory {

  private def randomEmail = EmailAddress(randomString() + "@" + randomString() + ".com")

  def getChannel = Channel(UUID.randomUUID, randomString())
  def getGroup(channelId: UUID, parentId: Option[UUID] = None) = Group(UUID.randomUUID, randomString(), channelId, parentId = parentId)
  def getPlatformUser = User(UUID.randomUUID, UserLevel.Platform, randomString(), randomString(), randomString(), randomEmail, randomString())
  def getChannelUser(channelId: UUID) = User(UUID.randomUUID, UserLevel.Channel, randomString(), randomString(), randomString(), randomEmail, randomString(), channelId = Some(channelId))
  def getGroupUser(groupId: UUID, role: Option[GroupRole] = None) = User(UUID.randomUUID, UserLevel.Group, randomString(), randomString(), randomString(), randomEmail, randomString(), groupId = Some(groupId), groupRole = role)
  def getGroupAdmin(groupId: UUID) = getGroupUser(groupId, Some(GroupRole.Admin))
}
