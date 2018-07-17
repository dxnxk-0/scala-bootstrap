package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Companies.CRUD._
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.UpdateName
import myproject.iam.Users.CRUD._
import myproject.iam.Users.UserRole
import org.scalatest.DoNotDiscover
import test.DatabaseSpec
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class ChannelSpecs extends DatabaseSpec {

  lazy val channel = createChannel("TEST").futureValue
  lazy val company = createCompany(channel.id, "ACME").futureValue
  lazy val user = createUser("smith", "whatever", company.id, UserRole.User, EmailAddress("no-reply@tests.com"))

  it should "create a channel" in {
    channel.name shouldBe "TEST"
  }

  it should "get a channel" in {
    getChannel(channel.id).futureValue.name shouldBe "TEST"
  }

  it should "update a channel" in {
    updateChannel(channel.id, UpdateName("SPECS")).futureValue
    getChannel(channel.id).futureValue.name shouldBe "SPECS"
  }

  it should "delete a channel" in {
    deleteChannel(channel.id).futureValue shouldBe Done
  }
}
