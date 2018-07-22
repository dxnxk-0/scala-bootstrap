package myproject.iam

import java.util.UUID

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class GroupSpecs extends DatabaseSpec {
  val now = getCurrentDateTime
  val channel = Channel(UUID.randomUUID, "TEST", now, now)
  val group = Group(UUID.randomUUID, "ACME", channel.id, now, now)

  it should "create a group" in {
    createChannel(channel).futureValue
    createGroup(group).futureValue.name shouldBe "ACME"
  }

  it should "get a group" in {
    getGroup(group.id).futureValue.name shouldBe "ACME"
  }

  it should "update a group" in {
    updateGroup(group.id, (g: Group) => g.copy(name = "Death Star")).futureValue
    getGroup(group.id).futureValue.name shouldBe "Death Star"
  }

  it should "delete a group" in {
    deleteGroup(group.id).futureValue shouldBe Done
  }
}
