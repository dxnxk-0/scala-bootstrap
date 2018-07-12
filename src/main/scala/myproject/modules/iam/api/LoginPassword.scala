package myproject.modules.iam.api

import myproject.Config
import myproject.api.ApiFunction
import myproject.audit.AuditData
import myproject.common.AuthenticationFailedException
import myproject.common.security.JWT
import myproject.common.serialization.ReifiedDataWrapper
import myproject.database.Database
import myproject.modules.iam.User
import myproject.modules.iam.domain.Authentication
import myproject.modules.iam.dto.UserDTO

import scala.concurrent.Future
import scala.concurrent.duration._

case object LoginPassword extends ApiFunction with UserDTO with LoginPassword {

  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = p.string("login")
    val candidate = p.string("password")

    loginPassword(login, candidate) map {
      case (user, token) => Map("whoami" -> user.serialize, "token" -> token)
    }
  }
}

trait LoginPassword extends Database with Authentication with JWT {

  private def checkLogin: PartialFunction[Either[String, AccessGranted], Future[Unit]] = {
    case Right(_) => Future.successful(Unit)
    case Left(error) => Future.failed(AuthenticationFailedException(error))
  }

  def loginPassword(login: String, candidate: String): Future[(User, String)] = for {
    user <- getByLoginName(login)
    _    <- checkLogin(loginPassword(user, candidate))
  } yield (user, createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
}
