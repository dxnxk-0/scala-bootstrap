package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.security.JWT
import myproject.common.{AccessRefusedException, AuthenticationFailedException, ObjectNotFoundException}
import myproject.iam.Authorization.voidIAMAuthzChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD.{createGroup, _}
import myproject.iam.Groups.{Group, GroupStatus}
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{User, UserLevel, UserStatus}
import org.scalatest.DoNotDiscover
import test.DatabaseSpec
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class UserSpecs extends DatabaseSpec {
  val now = getCurrentDateTime
  val channel = Channel(UUID.randomUUID, "TEST")
  val group = Group(UUID.randomUUID, "ACME", channel.id)
  val jdoe = User(UUID.randomUUID, UserLevel.Group, "user-specs", "John", "Doe", EmailAddress("user-specs@tests.com"), "Kondor_123", groupId = Some(group.id), status = UserStatus.Active)

  it should "create a user" in {
    createChannel(channel, voidIAMAuthzChecker)
    createGroup(group, voidIAMAuthzChecker)
    createUser(jdoe, voidIAMAuthzChecker).futureValue.login shouldBe jdoe.login
  }

  it should "get the created user by id" in {
    getUser(jdoe.id, voidIAMAuthzChecker).futureValue.login shouldBe jdoe.login
  }

  it should "not log in the user with incorrect password" in {
    an[AuthenticationFailedException] shouldBe thrownBy(
      loginPassword(jdoe.login, "incorrect", _ => voidIAMAuthzChecker).futureValue
    )
  }

  it should "not log in a non existent user" in {
    an[AuthenticationFailedException] shouldBe thrownBy(loginPassword("non-existent", "Kondor_123", _ => voidIAMAuthzChecker).futureValue)
  }

  it should "should not log in the user if he is not active" in {
    updateUser(jdoe.id, u => u.copy(status = UserStatus.Locked), voidIAMAuthzChecker).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(jdoe.login,jdoe.password, _ => Authorization.canLogin(jdoe, _)).futureValue)
    updateUser(jdoe.id, u => u.copy(status = UserStatus.Inactive), voidIAMAuthzChecker).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(jdoe.login,jdoe.password, _ => Authorization.canLogin(jdoe, _)).futureValue)
    updateUser(jdoe.id, u => u.copy(status = UserStatus.PendingActivation), voidIAMAuthzChecker).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(jdoe.login,jdoe.password, _ => Authorization.canLogin(jdoe, _)).futureValue)
    updateUser(jdoe.id, u => u.copy(status = UserStatus.Active), voidIAMAuthzChecker).futureValue
  }

  it should "should not log in the user if its group is locked or inactive" in {
    updateGroup(group.id, g => g.copy(status = GroupStatus.Inactive), voidIAMAuthzChecker).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(jdoe.login,jdoe.password, _ => Authorization.canLogin(jdoe, _)).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Locked), voidIAMAuthzChecker).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(jdoe.login,jdoe.password, _ => Authorization.canLogin(jdoe, _)).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Active), voidIAMAuthzChecker).futureValue
  }

  it should "log in the user" in {
    val (user, token) = loginPassword(jdoe.login,jdoe.password, _ => voidIAMAuthzChecker).futureValue
    user.id shouldBe jdoe.id
    JWT.extractToken(token).right.get.uid shouldBe jdoe.id
  }

  it should "update the user" in {
    updateUser(jdoe.id, u => u.copy(login = "smith"), voidIAMAuthzChecker).futureValue
    getUser(jdoe.id, voidIAMAuthzChecker).futureValue.login shouldBe "smith"
  }

  it should "update the password" in {
    updateUser(jdoe.id, u => u.copy(password = "new password"), voidIAMAuthzChecker)
    loginPassword(jdoe.login, "new password", _ => voidIAMAuthzChecker)
  }

  it should "delete the user" in {
    deleteUser(jdoe.id, voidIAMAuthzChecker).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(jdoe.id, voidIAMAuthzChecker).futureValue)
  }
}
