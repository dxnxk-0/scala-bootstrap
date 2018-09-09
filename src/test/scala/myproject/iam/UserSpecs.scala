package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common.security.JWT
import myproject.common.{AccessRefusedException, AuthenticationFailedException, ObjectNotFoundException}
import myproject.iam.Authorization.voidIAMAuthzChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Groups.CRUD.{createGroup, _}
import myproject.iam.Groups.GroupStatus
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, UserStatus}
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMTestDataFactory}

@DoNotDiscover
class UserSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)
  val user = IAMTestDataFactory.getGroupUser(group.id, Some(GroupRole.Admin))

  implicit val authz = voidIAMAuthzChecker
  
  it should "create a user" in {
    createChannel(channel)
    createGroup(group)
    createUser(user).futureValue.login shouldBe user.login
  }

  it should "get the created user by id" in {
    getUser(user.id).futureValue.login shouldBe user.login
  }

  it should "not log in the user with incorrect password" in {
    an[AuthenticationFailedException] shouldBe thrownBy(
      loginPassword(user.login, "incorrect", _ => voidIAMAuthzChecker).futureValue
    )
  }

  it should "not log in a non existent user" in {
    an[AuthenticationFailedException] shouldBe thrownBy(loginPassword("non-existent", "Kondor_123", _ => voidIAMAuthzChecker).futureValue)
  }

  it should "should not log in the user if he is not active" in {
    updateUser(user.id, u => u.copy(status = UserStatus.Locked)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(user.login,user.password, _ => Authorization.canLogin(user, _)).futureValue)
    updateUser(user.id, u => u.copy(status = UserStatus.Inactive)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(user.login,user.password, _ => Authorization.canLogin(user, _)).futureValue)
    updateUser(user.id, u => u.copy(status = UserStatus.PendingActivation)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(user.login,user.password, _ => Authorization.canLogin(user, _)).futureValue)
    updateUser(user.id, u => u.copy(status = UserStatus.Active)).futureValue
  }

  it should "should not log in the user if its group is locked or inactive" in {
    updateGroup(group.id, g => g.copy(status = GroupStatus.Inactive)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(user.login,user.password, _ => Authorization.canLogin(user, _)).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Locked)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(user.login,user.password, _ => Authorization.canLogin(user, _)).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Active)).futureValue
  }

  it should "log in the user" in {
    val (logged, token) = loginPassword(user.login,user.password, _ => voidIAMAuthzChecker).futureValue
    logged.id shouldBe user.id
    JWT.extractToken(token).right.get.uid shouldBe user.id
  }

  it should "update the user" in {
    updateUser(user.id, u => u.copy(login = "smith")).futureValue
    getUser(user.id).futureValue.login shouldBe "smith"
  }

  it should "update the password" in {
    updateUser(user.id, u => u.copy(password = "new password"))
    loginPassword(user.login, "new password", _ => voidIAMAuthzChecker)
  }

  it should "delete the user" in {
    deleteUser(user.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(user.id).futureValue)
  }
}
