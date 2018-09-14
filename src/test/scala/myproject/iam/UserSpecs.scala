package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common.security.JWT
import myproject.common.{AccessRefusedException, AuthenticationFailedException, ObjectNotFoundException}
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Groups.CRUD.{createGroup, _}
import myproject.iam.Groups.GroupStatus
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, UserStatus}
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMTestDataFactory}
import uk.gov.hmrc.emailaddress.EmailAddress

@DoNotDiscover
class UserSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)
  val groupUser = IAMTestDataFactory.getGroupUser(group.id, Some(GroupRole.Admin))
  val channelUser = IAMTestDataFactory.getChannelUser(channel.id)
  val platformUser = IAMTestDataFactory.getPlatformUser

  implicit val authz = VoidIAMAccessChecker

  it should "create a user and silently put the email and login in lower case" in {
    createChannel(channel)
    createGroup(group)
    createUser(groupUser.copy(email = EmailAddress(groupUser.email.value.toUpperCase), login = groupUser.login.toUpperCase)).futureValue.login shouldBe groupUser.login
    createUser(channelUser).futureValue.id shouldBe channelUser.id
    createUser(platformUser).futureValue.id shouldBe platformUser.id
  }

  it should "get the created user by id" in {
    getUser(groupUser.id).futureValue.login shouldBe groupUser.login
  }

  it should "not log in the user with incorrect password" in {
    an[AuthenticationFailedException] shouldBe thrownBy(
      loginPassword(groupUser.login, "incorrect").futureValue
    )
  }

  it should "not log in a non existent user" in {
    an[AuthenticationFailedException] shouldBe thrownBy(loginPassword("non-existent", "Kondor_123").futureValue)
  }

  it should "should not log in the user if he is not active" in {
    updateUser(groupUser.id, u => u.copy(status = UserStatus.Locked)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, u => u.copy(status = UserStatus.Inactive)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, u => u.copy(status = UserStatus.PendingActivation)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, u => u.copy(status = UserStatus.Active)).futureValue
  }

  it should "should not log in the user if its group is locked or inactive" in {
    updateGroup(group.id, g => g.copy(status = GroupStatus.Inactive)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Locked)).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateGroup(group.id, g => g.copy(status = GroupStatus.Active)).futureValue
  }

  it should "log in the user" in {
    val (logged, token) = loginPassword(groupUser.login, groupUser.password).futureValue
    logged.id shouldBe groupUser.id
    JWT.extractToken(token).right.get.uid shouldBe groupUser.id

    loginPassword(channelUser.login, channelUser.password).futureValue._1.id shouldBe channelUser.id

    loginPassword(platformUser.login, platformUser.password).futureValue._1.id shouldBe platformUser.id
  }

  it should "update the user" in {
    updateUser(groupUser.id, u => u.copy(login = "SMITH")).futureValue
    val updated = getUser(groupUser.id).futureValue
    updated.login shouldBe "smith"
    updated.lastUpdate.isDefined shouldBe true
  }

  it should "update the password" in {
    updateUser(groupUser.id, u => u.copy(password = "new password"))
    loginPassword(groupUser.login, "new password")
  }

  it should "delete the user" in {
    deleteUser(groupUser.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(groupUser.id).futureValue)
  }
}
