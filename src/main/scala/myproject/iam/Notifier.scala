package myproject.iam

import myproject.Config
import myproject.common.{DefaultMailer, SimpleEmailData}
import myproject.iam.Tokens.Token
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future

trait Notifier {
  def sendMagicLink(emailAddress: EmailAddress, token: Token): Future[Any]
}

object DefaultNotifier extends Notifier {
  override def sendMagicLink(emailAddress: EmailAddress, token: Token) = {
    val url = s"${Config.server.uiBaseUrl}/login/${token.id}"
    val email = SimpleEmailData(None, List(emailAddress), s"prolicent magic login", s"Please login: $url")
    DefaultMailer.send(email)
  }
}
