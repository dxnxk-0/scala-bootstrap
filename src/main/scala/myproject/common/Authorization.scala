package myproject.common

import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users._

import scala.util.{Failure, Success, Try}

object Authorization {

  sealed trait AccessGranted
  case object AccessGranted extends AccessGranted
  type AuthorizationCheck = Try[AccessGranted]

  implicit class AuthorizationCheckOperators(authz: AuthorizationCheck) {
    def and(other: AuthorizationCheck) = authz.flatMap(_ => other)
    def or(other: AuthorizationCheck) = authz.orElse(other)
  }

  trait AccessChecker {

    implicit val requester: Option[User]
    
    def refuse(implicit requester: Option[User]) = requester match {
      case Some(r) => Failure(AccessRefusedException(s"user with id ${r.id} is not granted to perform the requested operation"))
      case None => Failure(AccessRefusedException(s"operation not granted (no identified requester)"))
    }
    def grant = Success(AccessGranted)

    protected def isPlatformAdmin = requester.map(r => if(r.level==UserLevel.Platform) grant else refuse) getOrElse refuse
    protected def isChannelAdmin(implicit channel: Channel) = requester.map(r => if(r.level==UserLevel.Channel && r.channelId.contains(channel.id)) grant else refuse) getOrElse refuse
    protected def isGroupAdmin(implicit group: Group) = requester.map(r => if(r.level==UserLevel.Group && r.groupRole.contains(GroupRole.Admin) && r.groupId.contains(group.id)) grant else refuse) getOrElse refuse
    protected def isUserHimself(implicit user: User) = requester.map(r => if(user.id==r.id) grant else refuse) getOrElse refuse
    protected def isInTheSameGroup(implicit user: User) = requester.map(r => if(user.level==UserLevel.Group && user.groupId.isDefined && user.groupId==r.groupId) grant else refuse) getOrElse refuse
    protected def belongToTheGroup(implicit group: Group) = requester.map(r => if(r.groupId.contains(group.id)) grant else refuse) getOrElse refuse
    protected def isAdminOfOneGroup(groups: List[Group]) = requester.map(r => if(groups.exists(g => r.groupId.contains(g.id)) && r.groupRole.contains(GroupRole.Admin)) grant else refuse) getOrElse refuse
    protected def belongToOneGroup(groups: List[Group]) = requester.map(r => if(groups.exists(g => r.groupId.contains(g.id))) grant else refuse) getOrElse refuse
  }
}
