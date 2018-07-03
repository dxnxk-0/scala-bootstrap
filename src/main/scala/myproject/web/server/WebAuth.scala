package myproject.web.server

import akka.http.scaladsl.server.Directives.AsyncAuthenticatorPF
import akka.http.scaladsl.server.directives.Credentials
import myproject.common.{AuthenticationFailedException, DefaultExecutionContext}
import myproject.common.security.JWT
import myproject.identity.{ApiUser, Guest, User}

import scala.concurrent.Future

trait WebAuth extends DefaultExecutionContext {

  private def authenticate(token: String) = Future {
    JWT.extractToken(token) match {
      case Left(e) => throw AuthenticationFailedException(e.msg) // Future.failed
      case Right(payload) => ApiUser(payload.uid, payload.sub)
    }
  }

  def jwtAuthenticator: AsyncAuthenticatorPF[User] = {
    case Credentials.Missing => Future.successful(Guest())
    case Credentials.Provided(token) => authenticate(token)
  }
}
