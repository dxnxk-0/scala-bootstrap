package myproject.web.controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCookie}
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, MissingCookieRejection, RejectionHandler}
import myproject.common.serialization.AkkaHttpMarshalling
import myproject.iam.Authorization
import myproject.iam.Users.CRUD.loginPassword
import myproject.web.server.WebAuth._
import myproject.web.views.AppView._

import scala.util.Success

object AppController extends HtmlController {

  private val authCookieName = "tapas-auth"

  private implicit val htmlMarshaller = AkkaHttpMarshalling.getHtmlMarshaller

  private val appName = "app"
  private val loginRouteName = "login"
  private val homeRouteName = "home"
  private val logoutRouteName = "logout"
  private val rootRouteName = appName
  private val loginUrl = "/" + rootRouteName + "/" + loginRouteName
  private val homeUrl = "/" + rootRouteName + "/" + homeRouteName
  private val assetRoute = "public"
  private val cssFileName = "app.css"
  private val cssUrl = "/" + rootRouteName + "/" + s"$assetRoute/$cssFileName"
  private val cssPathMatcher = assetRoute / cssFileName

  private implicit val appRejectionHandler = RejectionHandler.newBuilder()
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
              onComplete(loginPassword(login, password, u => Authorization.canLogin(u, _))) {
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
