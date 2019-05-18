package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.ChannelUpdate
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMHelpers, IAMTestDataFactory}

@DoNotDiscover
class ChannelSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)
  val user = IAMTestDataFactory.getGroupAdmin(group.id)

  implicit val authz = VoidIAMAccessChecker

  it should "create a channel" in {
    IAMHelpers.createChannel(channel).futureValue.name shouldBe channel.name
    IAMHelpers.createGroup(group).futureValue
    IAMHelpers.createUser(user).futureValue
  }

  it should "get a channel" in {
    getChannel(channel.id).futureValue.name shouldBe channel.name
  }

  it should "update a channel" in {
    updateChannel(channel.id, ChannelUpdate(name = Some("SPECS"))).futureValue
    val updated = getChannel(channel.id).futureValue
    updated.name shouldBe "SPECS"
    updated.lastUpdate.isDefined shouldBe true
  }

  it should "delete a channel" in {
    deleteChannel(channel.id).futureValue shouldBe Done
  }
}
