package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.TokenExpiredException
import myproject.iam.Tokens.TokenRole
import test.DatabaseSpec

import scala.concurrent.duration._

class TokenSpecs extends DatabaseSpec {

  lazy val expiredToken = Tokens.createToken(UUID.randomUUID(), TokenRole.Authentication, Some(0.second)).futureValue
  lazy val validToken = Tokens.createToken(UUID.randomUUID(), TokenRole.Signup, Some(10.minutes)).futureValue

  it should "create a token" in {
    expiredToken.role shouldBe TokenRole.Authentication
    validToken.role shouldBe TokenRole.Signup
  }

  it should "not retrieve the expired token" in {
    a [TokenExpiredException] should be thrownBy Tokens.getToken(expiredToken.id).futureValue
  }

  it should "retrieve the valid token" in {
    Tokens.getToken(validToken.id).futureValue.role shouldBe TokenRole.Signup
  }
}
