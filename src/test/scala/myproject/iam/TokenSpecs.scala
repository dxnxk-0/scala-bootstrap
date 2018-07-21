package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.{ObjectNotFoundException, TimeManagement, TokenExpiredException}
import myproject.iam.Channels.CRUD.createChannel
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD.createGroup
import myproject.iam.Groups.Group
import myproject.iam.Tokens.CRUD._
import myproject.iam.Tokens.{Token, TokenRole}
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{User, UserLevel}
import org.scalatest.DoNotDiscover
import test.DatabaseSpec
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class TokenSpecs extends DatabaseSpec {
  val now = getCurrentDateTime
  val channel = Channel(UUID.randomUUID, "TESTS", now, now)
  val group = Group(UUID.randomUUID, "ACME", channel.id, now, now)
  val user = User(UUID.randomUUID, UserLevel.Group, "tokens-specs", EmailAddress("token-specs@tests.com"), "secret", None, Some(group.id), None, now, now)
  val expiredToken = Token(UUID.randomUUID, user.id, TokenRole.Authentication, now, Some(TimeManagement.getCurrentDateTime.plusSeconds(0)))
  val validToken = Token(UUID.randomUUID, user.id, TokenRole.Signup, now, Some(TimeManagement.getCurrentDateTime.plusMinutes(10)))

  it should "create a token" in {
    createChannel(channel)
    createGroup(group)
    createUser(user).futureValue
    createToken(expiredToken).futureValue.role shouldBe TokenRole.Authentication
    createToken(validToken).futureValue.role shouldBe TokenRole.Signup
  }

  it should "not retrieve the expired token" in {
    a [TokenExpiredException] should be thrownBy getToken(expiredToken.id).futureValue
  }

  it should "retrieve the valid token" in {
    getToken(validToken.id).futureValue.role shouldBe TokenRole.Signup
  }

  it should "delete the token" in {
    deleteToken(validToken.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getToken(validToken.id).futureValue)
  }
}
