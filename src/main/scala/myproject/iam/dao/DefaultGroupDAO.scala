package myproject.iam.dao

import java.sql.JDBCType
import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Runtime.ec
import myproject.database.DAO.DBIOImplicits._
import myproject.database.{DAO, SlickProfile}
import myproject.iam.Groups.GroupStatus.GroupStatus
import myproject.iam.Groups.{Group, GroupDAO, GroupStatus}
import slick.jdbc.{GetResult, PositionedParameters, SetParameter}

trait DefaultGroupDAO extends GroupDAO with DAO { self: SlickProfile with DefaultChannelDAO with DefaultUserDAO =>

  import slickProfile.api._

  implicit val GetUUID: GetResult[UUID] = GetResult[UUID](_.nextObject().asInstanceOf[UUID])

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }

  implicit def groupStatusMapper = MappedColumnType.base[GroupStatus, Int](
    e => e.id,
    i => GroupStatus(i))


  protected class GroupsTable(tag: Tag) extends Table[Group](tag, "groups") {
    def id = column[UUID]("group_id", O.PrimaryKey, O.SqlType("uuid"))
    def name = column[String]("name")
    def status = column[GroupStatus]("status")
    def parentId = column[Option[UUID]]("parent_id", O.SqlType("uuid"))
    def channelId = column[UUID]("channel_id", O.SqlType("uuid"))
    def created = column[LocalDateTime]("created")
    def lastUpdate = column[Option[LocalDateTime]]("last_update")
    def channel = foreignKey("group_channel_fk", channelId, channels)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, channelId, status, created.?, lastUpdate, parentId) <> (Group.tupled, Group.unapply)
  }

  protected lazy val groups = TableQuery[GroupsTable]

  override def getGroup(id: UUID) = groups.filter(_.id===id).result.headOption
  override def insert(group: Group) = {
    val action = for {
      c <- groups += group
    } yield c

    action.transactionally.doneSingleUpdate
  }
  override def update(group: Group) = groups.filter(_.id===group.id).update(group).doneSingleUpdate
  override def deleteGroup(id: UUID) = groups.filter(_.id===id).delete.doneSingleUpdate

  override def getGroupChildren(groupId: UUID) = { // CTE not supported by Slick
    val cte = sql"""
            |WITH RECURSIVE "cte" ("group_id", "parent_id") AS (
            |   SELECT "group_id", "parent_id"
            |   FROM "groups"
            |   WHERE "parent_id"=$groupId
            |   UNION ALL
            |   SELECT "groups"."group_id", "groups"."parent_id"
            |   FROM "groups"
            |   JOIN "cte" ON "groups"."parent_id" = "cte"."group_id"
            |)
            |SELECT "group_id" FROM "cte"""".stripMargin.as[UUID]

    for {
      idList   <- cte
      children <- groups.filter(_.id.inSet(idList)).result
    } yield children
  }

  override def getGroupParents(groupId: UUID) = { // CTE not supported by Slick
    val cte = sql"""
           |WITH RECURSIVE "cte" ("group_id", "parent_id") AS (
           |   SELECT "group_id", "parent_id"
           |   FROM   "groups"
           |   WHERE  "groups"."group_id"=$groupId
           |   UNION ALL
           |   SELECT "groups"."group_id", "groups"."parent_id"
           |   FROM   "cte"
           |   JOIN   "groups" ON "groups"."group_id" = "cte"."parent_id"
           |   )
           |SELECT "group_id" FROM "cte" WHERE "group_id"<>$groupId""".stripMargin.as[UUID]

    for {
      idList  <- cte
      parents <- groups.filter(_.id.inSet(idList)).result
    } yield parents
  }
}
