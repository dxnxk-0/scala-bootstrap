package myproject.web.server

import akka.http.scaladsl.server.Directives.AsyncAuthenticatorPF
import akka.http.scaladsl.server.directives.Credentials
import myproject.common.security.JWT
import myproject.common.{AuthenticationFailedException, DefaultExecutionContext}
import myproject.modules.iam.dao.UserDAO
import myproject.modules.iam.{Guest, User, UserGeneric}

import scala.concurrent.Future

trait WebAuth extends DefaultExecutionContext with UserDAO {

  private def authenticate(token: String): Future[User] = {
    Future(JWT.extractToken(token)) flatMap {
      case Left(e) => throw AuthenticationFailedException(e.msg) // Future.failed
      case Right(payload) => getById(payload.uid)
    }
  }

  def jwtAuthenticator: AsyncAuthenticatorPF[UserGeneric] = {
    case Credentials.Missing => Future.successful(Guest())
    case Credentials.Provided(token) => authenticate(token)
  }
}
