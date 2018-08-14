package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.IllegalOperationException
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Groups.CRUD._
import myproject.iam.Groups.Group
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class OrganizationSpecs extends DatabaseSpec {

  val channel = Channel(UUID.randomUUID, "organization specs channel", None, None)
  val g1 = Group(UUID.randomUUID, "group 1", None, channel.id, None, None)
  val g2 = Group(UUID.randomUUID, "group 2", None, channel.id, None, None)
  val g3 = Group(UUID.randomUUID, "group 3", None, channel.id, None, None)
  val g4 = Group(UUID.randomUUID, "group 4", None, channel.id, None, None)
  val g5 = Group(UUID.randomUUID, "group 5", None, channel.id, None, None)
  val g6 = Group(UUID.randomUUID, "group 6", None, channel.id, None, None)
  val g7 = Group(UUID.randomUUID, "group 7", None, channel.id, None, None)
  val g8 = Group(UUID.randomUUID, "group 8", None, channel.id, None, None)
  val g9 = Group(UUID.randomUUID, "group 9", None, channel.id, None, None)

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

  it should "create the organization" in {
    attachGroup(g2.id, g1.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 2
    attachGroup(g3.id, g1.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 3
    attachGroup(g4.id, g2.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 4
    attachGroup(g5.id, g2.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 5
    attachGroup(g6.id, g5.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 6
    attachGroup(g7.id, g5.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 7
    attachGroup(g8.id, g3.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 8
    attachGroup(g9.id, g3.id, Authorization.voidIAMAuthzChecker).futureValue.size shouldBe 9
  }

  it should "return the correct children for 1" in {
    val children = getGroupChildren(g1.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 9
    children.filter(t => t._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 0))
    children.filter(t => t._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 1))
    children.filter(t => t._1.id==g3.id).map(t => (t._1.id, t._2)) shouldBe List((g3.id, 1))
    children.filter(t => t._1.id==g4.id).map(t => (t._1.id, t._2)) shouldBe List((g4.id, 2))
    children.filter(t => t._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 2))
    children.filter(t => t._1.id==g8.id).map(t => (t._1.id, t._2)) shouldBe List((g8.id, 2))
    children.filter(t => t._1.id==g9.id).map(t => (t._1.id, t._2)) shouldBe List((g9.id, 2))
    children.filter(t => t._1.id==g6.id).map(t => (t._1.id, t._2)) shouldBe List((g6.id, 3))
    children.filter(t => t._1.id==g7.id).map(t => (t._1.id, t._2)) shouldBe List((g7.id, 3))
  }

  it should "return the correct children for 2" in {
    val children = getGroupChildren(g2.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 5
    children.filter(t => t._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 0))
    children.filter(t => t._1.id==g4.id).map(t => (t._1.id, t._2)) shouldBe List((g4.id, 1))
    children.filter(t => t._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 1))
    children.filter(t => t._1.id==g6.id).map(t => (t._1.id, t._2)) shouldBe List((g6.id, 2))
    children.filter(t => t._1.id==g7.id).map(t => (t._1.id, t._2)) shouldBe List((g7.id, 2))
  }

  it should "return the correct children for 3" in {
    val children = getGroupChildren(g3.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 3
    children.filter(t => t._1.id==g3.id).map(t => (t._1.id, t._2)) shouldBe List((g3.id, 0))
    children.filter(t => t._1.id==g8.id).map(t => (t._1.id, t._2)) shouldBe List((g8.id, 1))
    children.filter(t => t._1.id==g9.id).map(t => (t._1.id, t._2)) shouldBe List((g9.id, 1))
  }

  it should "return the correct children for 4" in {
    val children = getGroupChildren(g4.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 1
    children.filter(t => t._1.id==g4.id).map(t => (t._1.id, t._2)) shouldBe List((g4.id, 0))
  }

  it should "return the correct children for 5" in {
    val children = getGroupChildren(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 3
    children.filter(t => t._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 0))
    children.filter(t => t._1.id==g6.id).map(t => (t._1.id, t._2)) shouldBe List((g6.id, 1))
    children.filter(t => t._1.id==g7.id).map(t => (t._1.id, t._2)) shouldBe List((g7.id, 1))
  }

  it should "return the correct children for 6" in {
    val children = getGroupChildren(g6.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 1
    children.filter(t => t._1.id==g6.id).map(t => (t._1.id, t._2)) shouldBe List((g6.id, 0))
  }

  it should "return the correct children for 7" in {
    val children = getGroupChildren(g7.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 1
    children.filter(t => t._1.id==g7.id).map(t => (t._1.id, t._2)) shouldBe List((g7.id, 0))
  }

  it should "return the correct children for 8" in {
    val children = getGroupChildren(g8.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 1
    children.filter(t => t._1.id==g8.id).map(t => (t._1.id, t._2)) shouldBe List((g8.id, 0))
  }

  it should "return the correct children for 9" in {
    val children = getGroupChildren(g9.id, Authorization.voidIAMAuthzChecker).futureValue
    children.size shouldBe 1
    children.filter(t => t._1.id==g9.id).map(t => (t._1.id, t._2)) shouldBe List((g9.id, 0))
  }

  it should "return the correct parents for 1" in {
    val parents = getGroupParents(g1.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.map(t => (t._1.id, t._2)) shouldBe List((g1.id, 0))
  }

  it should "return the correct parents for 2" in {
    val parents = getGroupParents(g2.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.filter(_._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 0))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 1))
  }

  it should "return the correct parents for 3" in {
    val parents = getGroupParents(g3.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 2
    parents.filter(_._1.id==g3.id).map(t => (t._1.id, t._2)) shouldBe List((g3.id, 0))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 1))
  }

  it should "return the correct parents for 4" in {
    val parents = getGroupParents(g4.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.filter(_._1.id==g4.id).map(t => (t._1.id, t._2)) shouldBe List((g4.id, 0))
    parents.filter(_._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 1))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 2))
  }

  it should "return the correct parents for 5" in {
    val parents = getGroupParents(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.filter(_._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 0))
    parents.filter(_._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 1))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 2))
  }

  it should "return the correct parents for 6" in {
    val parents = getGroupParents(g6.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 4
    parents.filter(_._1.id==g6.id).map(t => (t._1.id, t._2)) shouldBe List((g6.id, 0))
    parents.filter(_._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 1))
    parents.filter(_._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 2))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 3))
  }

  it should "return the correct parents for 7" in {
    val parents = getGroupParents(g7.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 4
    parents.filter(_._1.id==g7.id).map(t => (t._1.id, t._2)) shouldBe List((g7.id, 0))
    parents.filter(_._1.id==g5.id).map(t => (t._1.id, t._2)) shouldBe List((g5.id, 1))
    parents.filter(_._1.id==g2.id).map(t => (t._1.id, t._2)) shouldBe List((g2.id, 2))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 3))
  }

  it should "return the correct parents for 8" in {
    val parents = getGroupParents(g8.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.filter(_._1.id==g8.id).map(t => (t._1.id, t._2)) shouldBe List((g8.id, 0))
    parents.filter(_._1.id==g3.id).map(t => (t._1.id, t._2)) shouldBe List((g3.id, 1))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 2))
  }

  it should "return the correct parents for 9" in {
    val parents = getGroupParents(g9.id, Authorization.voidIAMAuthzChecker).futureValue
    parents.size shouldBe 3
    parents.filter(_._1.id==g9.id).map(t => (t._1.id, t._2)) shouldBe List((g9.id, 0))
    parents.filter(_._1.id==g3.id).map(t => (t._1.id, t._2)) shouldBe List((g3.id, 1))
    parents.filter(_._1.id==g1.id).map(t => (t._1.id, t._2)) shouldBe List((g1.id, 2))
  }

  it should "give the whole organization for a group member" in {
    val organization = getGroupOrganization(g3.id, Authorization.voidIAMAuthzChecker).futureValue
    organization.size shouldBe 9
  }

  it should "not attach an already attached group" in {
    an[IllegalOperationException] shouldBe thrownBy(attachGroup(g2.id, g3.id, Authorization.voidIAMAuthzChecker).futureValue)
  }

  it should "not attach two groups in different channels" in {
    val g10 = Group(UUID.randomUUID, "group 10", None, channel.id, None, None)
    val otherChannel = Channel(UUID.randomUUID, "another organization channel", None, None)
    val g11 = Group(UUID.randomUUID, "group 11", None, otherChannel.id, None, None)
    createChannel(otherChannel, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g10, Authorization.voidIAMAuthzChecker).futureValue
    createGroup(g11, Authorization.voidIAMAuthzChecker).futureValue
    an[ IllegalOperationException] shouldBe thrownBy(attachGroup(g10.id, g11.id, Authorization.voidIAMAuthzChecker).futureValue)
  }

  it should "not attach a group to itself" in {
    val g12 = Group(UUID.randomUUID, "group 12", None, channel.id, None, None)
    createGroup(g12, Authorization.voidIAMAuthzChecker).futureValue
    an[IllegalOperationException] shouldBe thrownBy(attachGroup(g12.id, g12.id, Authorization.voidIAMAuthzChecker).futureValue)
  }

  it should "delete the whole subtree on detach and reset the groups parent id" in {
    detachGroup(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    val children = getGroupChildren(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    val parents = getGroupParents(g5.id, Authorization.voidIAMAuthzChecker).futureValue
    children.map(t => (t._1.id, t._2)) shouldBe List((g5.id, 0))
    parents.map(t => (t._1.id, t._2)) shouldBe List((g5.id, 0))
    val organization = getGroupOrganization(g1.id, Authorization.voidIAMAuthzChecker).futureValue
    organization.size shouldBe 6
    getGroup(g5.id, Authorization.voidIAMAuthzChecker).futureValue.parentId shouldBe None
    getGroup(g6.id, Authorization.voidIAMAuthzChecker).futureValue.parentId shouldBe None
    getGroup(g7.id, Authorization.voidIAMAuthzChecker).futureValue.parentId shouldBe None
  }
}
