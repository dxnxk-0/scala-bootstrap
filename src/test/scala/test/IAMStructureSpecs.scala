package test

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, IllegalOperationException, ValidationErrorException}
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Users.{UserLevel, UserUpdate}
import myproject.iam.{Channels, Groups, Users}
import org.scalatest.DoNotDiscover

@DoNotDiscover
class IAMStructureSpecs extends DatabaseSpec {

  implicit val authz = VoidIAMAccessChecker

  val platformUser = IAMTestDataFactory.getPlatformUser
  val channel1 = IAMTestDataFactory.getChannel
  val channel2 = IAMTestDataFactory.getChannel
  val channelUserInChannel1 =IAMTestDataFactory.getChannelUser(channel1.id)
  val channelUserInChannel2 =IAMTestDataFactory.getChannelUser(channel2.id)
  val groupInChannel1 = IAMTestDataFactory.getGroup(channel1.id)
  val groupUserInGroupChannel1 = IAMTestDataFactory.getGroupUser(groupInChannel1.id)
  val groupInChannel2 = IAMTestDataFactory.getGroup(channel2.id)
  val groupUserInGroupChannel2 = IAMTestDataFactory.getGroupUser(groupInChannel2.id)

  it should "create the multi-tenant database" in {
    val createFuture = for {
      _ <- Users.CRUD.createUser(platformUser)
      _ <- Channels.CRUD.createChannel(channel1)
      _ <- Channels.CRUD.createChannel(channel2)
      _ <- Users.CRUD.createUser(channelUserInChannel1)
      _ <- Users.CRUD.createUser(channelUserInChannel2)
      _ <- Groups.CRUD.createGroup(groupInChannel1)
      _ <- Groups.CRUD.createGroup(groupInChannel2)
      _ <- Users.CRUD.createUser(groupUserInGroupChannel1)
      _ <- Users.CRUD.createUser(groupUserInGroupChannel2)
    } yield Done

    createFuture.futureValue shouldBe Done
  }

  it should "not allow to create a group with a parent group in another channel" in {
    val myGroupInChannel1 = IAMTestDataFactory.getGroup(channel1.id)
    an [IllegalOperationException] shouldBe thrownBy(Groups.CRUD.createGroup(myGroupInChannel1.copy(parentId = Some(groupInChannel2.id))).futureValue)
  }

  it should "not allow to update a group with a parent group in another channel" in {
    an [IllegalOperationException] shouldBe thrownBy(Groups.CRUD.updateGroup(groupInChannel1.id, g => g.copy(parentId = Some(groupInChannel2.id))).futureValue)
  }

  it should "not allow to change a user's level" in {
    an [IllegalOperationException] shouldBe thrownBy(Users.CRUD.updateUser(platformUser.id, u => u.copy(level = UserLevel.Channel)).futureValue)
    an [IllegalOperationException] shouldBe thrownBy(Users.CRUD.updateUser(channelUserInChannel1.id, u => u.copy(level = UserLevel.Group)).futureValue)
    an [IllegalOperationException] shouldBe thrownBy(Users.CRUD.updateUser(groupUserInGroupChannel1.id, u => u.copy(level = UserLevel.Channel)).futureValue)
  }

  it should "not allow to create an inconsistent user" in {
    val invalidUsers = List(
      //IAMTestDataFactory.getGroupUser(groupInChannel1.id).copy(level = UserLevel.Platform),
      IAMTestDataFactory.getGroupUser(groupInChannel1.id).copy(level = UserLevel.Channel),
      IAMTestDataFactory.getGroupUser(groupInChannel1.id).copy(groupId = None),
      IAMTestDataFactory.getPlatformUser.copy(groupId = Some(groupInChannel1.id)),
      IAMTestDataFactory.getPlatformUser.copy(channelId = Some(channel1.id)),
      IAMTestDataFactory.getChannelUser(channel1.id).copy(groupId = Some(groupInChannel1.id))
    )

    invalidUsers foreach { u =>
      a [ValidationErrorException] shouldBe thrownBy(Users.CRUD.createUser(u).futureValue)
    }
  }

  it should "not allow to move a user from his group to another" in {
    val upd: UserUpdate = u => u.copy(groupId = Some(groupInChannel2.id))
    an [IllegalOperationException] shouldBe thrownBy(Users.CRUD.updateUser(groupUserInGroupChannel1.id, upd).futureValue)
  }
}
