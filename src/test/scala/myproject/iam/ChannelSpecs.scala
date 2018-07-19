package myproject.iam

import java.util.UUID

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Groups.CRUD._
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{User, UserLevel}
import org.scalatest.DoNotDiscover
import test.DatabaseSpec
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class ChannelSpecs extends DatabaseSpec {

  val channel = Channel(UUID.randomUUID, "TEST")
  val group = Group(UUID.randomUUID, "ACME", channel.id)
  val smith = User(UUID.randomUUID, UserLevel.Group, "channel-specs", EmailAddress("channel-specs@tests.com"), "whatever", None, Some(group.id), None)

  it should "create a channel" in {
    createChannel(channel).futureValue.name shouldBe "TEST"
    createGroup(group)
    createUser(smith).futureValue
  }

  it should "get a channel" in {
    getChannel(channel.id).futureValue.name shouldBe "TEST"
  }

  it should "update a channel" in {
    updateChannel(channel.copy(name = "SPECS")).futureValue
    getChannel(channel.id).futureValue.name shouldBe "SPECS"
  }

  it should "delete a channel" in {
    deleteChannel(channel.id).futureValue shouldBe Done
  }
}
