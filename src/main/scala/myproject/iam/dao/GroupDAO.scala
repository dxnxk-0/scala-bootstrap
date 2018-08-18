package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.{Done, IllegalOperationException}
import myproject.database.DAO
import myproject.iam.Groups.Group

trait GroupDAO extends DAO { self: ChannelDAO with UserDAO =>

  import api._

  protected class GroupsTable(tag: Tag) extends Table[Group](tag, "GROUPS") {
    def id = column[UUID]("GROUP_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def parentId = column[Option[UUID]]("PARENT_ID", O.SqlType("UUID"))
    def channelId = column[UUID]("CHANNEL_ID", O.SqlType("UUID"))
    def created = column[LocalDateTime]("CREATED")
    def lastUpdate = column[Option[LocalDateTime]]("LAST_UPDATE")
    def channel = foreignKey("GROUP_CHANNEL_FK", channelId, channels)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, parentId, channelId, created.?, lastUpdate).mapTo[Group]
  }

  protected class OrganizationTable(tag: Tag) extends Table[(UUID, UUID, Int)](tag, "ORGANIZATION_TREES") {
    def ancestorId = column[UUID]("ANCESTOR_ID", O.SqlType("UUID"))
    def descendantId = column[UUID]("DESCENDANT_ID", O.SqlType("UUID"))
    def depth = column[Int]("DEPTH")
    def ancestor = foreignKey("ORGANIZATION_ANCESTOR_FK", ancestorId, groups)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def descendant = foreignKey("ORGANIZATION_DESCENDANT_FK", descendantId, groups)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (ancestorId, descendantId, depth)
  }

  protected lazy val groups = TableQuery[GroupsTable]
  protected lazy val organizations = TableQuery[OrganizationTable]

  def getGroup(id: UUID) = db.run(groups.filter(_.id===id).result) map (_.headOption)
  def insert(group: Group) = {
    val action = for {
      _ <- groups += group
      _ <- organizations += ((group.id, group.id, 0))
    } yield group

    db.run(action.transactionally)
  }
  def update(group: Group) = db.run(groups.filter(_.id===group.id).update(group)) map (_ => group)
  def deleteGroup(id: UUID) = db.run(groups.filter(_.id===id).delete) map (_ => Done)
  def getGroupUsers(groupId: UUID) = db.run(users.filter(_.groupId===groupId).result)

  def attachGroup(groupId: UUID, parentId: UUID) = {
    val groupAlreadyAttached = organizations.filter(p => p.descendantId===groupId || p.ancestorId===groupId)
    val selectSubQuery = organizations.filter(_.descendantId===parentId).map(r => (r.ancestorId, groupId, r.depth+1))
    val insertInto = organizations.forceInsertQuery(selectSubQuery)
    val action = for {
      _ <- groupAlreadyAttached.size.result.flatMap {
        case 1 => DBIO.successful(1)
        case _ => DBIO.failed(IllegalOperationException(s"group with group id $groupId is already attached to an organization"))
      }
      _ <- insertInto
      _ <- groups.filter(_.id===groupId).map(_.parentId).update(Some(parentId))
    } yield Unit

    db.run(action.transactionally) //TODO: Concurrency mgmt
      .flatMap(_ => getGroupOrganization(groupId))
  }

  def detachGroup(groupId: UUID) = {
    val inClause = organizations.filter(_.ancestorId===groupId).map(_.descendantId)
    val select = organizations.filter(_.descendantId in inClause)
    val children = groups.filter(_.id in inClause).map(_.parentId)

    val action = for {
      _ <- children.update(None)
      _ <- groups.filter(_.id===groupId).map(_.parentId).update(None)
      _ <- select.filter(t => !(t.depth===0)).delete
    } yield Done

    db.run(action.transactionally) //TODO: Concurrency mgmt
  }

  def getGroupOrganization(groupId: UUID) = {
    getGroupParents(groupId).map(_.maxBy(_._2)) flatMap { root =>
      getGroupChildren(root._1.id)
    }
  }

  def getGroupChildren(groupId: UUID) = {
    val groupIdAndDepthQuery = organizations.filter(_.ancestorId===groupId).map(p => (p.descendantId, p.depth))
    val childrenQuery = groups.filter(_.id in groupIdAndDepthQuery.map(_._1))

    val action = for {
      idAndDepth <- groupIdAndDepthQuery.result
      children <- childrenQuery.result
    } yield {
      children.map(p => (p, idAndDepth.find(_._1==p.id).map(_._2).get))
    }

    db.run(action)
  }

  def getGroupParents(groupId: UUID) = {
    val groupIdAndDepthQuery = organizations.filter(_.descendantId===groupId).map(p => (p.ancestorId, p.depth))
    val parentQuery = groups.filter(_.id in groupIdAndDepthQuery.map(_._1))

    val action = for {
      idAndDepth <- groupIdAndDepthQuery.result
      parents <- parentQuery.result
    } yield {
      parents.map(p => (p, idAndDepth.find(_._1==p.id).map(_._2).get))
    }

    db.run(action)
  }
}
