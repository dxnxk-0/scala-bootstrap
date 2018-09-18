package myproject.iam

import myproject.common.AccessRefusedException
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Users.User
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMTestDataFactory}

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
  val group2Channel2 = IAMTestDataFactory.getGroup(channel2.id)
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
    accessShouldBeRefused(Users.CRUD.createUser(platformUser)(using(user1Channel1), db))
    accessShouldBeRefused(Users.CRUD.createUser(platformUser)(using(user1group1Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(platformUser)(using(platformUser), db))
  }

  it should "allow the right people to create/update the channels" in {
    accessShouldBeRefused(Channels.CRUD.createChannel(channel1)(using(user1Channel1), db))
    accessShouldBeRefused(Channels.CRUD.createChannel(channel1)(using(user1group1Channel1), db))

    accessShouldBeGranted(Channels.CRUD.createChannel(channel1)(using(platformUser), db))
    accessShouldBeGranted(Channels.CRUD.createChannel(channel2)(using(platformUser), db))
  }

  it should "allow the right people to create/update the channel users" in {
    accessShouldBeRefused(Users.CRUD.createUser(user1Channel1)(using(user1Channel2), db))

    accessShouldBeGranted(Users.CRUD.createUser(user1Channel1)(using(user2Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2Channel1)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user1Channel2)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2Channel2)(using(platformUser), db))

    accessShouldBeRefused(Users.CRUD.updateUser(user1Channel1.id, u => u)(using(user1Channel2), db))
    accessShouldBeGranted(Users.CRUD.updateUser(user1Channel1.id, u => u)(using(user2Channel1), db))
  }

  it should "allow the right people to create/update the groups" in {
    accessShouldBeRefused(Groups.CRUD.createGroup(group1Channel1)(using(user1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.createGroup(group1Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(Groups.CRUD.createGroup(group2Channel1)(using(platformUser), db))

    accessShouldBeRefused(Groups.CRUD.createGroup(group1Channel2)(using(user1Channel1), db))
    accessShouldBeGranted(Groups.CRUD.createGroup(group1Channel2)(using(user1Channel2), db))
    accessShouldBeGranted(Groups.CRUD.createGroup(group2Channel2)(using(platformUser), db))
  }

  it should "allow the right people to create/update the group users" in {
    accessShouldBeRefused(Users.CRUD.createUser(adminGroup1Channel1)(using(user1Channel2), db))
    accessShouldBeGranted(Users.CRUD.createUser(adminGroup1Channel1)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user1group1Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2group1Channel1)(using(adminGroup1Channel1), db))

    accessShouldBeRefused(Users.CRUD.createUser(adminGroup2Channel1)(using(adminGroup1Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(adminGroup2Channel1)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user1group2Channel1)(using(user1Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2group2Channel1)(using(adminGroup2Channel1), db))

    accessShouldBeRefused(Users.CRUD.createUser(adminGroup2Channel2)(using(adminGroup1Channel1), db))
    accessShouldBeRefused(Users.CRUD.createUser(user1group1Channel2)(using(user2Channel1), db))
    accessShouldBeGranted(Users.CRUD.createUser(adminGroup1Channel2)(using(user1Channel2), db))
    accessShouldBeGranted(Users.CRUD.createUser(user1group1Channel2)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2group1Channel2)(using(user1Channel2), db))

    accessShouldBeGranted(Users.CRUD.createUser(adminGroup2Channel2)(using(user2Channel2), db))
    accessShouldBeGranted(Users.CRUD.createUser(user1group2Channel2)(using(platformUser), db))
    accessShouldBeGranted(Users.CRUD.createUser(user2group2Channel2)(using(adminGroup2Channel2), db))
  }
}
