package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.{IllegalOperationException, ValidationErrorException}
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class OrganizationSpecs extends DatabaseSpec {

  val channel = Channel(UUID.randomUUID, "organization specs channel", None, None)
  val g1 = Group(UUID.randomUUID, "group 1", channel.id)
  val g2 = Group(UUID.randomUUID, "group 2", channel.id, parentId = Some(g1.id))
  val g3 = Group(UUID.randomUUID, "group 3", channel.id, parentId = Some(g1.id))
  val g4 = Group(UUID.randomUUID, "group 4", channel.id, parentId = Some(g2.id))
  val g5 = Group(UUID.randomUUID, "group 5", channel.id, parentId = Some(g2.id))
  val g6 = Group(UUID.randomUUID, "group 6", channel.id, parentId = Some(g5.id))
  val g7 = Group(UUID.randomUUID, "group 7", channel.id, parentId = Some(g5.id))
  val g8 = Group(UUID.randomUUID, "group 8", channel.id, parentId = Some(g3.id))
  val g9 = Group(UUID.randomUUID, "group 9", channel.id, parentId = Some(g3.id))

  it should "create all groups" in {
    createChannel(channel, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g1, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g2, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g3, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g4, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g5, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g6, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g7, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g8, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g9, Authorization.voidIAMAuthzChecker).futureValue
  }

  it should "return the correct children for 1" in {
    val children = getGroupChildren(g1.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 8
    children.find(_.id==g2.id).map(_.id) shouldBe Some(g2.id)
    children.find(_.id==g3.id).map(_.id) shouldBe Some(g3.id)
    children.find(_.id==g4.id).map(_.id) shouldBe Some(g4.id)
    children.find(_.id==g5.id).map(_.id) shouldBe Some(g5.id)
    children.find(_.id==g8.id).map(_.id) shouldBe Some(g8.id)
    children.find(_.id==g9.id).map(_.id) shouldBe Some(g9.id)
    children.find(_.id==g6.id).map(_.id) shouldBe Some(g6.id)
    children.find(_.id==g7.id).map(_.id) shouldBe Some(g7.id)
  }

  it should "return the correct children for 2" in {
    val children = getGroupChildren(g2.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 4
    children.find(_.id==g4.id).map(_.id) shouldBe Some(g4.id)
    children.find(_.id==g5.id).map(_.id) shouldBe Some(g5.id)
    children.find(_.id==g6.id).map(_.id) shouldBe Some(g6.id)
    children.find(_.id==g7.id).map(_.id) shouldBe Some(g7.id)
  }

  it should "return the correct children for 3" in {
    val children = getGroupChildren(g3.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 2
    children.find(_.id==g8.id).map(_.id) shouldBe Some(g8.id)
    children.find(_.id==g9.id).map(_.id) shouldBe Some(g9.id)
  }

  it should "return the correct children for 4" in {
    val children = getGroupChildren(g4.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 0
  }

  it should "return the correct children for 5" in {
    val children = getGroupChildren(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 2
    children.find(_.id==g6.id).map(_.id) shouldBe Some(g6.id)
    children.find(_.id==g7.id).map(_.id) shouldBe Some(g7.id)
  }

  it should "return the correct children for 6" in {
    val children = getGroupChildren(g6.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 0
  }

  it should "return the correct children for 7" in {
    val children = getGroupChildren(g7.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 0
  }

  it should "return the correct children for 8" in {
    val children = getGroupChildren(g8.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 0
  }

  it should "return the correct children for 9" in {
    val children = getGroupChildren(g9.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 0
  }

  it should "return the correct parents for 1" in {
    val parents = getGroupParents(g1.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 0
  }

  it should "return the correct parents for 2" in {
    val parents = getGroupParents(g2.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 1
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 3" in {
    val parents = getGroupParents(g3.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 1
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 4" in {
    val parents = getGroupParents(g4.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.find(_.id==g2.id).map(_.id) shouldBe Some(g2.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 5" in {
    val parents = getGroupParents(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.find(_.id==g2.id).map(_.id) shouldBe Some(g2.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 6" in {
    val parents = getGroupParents(g6.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.find(_.id==g5.id).map(_.id) shouldBe Some(g5.id)
    parents.find(_.id==g2.id).map(_.id) shouldBe Some(g2.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 7" in {
    val parents = getGroupParents(g7.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.find(_.id==g5.id).map(_.id) shouldBe Some(g5.id)
    parents.find(_.id==g2.id).map(_.id) shouldBe Some(g2.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 8" in {
    val parents = getGroupParents(g8.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.find(_.id==g3.id).map(_.id) shouldBe Some(g3.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "return the correct parents for 9" in {
    val parents = getGroupParents(g9.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.find(_.id==g3.id).map(_.id) shouldBe Some(g3.id)
    parents.find(_.id==g1.id).map(_.id) shouldBe Some(g1.id)
  }

  it should "not attach two groups in different channels" in {
    val g10 = Group(UUID.randomUUID, "group 10", channel.id)
    val otherChannel = Channel(UUID.randomUUID, "another organization channel")
    val g11 = Group(UUID.randomUUID, "group 11", otherChannel.id, parentId = Some(g10.id))
    createChannel(otherChannel, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g10, Authorization.voidIAMAuthzChecker).futureValue
    an[ IllegalOperationException] shouldBe thrownBy(createGroup(g11, Authorization.voidIAMAuthzChecker).futureValue)
  }

  it should "not attach a group to itself" in {
    val id = UUID.randomUUID
    val g12 = Group(id, "group 12", channel.id, parentId = Some(id))
    a[ValidationErrorException] shouldBe thrownBy(createGroup(g12, Authorization.voidIAMAuthzChecker).futureValue)
  }
}
