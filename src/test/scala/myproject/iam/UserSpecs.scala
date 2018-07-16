package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common.security.JWT
import myproject.common.{AuthenticationFailedException, ObjectNotFoundException}
import myproject.iam.Domains.CRUD.createDomain
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{UpdateLogin, UserRole}
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class UserSpecs extends DatabaseSpec {

  lazy val domain = createDomain("TESTS").futureValue
  lazy val company = Companies.CRUD.createCompany(domain.id, "ACME").futureValue
  lazy val jdoe = createUser("jdoe", "Kondor_123", company.id, UserRole.User).futureValue

  it should "create a user" in {
    jdoe.login shouldBe "jdoe"
  }

  it should "get the created user by id" in {
    getUser(jdoe.id).futureValue.login shouldBe "jdoe"
  }

  it should "not log in the user with incorrect password" in {
    a [AuthenticationFailedException] shouldBe thrownBy(
      loginPassword(jdoe.login, "incorrect").futureValue
    )
  }

  it should "not log in a non existent user" in {
    a [ObjectNotFoundException] shouldBe thrownBy(
      loginPassword("non-existent", "Kondor_123").futureValue
    )
  }

  it should "log in the user" in {
    val (user, token) = loginPassword(jdoe.login,"Kondor_123").futureValue
    user.id shouldBe jdoe.id
    JWT.extractToken(token).right.get.uid shouldBe jdoe.id
  }

  it should "update the user" in {
    updateUser(jdoe.id, UpdateLogin("smith")).futureValue
    getUser(jdoe.id).futureValue.login shouldBe "smith"
  }

  it should "delete the user" in {
    deleteUser(jdoe.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(jdoe.id).futureValue)
  }
}
