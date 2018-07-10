package myproject.modules.iam.api

import myproject.audit.AuditData
import myproject.common.AuthenticationFailedException
import myproject.common.security.JWT
import myproject.common.serialization.ReifiedDataWrapper
import myproject.database.Database
import myproject.modules.iam.domain.Authentication
import myproject.modules.iam.dto.UserDTO
import myproject.modules.iam.{Config, User}
import myproject.web.api.ApiFunction

import scala.concurrent.Future
import scala.concurrent.duration._

case object ApiLogin extends ApiFunction with Database with UserDTO with Authentication with JWT {

  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = p.string("login")
    val candidate = p.string("password")

    def checkLogin: PartialFunction[Option[User], Future[Unit]] = {
      case Some(_) => Future.successful(Unit)
      case None => Future.failed(AuthenticationFailedException("Authentication failed"))
    }

    for {
      user <- getByLoginName(login)
      _    <- checkLogin(loginPassword(user, candidate))
    } yield Map("whoami" -> user.serialize, "token" -> createToken(user.login, user.id, Some(Config.jwtTimeToLive.seconds)))
  }
}
