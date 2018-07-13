package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.AuthenticationFailedException
import myproject.common.Runtime.ec
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB
import myproject.iam.Users.User

import scala.concurrent.Future
import scala.concurrent.duration._

object IAM {

  def createUser(login: String, password: String): Future[User] = {
    val user = User(UUID.randomUUID(), login, BCrypt.hashPassword(password))
    DB.insert(user)
  }

  def updateUser(user: User): Future[User] = for {
    _      <- DB.getById(user.id)
    saved  <- DB.update(user)
  } yield saved

  def getUser(userId: UUID): Future[User] = DB.getById(userId)

  def loginPassword(login: String, candidate: String): Future[(User, String)] = {

    def checkLogin: PartialFunction[Either[String, Unit], Future[Unit]] = {
      case Right(_) => Future.successful(Unit)
      case Left(error: String) => Future.failed(AuthenticationFailedException(error))
    }

    for {
      user <- DB.getByLoginName(login)
      _    <- checkLogin(Authentication.loginPassword(user, candidate))
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}

