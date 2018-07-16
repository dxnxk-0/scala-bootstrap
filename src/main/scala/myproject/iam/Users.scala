package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.Runtime.ec
import myproject.common.security.{BCrypt, JWT}
import myproject.common.{AuthenticationFailedException, Done, IllegalOperationException}
import myproject.database.DB

import scala.concurrent.Future
import scala.concurrent.duration._

object Users {

  sealed trait UserGeneric {
    val id: UUID
    val login: String
  }

  case class User(id: UUID, login: String, hashedPassword: String, companyId: UUID)
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val id = UUID fromString "99999999-9999-9999-9999-999999999999"
    val login = "guest"
  }

  def createUser(login: String, password: String, companyId: UUID): Future[User] = {
    val user = User(UUID.randomUUID(), login, BCrypt.hashPassword(password), companyId)
    DB.insert(user)
  }

  def updateUser(user: User): Future[User] = {
    def checkUpdate: PartialFunction[User, Future[Done]] = {
      case u if u.companyId != user.companyId =>
        Future.failed(IllegalOperationException(s"a user's company id cannot be changed"))
      case _ =>
        Future.successful(Done)
    }

    for {
      _      <- DB.getById(user.id) map checkUpdate
      saved  <- DB.update(user)
    } yield saved
  }

  def getUser(userId: UUID): Future[User] = DB.getById(userId)

  def loginPassword(login: String, candidate: String): Future[(User, String)] = {

    def checkLogin: PartialFunction[Either[String, Done], Future[Done]] = {
      case Right(_) => Future.successful(Done)
      case Left(error: String) => Future.failed(AuthenticationFailedException(error))
    }

    for {
      user <- DB.getByLoginName(login)
      _    <- checkLogin(Authentication.loginPassword(user, candidate))
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }

  def deleteUser(id: UUID): Future[Done] = DB.deleteUser(id)
}