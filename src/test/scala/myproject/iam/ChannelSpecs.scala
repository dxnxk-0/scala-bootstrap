package myproject.iam

import java.util.UUID

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.common.TimeManagement._
import myproject.iam.Authorization.voidIAMAuthzChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{User, UserLevel}
import org.scalatest.DoNotDiscover
import test.DatabaseSpec
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class ChannelSpecs extends DatabaseSpec {
  val now = getCurrentDateTime
  val channel = Channel(UUID.randomUUID, "TEST", None, None)
  val group = Group(UUID.randomUUID, "ACME", None, channel.id, None, None)
  val smith = User(UUID.randomUUID, UserLevel.Group, "channel-specs", "John", "Smith", EmailAddress("channel-specs@tests.com"), "whatever", None, Some(group.id), None, None, None)

  it should "create a channel" in {
    createChannel(channel, voidIAMAuthzChecker).futureValue.name shouldBe "TEST"
    createGroup(group, voidIAMAuthzChecker).futureValue
    createUser(smith, voidIAMAuthzChecker).futureValue
  }

  it should "get a channel" in {
    getChannel(channel.id, voidIAMAuthzChecker).futureValue.name shouldBe "TEST"
  }

  it should "update a channel" in {
    updateChannel(channel.id, c => c.copy(name = "SPECS"), voidIAMAuthzChecker).futureValue
    getChannel(channel.id, voidIAMAuthzChecker).futureValue.name shouldBe "SPECS"
  }

  it should "delete a channel" in {
    deleteChannel(channel.id, voidIAMAuthzChecker).futureValue shouldBe Done
  }
}
