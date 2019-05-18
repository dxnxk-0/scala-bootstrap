package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.GroupUpdate
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMHelpers, IAMTestDataFactory}

@DoNotDiscover
class GroupSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)

  implicit val authz = VoidIAMAccessChecker
  
  it should "create a group" in {
    IAMHelpers.createChannel(channel).futureValue
    IAMHelpers.createGroup(group).futureValue.name shouldBe group.name
  }

  it should "get a group" in {
    getGroup(group.id).futureValue.name shouldBe group.name
  }

  it should "update a group" in {
    updateGroup(group.id, GroupUpdate(name = Some("Death Star"))).futureValue
    val updated = getGroup(group.id).futureValue
    updated.name shouldBe "Death Star"
    updated.lastUpdate.isDefined shouldBe true
  }

  it should "delete a group" in {
    deleteGroup(group.id).futureValue shouldBe Done
  }
}
