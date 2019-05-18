package myproject.iam

import myproject.common.FutureImplicits._
import myproject.common._
import myproject.common.security.JWT
import myproject.iam.Authorization.VoidIAMAccessChecker
import myproject.iam.Channels.CRUD._
import myproject.iam.Groups.CRUD.{createGroup, _}
import myproject.iam.Groups.GroupStatus
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, UserStatus, UserUpdate}
import org.scalatest.DoNotDiscover
import test.{DatabaseSpec, IAMHelpers, IAMTestDataFactory}
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future

@DoNotDiscover
class UserSpecs extends DatabaseSpec {
  val channel = IAMTestDataFactory.getChannel
  val group = IAMTestDataFactory.getGroup(channel.id)
  val groupUser = IAMTestDataFactory.getGroupUser(group.id, Some(GroupRole.Admin))
  val channelUser = IAMTestDataFactory.getChannelUser(channel.id)
  val platformUser = IAMTestDataFactory.getPlatformUser

  implicit val authz = VoidIAMAccessChecker

  it should "create the organization" in {
    createChannel(channel).futureValue
    createGroup(group).futureValue
  }

  it should "not create a user with an invalid login" in {
    a [ValidationErrorException] shouldBe thrownBy(IAMHelpers.createUser(channelUser.copy(login = "")).futureValue)
    a [ValidationErrorException] shouldBe thrownBy(IAMHelpers.createUser(channelUser.copy(login = "foo bar")).futureValue)
    a [ValidationErrorException] shouldBe thrownBy(IAMHelpers.createUser(channelUser.copy(login = "foo\tbar")).futureValue)
    a [ValidationErrorException] shouldBe thrownBy(IAMHelpers.createUser(channelUser.copy(login = "foo\rbar")).futureValue)
    a [ValidationErrorException] shouldBe thrownBy(IAMHelpers.createUser(channelUser.copy(login = "foo\nbar")).futureValue)
  }

  it should "create a user and silently put the email and login in lower case" in {
    IAMHelpers.createUser(
      groupUser.copy(email = EmailAddress(groupUser.email.value.toUpperCase), login = groupUser.login.toUpperCase)
    ).futureValue.login shouldBe groupUser.login
    IAMHelpers.createUser(channelUser).futureValue.id shouldBe channelUser.id
    IAMHelpers.createUser(platformUser).futureValue.id shouldBe platformUser.id
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
    updateUser(groupUser.id, UserUpdate(status = Some(UserStatus.Locked))).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, UserUpdate(status = Some(UserStatus.Inactive))).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, UserUpdate(status = Some(UserStatus.PendingActivation))).futureValue
    an[AccessRefusedException] shouldBe thrownBy(loginPassword(groupUser.login,groupUser.password).futureValue)
    updateUser(groupUser.id, UserUpdate(status = Some(UserStatus.Active))).futureValue
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

  it should "not update the user with an already existing email" in {
    val otherUser = IAMTestDataFactory.getGroupUser(group.id)
    IAMHelpers.createUser(otherUser).futureValue
    a[EmailAlreadyExistsException] shouldBe thrownBy(updateUser(groupUser.id, UserUpdate(email = Some(otherUser.email))).futureValue)
  }

  it should "not update the user with an already existing login" in {
    val otherUser = IAMTestDataFactory.getGroupUser(group.id)
    IAMHelpers.createUser(otherUser).futureValue
    a[LoginAlreadyExistsException] shouldBe thrownBy(updateUser(groupUser.id, UserUpdate(login = Some(otherUser.login))).futureValue)
  }

  it should "not create a user with an already existing email" in {
    val otherUser = IAMTestDataFactory.getGroupUser(group.id)
    a[EmailAlreadyExistsException] shouldBe thrownBy(IAMHelpers.createUser(otherUser.copy(email = groupUser.email)).futureValue)
  }

  it should "not create a user with an already existing login" in {
    val otherUser = IAMTestDataFactory.getGroupUser(group.id)
    a[LoginAlreadyExistsException] shouldBe thrownBy(IAMHelpers.createUser(otherUser.copy(login = groupUser.login)).futureValue)
  }

  it should "update the user" in {
    updateUser(groupUser.id, UserUpdate(login = Some("SMITH"))).futureValue
    val updated = getUser(groupUser.id).futureValue
    updated.login shouldBe "smith"
    updated.lastUpdate.isDefined shouldBe true
  }

  it should "update the password" in {
    updateUser(groupUser.id, UserUpdate(password = Some("new password")))
    loginPassword(groupUser.login, "new password")
  }

  it should "provide a magic link capability" in {
    implicit val notifier = new Notifier {
      override def sendMagicLink(emailAddress: EmailAddress, token: Tokens.Token) = Future.successful(Done)
    }
    val token = sendMagicLink(groupUser.email).futureValue
    loginToken(token.id).futureValue._1.email shouldBe groupUser.email
    a [ObjectNotFoundException] shouldBe thrownBy(loginToken(token.id).futureValue)
  }

  it should "delete the user" in {
    deleteUser(groupUser.id).futureValue
    a [ObjectNotFoundException] shouldBe thrownBy(getUser(groupUser.id).futureValue)
  }
}
