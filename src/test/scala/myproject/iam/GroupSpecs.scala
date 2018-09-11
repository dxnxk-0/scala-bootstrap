package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Groups.CRUD._
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMTestDataFactory}

@DoNotDiscover
class GroupSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)

  implicit val authz = VoidIAMAccessChecker
  
  it should "create a group" in {
    createChannel(channel).futureValue
    createGroup(group).futureValue.name shouldBe group.name
  }

  it should "get a group" in {
    getGroup(group.id).futureValue.name shouldBe group.name
  }

  it should "update a group" in {
    updateGroup(group.id, g => g.copy(name = "Death Star")).futureValue
    getGroup(group.id).futureValue.name shouldBe "Death Star"
  }

  it should "delete a group" in {
    deleteGroup(group.id).futureValue shouldBe Done
  }
}
