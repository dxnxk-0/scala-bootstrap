package myproject.web.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCookie}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, MissingCookieRejection, RejectionHandler}
import myproject.common.serialization.AkkaHttpMarshalling
import myproject.modules.iam.api.LoginPassword
import myproject.web.server.WebAuth
import myproject.web.views.AppView

import scala.util.Success

trait AppController extends HtmlController with AkkaHttpMarshalling with WebAuth with AppView with LoginPassword {

  private val authCookieName = "tapas-auth"

  val appName = "app"
  val loginRouteName = "login"
  val homeRouteName = "home"
  val logoutRouteName = "logout"
  val rootRouteName = appName
  val loginUrl = "/" + rootRouteName + "/" + loginRouteName
  val homeUrl = "/" + rootRouteName + "/" + homeRouteName
  val assetRoute = "public"
  val cssFileName = "app.css"
  val cssUrl = "/" + rootRouteName + "/" + s"$assetRoute/$cssFileName"
  val cssPathMatcher = assetRoute / cssFileName

  implicit val appRejectionHandler = RejectionHandler.newBuilder()
    .handle { case AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, _) =>
      complete(loginView(Some("Incorrect user or password"), loginUrl).render)
    }
    .handle { case MissingCookieRejection(name) if name==authCookieName =>
      redirect(loginUrl, StatusCodes.TemporaryRedirect)
    }
    .result()

  val AppRoute =
    pathPrefix(rootRouteName) {
      handleRejections(appRejectionHandler) {
        path(loginRouteName) {
          get {
            complete(loginView(None, loginUrl).render)
          } ~
          post {
            formFields(('login.as[String], 'password.as[String])) { (login, password) =>
              onComplete(loginPassword(login, password)) {
                case Success((user, token)) =>
                  setCookie(HttpCookie(authCookieName, token)) {
                    redirect(homeUrl, StatusCodes.SeeOther)
                  }
                case _ =>
                  reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge(appName, None)))
              }
            }
          }
        } ~
        /* ***** Start of secured section ***** */
        cookie(authCookieName) { cookie =>
          onComplete(jwtAuthenticate(cookie.value)) {
            case Success(Some(u)) =>
              get {
                path(homeRouteName) {
                  complete(homeView(u.login, cssUrl).render)
                } ~
                path(cssPathMatcher) {
                  getFromResource(s"$assetRoute/$cssFileName")
                } ~
                path(logoutRouteName) {
                  deleteCookie(authCookieName) {
                    redirect(loginUrl, StatusCodes.SeeOther)
                  }
                } ~ redirect(homeUrl, StatusCodes.SeeOther)
              }
            case _ => reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge(appName, None)))
          }
        }
        /* ***** End of secured section ***** */
      }
    } ~ redirect(homeUrl, StatusCodes.MovedPermanently)
}
