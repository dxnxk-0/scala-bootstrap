package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common._
import myproject.iam.Authorization.VoidIAMAccessChecker
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMHelpers, IAMTestDataFactory}

@DoNotDiscover
class StructureSpecs extends DatabaseSpec {

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
      _ <- IAMHelpers.createChannel(channel1)
      _ <- IAMHelpers.createChannel(channel2)
      _ <- IAMHelpers.createGroup(groupInChannel2)
    } yield Done

    createFuture.futureValue shouldBe Done
  }

  it should "not allow to create a group with a parent group in another channel" in {
    val myGroupInChannel1 = IAMTestDataFactory.getGroup(channel1.id)
    an [IllegalOperationException] shouldBe thrownBy(IAMHelpers.createGroup(myGroupInChannel1.copy(parentId = Some(groupInChannel2.id))).futureValue)
  }

//TODO: Enhanced group admin capabilities
//  it should "allow a group admin to create a child group" in {
//
//  }
//
//  it should "allow a group admin to move a child within the organization" in {
//
//  }
//
//  it should "not allow a group admin to move a group outside his organization" in {
//
//  }
}
