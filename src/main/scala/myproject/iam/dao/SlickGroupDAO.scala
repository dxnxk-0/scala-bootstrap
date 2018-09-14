package myproject.iam.dao

import java.sql.JDBCType
import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.SlickDAO
import myproject.iam.Groups.GroupStatus.GroupStatus
import myproject.iam.Groups.{Group, GroupDAO, GroupStatus}
import slick.jdbc.{GetResult, PositionedParameters, SetParameter}

trait SlickGroupDAO extends GroupDAO with SlickDAO { self: SlickChannelDAO with SlickUserDAO =>

  import api._

  implicit val GetUUID: GetResult[UUID] = GetResult[UUID](_.nextObject().asInstanceOf[UUID])

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }

  implicit def groupStatusMapper = MappedColumnType.base[GroupStatus, Int](
    e => e.id,
    i => GroupStatus(i))


  protected class GroupsTable(tag: Tag) extends Table[Group](tag, "GROUPS") {
    def id = column[UUID]("GROUP_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def status = column[GroupStatus]("STATUS")
    def parentId = column[Option[UUID]]("PARENT_ID", O.SqlType("UUID"))
    def channelId = column[UUID]("CHANNEL_ID", O.SqlType("UUID"))
    def created = column[LocalDateTime]("CREATED")
    def lastUpdate = column[Option[LocalDateTime]]("LAST_UPDATE")
    def channel = foreignKey("GROUP_CHANNEL_FK", channelId, channels)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, channelId, status, created.?, lastUpdate, parentId) <> (Group.tupled, Group.unapply)
  }

  protected lazy val groups = TableQuery[GroupsTable]

  override def getGroup(id: UUID) = db.run(groups.filter(_.id===id).result) map (_.headOption)
  override def getGroupF(id: UUID) = getGroup(id).getOrFail(ObjectNotFoundException(s"group with id $id was not found"))
  override def insert(group: Group) = {
    val action = for {
      _ <- groups += group
    } yield group

    db.run(action.transactionally)
  }
  override def update(group: Group) = db.run(groups.filter(_.id===group.id).update(group)) map (_ => group)
  override def deleteGroup(id: UUID) = db.run(groups.filter(_.id===id).delete) map (_ => Done)
  override def getGroupUsers(groupId: UUID) = db.run(users.filter(_.groupId===groupId).result) map (_.toList)

  override def getGroupChildren(groupId: UUID) = { // CTE not supported by Slick
    val cte = sql"""
            |WITH RECURSIVE CTE (GROUP_ID, PARENT_ID) AS (
            |   SELECT GROUP_ID
            |   FROM GROUPS
            |   WHERE PARENT_ID=$groupId
            | UNION ALL
            |   SELECT GROUPS.GROUP_ID
            |   FROM GROUPS
            |   JOIN CTE ON GROUPS.PARENT_ID = CTE.GROUP_ID
            |)
            |SELECT GROUP_ID FROM CTE""".stripMargin.as[UUID]

    val action = for {
      idList   <- cte
      children <- groups.filter(_.id.inSet(idList)).result
    } yield children

    db.run(action).map(_.toList)
  }

  override def getGroupParents(groupId: UUID) = { // CTE not supported by Slick
    val cte = sql"""
           |WITH RECURSIVE CTE (GROUP_ID, PARENT_ID) AS (
           |   SELECT GROUP_ID, PARENT_ID
           |   FROM   GROUPS
           |   WHERE  GROUPS.GROUP_ID=$groupId
           |   UNION ALL
           |   SELECT GROUPS.GROUP_ID, GROUPS.PARENT_ID
           |   FROM   CTE
           |   JOIN   GROUPS ON GROUPS.GROUP_ID = CTE.PARENT_ID
           |   )
           |SELECT GROUP_ID FROM CTE WHERE GROUP_ID<>$groupId""".stripMargin.as[UUID]

    val action = for {
      idList  <- cte
      parents <- groups.filter(_.id.inSet(idList)).result
    } yield parents

    db.run(action).map(_.toList)
  }
}
