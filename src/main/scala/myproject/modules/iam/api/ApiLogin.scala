package myproject.modules.iam.api

import myproject.audit.AuditData
import myproject.common.AuthenticationFailedException
import myproject.common.serialization.ReifiedDataWrapper
import myproject.modules.iam.User
import myproject.modules.iam.dao.UserDAO
import myproject.modules.iam.domain.Authentication
import myproject.modules.iam.dto.UserDTO
import myproject.web.api.ApiFunction

import scala.concurrent.Future

case object ApiLogin extends ApiFunction with UserDAO with UserDTO with Authentication {
  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = Future {
    val login = p.string("login")
    val candidate = p.string("password")

    def checkLogin: PartialFunction[Option[User], Future[Unit]] = {
      case Some(_) => Future.successful(Unit)
      case None => Future.failed(AuthenticationFailedException("Authentication failed"))
    }

    for {
      user           <- getByLogin(login)
      hashedPassword <- getPassword(user.id)
      _              <- checkLogin(loginPassword(user, candidate, hashedPassword))
    } yield user.serialize
  }
}
