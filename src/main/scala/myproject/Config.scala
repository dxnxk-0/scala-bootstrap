package myproject

import com.typesafe.config.ConfigFactory

import scala.util.Try

object Config {

  private val config = ConfigFactory.load()

  object server {
    private val serverConfig = config.getConfig("server")
    val interface = serverConfig.getString("interface")
    val port = serverConfig.getInt("port")
    val sessionDuration = serverConfig.getDuration("session-duration")
  }

  object security {
    private val securityConfig = config.getConfig("security")
    val secret = securityConfig.getString("secret")
    val bcryptWork = securityConfig.getInt("bcrypt-work")
    val jwtTimeToLive = securityConfig.getInt("jwt-ttl")
  }

  object email {
    private val emailConfig = ConfigFactory.load().getConfig("email")
    val templatesPath = emailConfig.getString("templates-path")

    object smtp {
      private val smtpConfig = emailConfig.getConfig("smtp")
      val mailHost = smtpConfig.getString("mailhost")
      val mailPort = smtpConfig.getInt("mailport")
      val smtpStartTLS = smtpConfig.getBoolean("starttls")
      val smtpAuthUser = Try(smtpConfig.getString("username")).toOption
      val smtpAuthPassword = Try(smtpConfig.getString("password")).toOption
      val mailFrom = smtpConfig.getString("mailfrom")
      val adminEmail = smtpConfig.getString("admin")
      val disableEmail = Try(smtpConfig.getBoolean("disable")).getOrElse(false)
      val smtpConnectionTimeout = 3000
    }
  }
}
