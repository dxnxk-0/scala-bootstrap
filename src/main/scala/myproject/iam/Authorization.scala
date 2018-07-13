package myproject.iam

import myproject.iam.Users.User

object Authorization {

  def canImpersonate(requester: User, impersonate: User): Either[String, Unit] = ???

  def canLogin(user: User): Either[String, Unit] = Right(Unit)
}

