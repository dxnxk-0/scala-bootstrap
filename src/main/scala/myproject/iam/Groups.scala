package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Authorization._
import myproject.common.OptionImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.Validation.{ValidationError, Validator, _}
import myproject.common.{Done, IllegalOperationException, TimeManagement}
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

  case class Group(
      id: UUID,
      name: String,
      channelId: UUID,
      status: GroupStatus = GroupStatus.Active,
      created: Option[LocalDateTime] = None,
      lastUpdate: Option[LocalDateTime] = None,
      parentId: Option[UUID] = None)

  case object InvalidParentId extends ValidationError

  private object GroupValidator extends Validator[Group] {
    override val validators = List(
      (g: Group) => if(g.parentId.contains(g.id)) NOK(InvalidParentId) else OK
    )
  }

  type GroupUpdate = Group => Group

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

    def createGroup(group: Group)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Group] = {
      val action = {
        db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) flatMap { channel =>
          authz.canCreateGroup(channel, group) ifGranted {
            GroupValidator.validate(group.copy(created = Some(getCurrentDateTime))) ifValid { g =>
              for {
                _ <- checkParentGroup(g)
                _ <- db.insert(g)
              } yield g
            }
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

    def updateGroup(id: UUID, upd: GroupUpdate)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO with DatabaseInterface): Future[Group] = {
      def filter(existing: Group, candidate: Group) = {
        if(existing.channelId!=candidate.channelId)
          throw IllegalOperationException(s"attempt to move user: operation is not supported")

        existing.copy(
          name = candidate.name,
          parentId = candidate.parentId,
          status = candidate.status,
          lastUpdate = Some(TimeManagement.getCurrentDateTime))
      }

      def processAuthz(existing: Group, target: Group, channel: Channel, parents: List[Group]) =
        if(existing.status != target.status || existing.parentId != target.parentId) authz.canAdminGroup(channel, target)
        else authz.canUpdateGroup(channel, existing, parents)


      val action = {
        val dependencies = for {
          existing <- db.getGroup(id).map(_.getOrNotFound(id))
          channel  <- db.getChannel(existing.channelId).map(_.getOrNotFound(existing.channelId))
          parents  <- existing.parentId.map(_ => db.getGroupParents(existing.id)).getOrElse(DBIO.successful(Nil))
        } yield (existing, channel, parents)

        dependencies flatMap { case (existing, channel, parents) =>
          val updated = filter(existing, upd(existing))

          processAuthz(existing, updated, channel, parents.toList) ifGranted {
            GroupValidator.validate(updated) ifValid { g =>
              for {
                _ <- checkParentGroup(g)
                _ <- db.update(g)
              } yield g
            }
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
