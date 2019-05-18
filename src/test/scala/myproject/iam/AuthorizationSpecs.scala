package myproject.iam

import myproject.common.AccessRefusedException
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Groups.{GroupStatus, GroupUpdate}
import myproject.iam.Users.{User, UserStatus, UserUpdate}
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMHelpers, IAMTestDataFactory}

import scala.concurrent.Future

@DoNotDiscover
class AuthorizationSpecs extends DatabaseSpec {
  val platformUser = IAMTestDataFactory.getPlatformUser

  // Channel 1
  ////////////
  val channel1 = IAMTestDataFactory.getChannel

  // Channel Users
  val user1Channel1 = IAMTestDataFactory.getChannelUser(channel1.id)
  val user2Channel1 = IAMTestDataFactory.getChannelUser(channel1.id)

  // Group 1
  val group1Channel1 = IAMTestDataFactory.getGroup(channel1.id)
  val user1group1Channel1 = IAMTestDataFactory.getGroupUser(group1Channel1.id)
  val user2group1Channel1 = IAMTestDataFactory.getGroupUser(group1Channel1.id)
  val adminGroup1Channel1 = IAMTestDataFactory.getGroupAdmin(group1Channel1.id)

  // Group 2
  val group2Channel1 = IAMTestDataFactory.getGroup(channel1.id)
  val user1group2Channel1 = IAMTestDataFactory.getGroupUser(group2Channel1.id)
  val user2group2Channel1 = IAMTestDataFactory.getGroupUser(group2Channel1.id)
  val adminGroup2Channel1 = IAMTestDataFactory.getGroupAdmin(group2Channel1.id)

  // Channel 2
  ////////////
  val channel2 = IAMTestDataFactory.getChannel

  // Channel Users
  val user1Channel2 = IAMTestDataFactory.getChannelUser(channel2.id)
  val user2Channel2 = IAMTestDataFactory.getChannelUser(channel2.id)

  // Group 1
  val group1Channel2 = IAMTestDataFactory.getGroup(channel2.id)
  val user1group1Channel2 = IAMTestDataFactory.getGroupUser(group1Channel2.id)
  val user2group1Channel2 = IAMTestDataFactory.getGroupUser(group1Channel2.id)
  val adminGroup1Channel2 = IAMTestDataFactory.getGroupAdmin(group1Channel2.id)

  // Group 2
  val group2Channel2 = IAMTestDataFactory.getGroup(channel2.id, Some(group1Channel2.id))
  val user1group2Channel2 = IAMTestDataFactory.getGroupUser(group2Channel2.id)
  val user2group2Channel2 = IAMTestDataFactory.getGroupUser(group2Channel2.id)
  val adminGroup2Channel2 = IAMTestDataFactory.getGroupAdmin(group2Channel2.id)

  private def using(u: User) = new DefaultIAMAccessChecker(u)
  private def accessShouldBeRefused[A](f: => Future[A]) = {
    a [AccessRefusedException] shouldBe thrownBy(f.futureValue)
  }
  private def accessShouldBeGranted[A](f: => Future[A]) = {
    noException shouldBe thrownBy(f.futureValue)
  }

  it should "allow the right people to create/update platform users" in {
    accessShouldBeRefused(IAMHelpers.createUser(platformUser)(using(user1Channel1), db))
    accessShouldBeRefused(IAMHelpers.createUser(platformUser)(using(user1group1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(platformUser)(using(platformUser), db))
  }

  it should "allow the right people to create/update the channels" in {
    accessShouldBeRefused(IAMHelpers.createChannel(channel1)(using(user1Channel1), db))
    accessShouldBeRefused(IAMHelpers.createChannel(channel1)(using(user1group1Channel1), db))

    accessShouldBeGranted(IAMHelpers.createChannel(channel1)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createChannel(channel2)(using(platformUser), db))
  }

  it should "allow the right people to create/update the channel users" in {
    accessShouldBeRefused(IAMHelpers.createUser(user1Channel1)(using(user1Channel2), db))

    accessShouldBeGranted(IAMHelpers.createUser(user1Channel1)(using(user2Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2Channel1)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user1Channel2)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2Channel2)(using(platformUser), db))

    accessShouldBeRefused(Users.CRUD.updateUser(user1Channel1.id, UserUpdate())(using(user1Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1Channel1.id, UserUpdate())(using(user2Channel1), db))
  }

  it should "allow the right people to create/update the groups" in {
    accessShouldBeRefused(IAMHelpers.createGroup(group1Channel1)(using(user1Channel2), db))
    accessShouldBeGranted(IAMHelpers.createGroup(group1Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createGroup(group2Channel1)(using(platformUser), db))

    accessShouldBeRefused(IAMHelpers.createGroup(group1Channel2)(using(user1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createGroup(group1Channel2)(using(user1Channel2), db))
    accessShouldBeGranted(IAMHelpers.createGroup(group2Channel2)(using(platformUser), db))

    accessShouldBeGranted(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate())(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate())(using(adminGroup2Channel2), db))
    accessShouldBeGranted(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate())(using(platformUser), db))
    accessShouldBeGranted(Groups.CRUD.updateGroup(group1Channel2.id, GroupUpdate())(using(user1Channel2), db))

    accessShouldBeRefused(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate(status = Some(GroupStatus.Inactive)))(using(adminGroup1Channel2), db))
    accessShouldBeRefused(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate(status = Some(GroupStatus.Inactive)))(using(adminGroup2Channel2), db))
    accessShouldBeGranted(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate(status = Some(GroupStatus.Inactive)))(using(user1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.updateGroup(group2Channel2.id, GroupUpdate(status = Some(GroupStatus.Inactive)))(using(platformUser), db))
  }

  it should "allow the right people to create/update the group users" in {
    accessShouldBeRefused(IAMHelpers.createUser(adminGroup1Channel1)(using(user1Channel2), db))
    accessShouldBeGranted(IAMHelpers.createUser(adminGroup1Channel1)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user1group1Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2group1Channel1)(using(adminGroup1Channel1), db))

    accessShouldBeRefused(IAMHelpers.createUser(adminGroup2Channel1)(using(adminGroup1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(adminGroup2Channel1)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user1group2Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2group2Channel1)(using(adminGroup2Channel1), db))

    accessShouldBeRefused(IAMHelpers.createUser(adminGroup2Channel2)(using(adminGroup1Channel1), db))
    accessShouldBeRefused(IAMHelpers.createUser(user1group1Channel2)(using(user2Channel1), db))
    accessShouldBeGranted(IAMHelpers.createUser(adminGroup1Channel2)(using(user1Channel2), db))
    accessShouldBeGranted(IAMHelpers.createUser(user1group1Channel2)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2group1Channel2)(using(user1Channel2), db))

    accessShouldBeGranted(IAMHelpers.createUser(adminGroup2Channel2)(using(adminGroup1Channel2), db))
    accessShouldBeGranted(IAMHelpers.createUser(user1group2Channel2)(using(platformUser), db))
    accessShouldBeGranted(IAMHelpers.createUser(user2group2Channel2)(using(adminGroup2Channel2), db))

    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate())(using(user1group2Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate())(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate())(using(adminGroup2Channel2), db))
    accessShouldBeRefused(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate(status = Some(UserStatus.Locked)))(using(user1group2Channel1), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate(status = Some(UserStatus.Locked)))(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate(status = Some(UserStatus.Inactive)))(using(adminGroup2Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate(status = Some(UserStatus.PendingActivation)))(using(user1Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1group2Channel2.id, UserUpdate(status = Some(UserStatus.Active)))(using(platformUser), db))
  }

  it should "allow the right people to read objects" in {
    accessShouldBeRefused(Channels.CRUD.getAllChannels(using(user1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getAllChannels(using(user1group1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getAllChannels(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Channels.CRUD.getAllChannels(using(platformUser), db))

    accessShouldBeRefused(Channels.CRUD.getChannel(channel2.id)(using(user1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannelGroups(channel2.id)(using(user1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannel(channel2.id)(using(user1group1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannelGroups(channel2.id)(using(user1group1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannel(channel2.id)(using(adminGroup1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannelGroups(channel2.id)(using(adminGroup1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.getChannel(channel2.id)(using(user1group1Channel2), db))
    accessShouldBeRefused(Channels.CRUD.getChannelGroups(channel2.id)(using(user1group1Channel2), db))
    accessShouldBeRefused(Channels.CRUD.getChannel(channel2.id)(using(adminGroup1Channel2), db))
    accessShouldBeRefused(Channels.CRUD.getChannelGroups(channel2.id)(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Channels.CRUD.getChannel(channel2.id)(using(user1Channel2), db))
    accessShouldBeGranted(Channels.CRUD.getChannelGroups(channel2.id)(using(user1Channel2), db))

    accessShouldBeRefused(Groups.CRUD.getGroup(group2Channel2.id)(using(user1Channel1), db))
    accessShouldBeRefused(Groups.CRUD.getGroup(group2Channel2.id)(using(user1group1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.getGroup(group2Channel2.id)(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.getGroup(group1Channel2.id)(using(user2group1Channel2), db))

    accessShouldBeRefused(Users.CRUD.getUser(user1group2Channel2.id)(using(user1Channel1), db))
    accessShouldBeGranted(Users.CRUD.getUser(user1group2Channel2.id)(using(user1group1Channel2), db))
    accessShouldBeGranted(Users.CRUD.getUser(user1group2Channel2.id)(using(user1group2Channel2), db))
    accessShouldBeGranted(Users.CRUD.getUser(user1group2Channel2.id)(using(adminGroup1Channel2), db))
  }

  it should "allow the right people to delete" in {
    accessShouldBeRefused(Users.CRUD.deleteUser(user1group2Channel2.id)(using(user1Channel1), db))
    accessShouldBeRefused(Users.CRUD.deleteUser(user1group2Channel2.id)(using(user1group2Channel2), db))
    accessShouldBeRefused(Users.CRUD.deleteUser(user1group2Channel2.id)(using(user2group2Channel2), db))
    accessShouldBeGranted(Users.CRUD.deleteUser(user1group2Channel2.id)(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Users.CRUD.deleteUser(user2group2Channel2.id)(using(adminGroup2Channel2), db))

    accessShouldBeRefused(Groups.CRUD.deleteGroup(group2Channel2.id)(using(adminGroup2Channel2), db))
    accessShouldBeRefused(Groups.CRUD.deleteGroup(group2Channel2.id)(using(adminGroup1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.deleteGroup(group2Channel2.id)(using(user1Channel2), db))

    accessShouldBeRefused(Channels.CRUD.deleteChannel(channel1.id)(using(user1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.deleteChannel(channel1.id)(using(user1Channel2), db))
    accessShouldBeGranted(Channels.CRUD.deleteChannel(channel1.id)(using(platformUser), db))
  }
}
