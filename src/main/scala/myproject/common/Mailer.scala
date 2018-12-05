package myproject.common

import com.typesafe.scalalogging.Logger
import myproject.common.Runtime.ec
import org.apache.commons.mail.{Email, HtmlEmail, SimpleEmail}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class SimpleEmailData(
  from: Option[EmailAddress],
  recipients: List[EmailAddress],
  subject: String,
  message: String)

case class MultipartEmailData(
  from: Option[EmailAddress],
  recipients: List[EmailAddress],
  subject: String,
  txt: String,
  html: String)

case class SendMailResult(msgId: Option[String], msg: Option[String] = None)

trait Mailer {
  def send(data: SimpleEmailData): Future[SendMailResult]
  def send(data: MultipartEmailData): Future[SendMailResult]
}

object DefaultMailer extends Mailer {

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  import myproject.Config.email.smtp._

  def send(data: SimpleEmailData): Future[SendMailResult] = {
    val recipientsLog = data.recipients.mkString(",")

    if (disableEmail) {
      logger.info(s"will not send a mail to $recipientsLog (emails sending disabled)")
      Future.successful(SendMailResult(None, Some("Email disabled. No email was sent.")))
    }
    else
      Future {
        val start = System.currentTimeMillis
        val mailService = initMsg[SimpleEmail](new SimpleEmail())

        mailService.setFrom(data.from.map(_.toString).getOrElse(mailFrom))
        data.recipients.foreach(recipient => mailService.addTo(recipient))
        mailService.setSubject(data.subject)
        mailService.setMsg(data.message)

        Try(mailService.send()) match {
          case Success(res) =>
            val timeElapsed = System.currentTimeMillis - start
            logger.info(s"successfully sent email [to:$recipientsLog, subject:${data.subject}] ($res) [${timeElapsed}ms]")
            SendMailResult(Some(res))
          case Failure(e) =>
            logger.error(
              s"""error while sending following email [to:$recipientsLog, subject:${data.subject}]. Error:${e.getMessage} caused by ${e.getCause}""")
            throw e
        }
      }
  }

  private def initMsg[A <: Email](mail: A): A = {
    mail.setHostName(mailHost)
    mail.setStartTLSEnabled(smtpStartTLS)
    if (smtpAuthUser.isDefined && smtpAuthPassword.isDefined) {
      mail.setAuthentication(smtpAuthUser.get, smtpAuthPassword.get)
    }
    mail.setSmtpPort(mailPort)

    mail.setSocketConnectionTimeout(smtpConnectionTimeout)
    mail
  }

  def send(data: MultipartEmailData): Future[SendMailResult] = {
    if (disableEmail) {
      Future.successful(SendMailResult(None, Some("Email disabled. No email was sent.")))
    }
    else {
      Future {
        val start = System.currentTimeMillis
        val mailService = initMsg[HtmlEmail](new HtmlEmail())

        mailService.setFrom(data.from.map(_.toString).getOrElse(mailFrom))
        data.recipients.foreach(recipient => mailService.addTo(recipient))
        if (!adminEmail.isEmpty) mailService.addBcc(adminEmail) // TODO to be improved
        mailService.setSubject(data.subject)
        mailService.setHtmlMsg(data.html)
        mailService.setTextMsg(data.txt)

        val recipientsLog = data.recipients.mkString(",")

        Try(mailService.send()) match {
          case Success(res) =>
            val timeElapsed = System.currentTimeMillis - start
            logger.info(s"Successfully sent email [to:$recipientsLog, subject:${data.subject}] ($res) [${timeElapsed}ms]")
            SendMailResult(Some(res))
          case Failure(e) =>
            logger.error(s"Error while sending following email [to:$recipientsLog, subject:${data.subject}]. Error:${e.getMessage} caused by ${e.getCause}")
            throw e
        }
      }
    }
  }
}