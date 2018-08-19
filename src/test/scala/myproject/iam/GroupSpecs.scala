package myproject.iam

import java.util.UUID

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.iam.Authorization.voidIAMAuthzChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class GroupSpecs extends DatabaseSpec {
  val now = getCurrentDateTime
  val channel = Channel(UUID.randomUUID, "TEST", None, None)
  val group = Group(UUID.randomUUID, "ACME", channel.id)

  it should "create a group" in {
    createChannel(channel, voidIAMAuthzChecker).futureValue
    createGroup(group, voidIAMAuthzChecker).futureValue.name shouldBe "ACME"
  }

  it should "get a group" in {
    getGroup(group.id, voidIAMAuthzChecker).futureValue.name shouldBe "ACME"
  }

  it should "update a group" in {
    updateGroup(group.id, g => g.copy(name = "Death Star"), voidIAMAuthzChecker).futureValue
    getGroup(group.id, voidIAMAuthzChecker).futureValue.name shouldBe "Death Star"
  }

  it should "delete a group" in {
    deleteGroup(group.id, voidIAMAuthzChecker).futureValue shouldBe Done
  }
}
