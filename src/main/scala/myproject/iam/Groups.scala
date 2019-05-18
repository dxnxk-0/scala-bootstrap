package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Authorization._
import myproject.common.OptionImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, IllegalOperationException, InvalidParametersException, TimeManagement}
import myproject.database.DatabaseInterface
import myproject.iam.Channels.{Channel, ChannelDAO}
import myproject.iam.Groups.GroupStatus.GroupStatus
import slick.dbio.DBIO

import scala.concurrent.Future
import scala.language.reflectiveCalls

object Groups {

  object GroupStatus extends Enumeration {
    type GroupStatus = Value
    val Active = Value("active")
    val Locked = Value("locked")
    val Inactive = Value("inactive")
  }

  case class GroupUpdate(
    name: Option[String] = None,
    status: Option[GroupStatus] = None)

  case class Group(
      id: UUID,
      name: String,
      channelId: UUID,
      status: GroupStatus = GroupStatus.Active,
      created: Option[LocalDateTime] = None,
      lastUpdate: Option[LocalDateTime] = None,
      parentId: Option[UUID] = None) { if(parentId.contains(id)) throw InvalidParametersException(s"parent group cannot be the group itself", Nil) }

  trait GroupAccessChecker extends AccessChecker {
    def canCreateGroup(implicit channel: Channel, target: Group): AuthorizationCheck
    def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canAdminGroup(implicit channel: Channel, target: Group) : AuthorizationCheck
    def canDeleteGroup(implicit channel: Channel, target: Group): AuthorizationCheck
  }

  trait VoidGroupAccessChecker extends GroupAccessChecker {
    override val requester = None
    override def canCreateGroup(implicit channel: Channel, target: Group) = grant
    override def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canAdminGroup(implicit channel: Channel, target: Group)  = grant
    override def canDeleteGroup(implicit channel: Channel, target: Group) = grant
  }

  trait DefaultGroupAccessChecker extends GroupAccessChecker {

    override def canCreateGroup(implicit channel: Channel, target: Group) = {
      isPlatformAdmin or isChannelAdmin
    }

    override def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = {
      isPlatformAdmin or isChannelAdmin or isGroupAdmin or belongToTheGroup or isAdminOfOneGroup(parents)
    }

    override def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = {
      isPlatformAdmin or isChannelAdmin or isAdminOfOneGroup(target :: parents)
    }

    override def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = {
      isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    }

    override def canAdminGroup(implicit channel: Channel, target: Group) = {
      isPlatformAdmin or isChannelAdmin
    }

    override def canDeleteGroup(implicit channel: Channel, target: Group) = {
      isPlatformAdmin or isChannelAdmin
    }
  }

  trait GroupDAO {
    def getGroup(id: UUID): DBIO[Option[Group]]
    def insert(group: Group): DBIO[Done]
    def update(group: Group): DBIO[Done]
    def deleteGroup(id: UUID): DBIO[Done]
    def getGroupChildren(groupId: UUID): DBIO[Seq[Group]]
    def getGroupParents(groupId: UUID): DBIO[Seq[Group]]
  }

  object Pure {

    def createGroup(groupId: UUID, channelId: UUID, parentId: Option[UUID], update: GroupUpdate) = {
      def missingParam(p: String) = throw InvalidParametersException(s"$p is mandatory", Nil)

      Group(
        id = groupId,
        name = update.name.getOrElse(missingParam("group name")),
        channelId = channelId,
        status = update.status.getOrElse(missingParam("group status")),
        created = Some(TimeManagement.getCurrentDateTime),
        parentId = parentId)
    }

    def updateGroup(group: Group, update: GroupUpdate) = {
      group.copy(
        name = update.name.getOrElse(group.name),
        status = update.status.getOrElse(group.status),
        lastUpdate = Some(TimeManagement.getCurrentDateTime)
      )
    }

    def toGroupUpdate(group: Group) = GroupUpdate(name = Some(group.name), status = Some(group.status))
  }

  object CRUD {

    private def checkParentGroup(group: Group)(implicit db: GroupDAO with DatabaseInterface) = {
      group.parentId match {
        case None => DBIO.successful(Done)
        case Some(id) => db.getGroup(id).map(_.getOrNotFound(id)) map { parent =>
          if(parent.channelId==group.channelId) Done
          else throw IllegalOperationException(s"parent group has to be in the same channel")
        }
      }
    }

    def createGroup(id: UUID, channelId: UUID, parentId: Option[UUID], update: GroupUpdate)
      (implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Group] = {

      val action = {
        db.getChannel(channelId).map(_.getOrNotFound(channelId)) flatMap { channel =>

          val group = Pure.createGroup(id, channelId, parentId, update)

          authz.canCreateGroup(channel, group) ifGranted {
            for {
              _ <- checkParentGroup(group)
              _ <- db.insert(group)
            } yield group
          }
        }
      }

      db.run(action)
    }

    def getGroup(id: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Group] = {
      val action = {
        db.getGroup(id).map(_.getOrNotFound(id)) flatMap { group =>
          group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(DBIO.successful(Nil)) flatMap { parents =>
            db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) map { channel =>
              authz.canReadGroup(channel, group, parents.toList).ifGranted(group)
            }
          }
        }
      }

      db.run(action)
    }

    def updateGroup(id: UUID, update: GroupUpdate)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Group] = {

      def processAuthz(existing: Group, target: Group, channel: Channel, parents: List[Group]) ={
        if(update.status.exists(s => s!=existing.status)) authz.canAdminGroup(channel, target)
        else authz.canUpdateGroup(channel, existing, parents)
      }

      val action = {
        val dependencies = for {
          existing <- db.getGroup(id).map(_.getOrNotFound(id))
          channel  <- db.getChannel(existing.channelId).map(_.getOrNotFound(existing.channelId))
          parents  <- existing.parentId.map(_ => db.getGroupParents(existing.id)).getOrElse(DBIO.successful(Nil))
        } yield (existing, channel, parents)

        dependencies flatMap { case (existing, channel, parents) =>
          val updated = Pure.updateGroup(existing, update)

          processAuthz(existing, updated, channel, parents.toList) ifGranted {
            for {
              _ <- checkParentGroup(updated)
              _ <- db.update(updated)
            } yield updated
          }
        }
      }

      db.run(action)
    }

    def deleteGroup(id: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Done] = {
      val action = {
        db.getGroup(id).map(_.getOrNotFound(id)) flatMap { group =>
          db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) flatMap { channel =>
            authz.canDeleteGroup(channel, group).ifGranted(db.deleteGroup(id))
          }
        }
      }

      db.run(action)
    }

    def getGroupChildren(groupId: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Seq[Group]] = {
      val action = {
        db.getGroup(groupId).map(_.getOrNotFound(groupId)) flatMap { group =>
          db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) flatMap { channel =>
            group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(DBIO.successful(Nil)) flatMap { parents =>
              authz.canGetGroupHierarchy(channel, group, parents.toList).ifGranted(db.getGroupChildren(groupId))
            }
          }
        }
      }

      db.run(action)
    }

    def getGroupParents(groupId: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Seq[Group]] = {
      val action = {
        db.getGroup(groupId).map(_.getOrNotFound(groupId)) flatMap { group =>
          db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) flatMap { channel =>
            group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(DBIO.successful(Nil)) map { parents =>
              authz.canGetGroupHierarchy(channel, group, parents.toList).ifGranted(parents)
            }
          }
        }
      }

      db.run(action)
    }
  }
}
