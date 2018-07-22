package myproject.common

import myproject.iam.Users._

import scala.util.{Failure, Success, Try}

object Authorization {

  sealed trait AccessGranted
  case object AccessGranted extends AccessGranted
  type AuthorizationCheck = Try[AccessGranted]

  trait AuthzData
  type AuthzChecker = AuthzData => AuthorizationCheck
  type AuthzCheckerFactory = User => AuthzData => AuthorizationCheck

  def refuse(implicit requester: User, data: AuthzData) = Failure(AccessRefusedException(s"user with id ${requester.id} is not granted to perform the requested operation"))
  def grant = Success(AccessGranted)
}
