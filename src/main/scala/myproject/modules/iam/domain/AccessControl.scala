package myproject.modules.iam.domain

import myproject.modules.iam.User

trait AccessControl {

  def canImpersonate(requester: User, impersonate: User): Either[String, Unit] = ???

  def canLogin(user: User): Either[String, Unit] = Right(Unit)
}
