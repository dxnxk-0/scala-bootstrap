package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.{Done, IllegalOperationException}
import myproject.database.DAO
import myproject.iam.Groups.Group
import slick.jdbc.TransactionIsolation

trait GroupDAO extends DAO { self: ChannelDAO with UserDAO =>

  import api._

  protected class GroupsTable(tag: Tag) extends Table[Group](tag, "GROUPS") {
    def id = column[UUID]("GROUP_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def channelId = column[UUID]("CHANNEL_ID", O.SqlType("UUID"))
    def created = column[Option[LocalDateTime]]("CREATED")
    def lastUpdate = column[Option[LocalDateTime]]("LAST_UPDATE")
    def channel = foreignKey("GROUP_CHANNEL_FK", channelId, channels)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, channelId, created, lastUpdate) <> (Group.tupled, Group.unapply)
  }

  protected class OrganizationTable(tag: Tag) extends Table[(UUID, UUID, Int)](tag, "ORGANIZATION_TREES") {
    def ancestorId = column[UUID]("ANCESTOR_ID", O.SqlType("UUID"))
    def descendantId = column[UUID]("DESCENDANT_ID", O.SqlType("UUID"))
    def depth = column[Int]("DEPTH")
    def ancestor = foreignKey("ORGANIZATION_ANCESTOR_FK", ancestorId, groups)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def descendant = foreignKey("ORGANIZATION_DESCENDANT_FK", descendantId, groups)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (ancestorId, descendantId, depth)
  }

  protected val groups = TableQuery[GroupsTable]
  protected val organizations = TableQuery[OrganizationTable]

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
      parents <- organizations.filter(_.descendantId===groupId).map(p => (p.ancestorId, p.depth)).result
    } yield parents

    db.run(action.withTransactionIsolation(TransactionIsolation.Serializable).transactionally)
  }

  def detachGroup(groupId: UUID) = {
    val inClause = organizations.filter(_.ancestorId===groupId).map(_.descendantId)
    val select = organizations.filter(_.descendantId in inClause)
    val insert = organizations += ((groupId, groupId, 0))
    val action = for {
      _ <- select.delete
      _ <- insert
    } yield Done

    db.run(action.withTransactionIsolation(TransactionIsolation.Serializable).transactionally)
  }

  def getGroupChildren(groupId: UUID) = db.run(organizations.filter(_.ancestorId===groupId).map(p => (p.descendantId, p.depth)).result)
  def getGroupParents(groupId: UUID) = db.run(organizations.filter(_.descendantId===groupId).map(p => (p.ancestorId, p.depth)).result)
}
