package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common.security.JWT
import myproject.common.{AuthenticationFailedException, ObjectNotFoundException}
import myproject.iam.Users.CRUD._
import test.DatabaseSpec

class UserSpecs extends DatabaseSpec {

  lazy val company = Companies.CRUD.createCompany("ACME").futureValue
  lazy val jdoe = createUser("jdoe", "Kondor_123", company.id).futureValue

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
    updateUser(jdoe.copy(login = "smith")).futureValue
    getUser(jdoe.id).futureValue.login shouldBe "smith"
  }

  it should "delete the user" in {
    deleteUser(jdoe.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(jdoe.id).futureValue)
  }
}
