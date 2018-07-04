package myproject.modules.iam.pure

import myproject.modules.iam.{Guest, User}

trait AccessControl {

  def canImpersonate(requester: User, impersonate: User): Either[String, Unit] = impersonate match {
    case Guest() => Left("Cannot impersonate guest user")
    case _ => Right(Unit)
  }

  def canLogin(user: User): Either[String, Unit] = Right(Unit)
}
