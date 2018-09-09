package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.voidIAMAuthzChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Groups.CRUD._
import myproject.iam.Users.CRUD._
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMTestDataFactory}

@DoNotDiscover
class ChannelSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)
  val user = IAMTestDataFactory.getGroupAdmin(group.id)

  implicit val authz = voidIAMAuthzChecker

  it should "create a channel" in {
    createChannel(channel).futureValue.name shouldBe channel.name
    createGroup(group).futureValue
    createUser(user).futureValue
  }

  it should "get a channel" in {
    getChannel(channel.id).futureValue.name shouldBe channel.name
  }

  it should "update a channel" in {
    updateChannel(channel.id, c => c.copy(name = "SPECS")).futureValue
    getChannel(channel.id).futureValue.name shouldBe "SPECS"
  }

  it should "delete a channel" in {
    deleteChannel(channel.id).futureValue shouldBe Done
  }
}
