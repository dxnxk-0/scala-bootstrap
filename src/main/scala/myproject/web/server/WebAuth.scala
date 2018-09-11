package myproject.web.server

import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import akka.http.scaladsl.server.directives.Credentials
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.security.JWT
import myproject.iam.Users._

import scala.concurrent.Future

object WebAuth {

  def jwtAuthenticate(token: String)(implicit db: UserDAO): Future[Option[User]] = {
    Future(JWT.extractToken(token)) flatMap {
      case Left(_) => Future.successful(None)
      case Right(payload) =>
        db.getUserByIdF(payload.uid) map (Some(_)) recover { case ObjectNotFoundException(_) => None }
    }
  }

  def jwtAuthenticator(implicit db: UserDAO): AsyncAuthenticator[UserGeneric] = {
    case Credentials.Missing => Future.successful(Some(Guest()))
    case Credentials.Provided(token) => jwtAuthenticate(token)
  }
}
