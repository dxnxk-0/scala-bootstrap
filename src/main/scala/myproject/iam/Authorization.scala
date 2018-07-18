package myproject.iam

import myproject.common.AccessRefusedException
import myproject.iam.Users._

import scala.util.{Failure, Success}

object Authorization {

  sealed trait AccessGranted
  case object AccessGranted extends AccessGranted

  private def block(u: User) = Failure(AccessRefusedException(s"user with id ${u.id} is not granted to perform the requested operation"))
  private def success = Success(AccessGranted)

  def authzLoginAccess(user: User) = success
}

