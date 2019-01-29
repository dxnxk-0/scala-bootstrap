package myproject
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.util.Try

object Config {

  implicit val logger = Logger(LoggerFactory.getLogger("configuration"))

  trait ConfigurationSection {
    def dumpLog(implicit logger: Logger): Unit
  }

  val config = sys.env.get("APP_CONFIG_FILE") match {
    case Some(f) =>
      logger.info(s"loading `$f` configuration file")
      ConfigFactory.load(f)
    case _ =>
      logger.info(s"loading default configuration file")
      ConfigFactory.load()
  }

  def dumpLog = {
    DataLoading.dumpLog
    Database.dumpLog
    Server.dumpLog
    Security.dumpLog
    Email.dumpLog
    Email.Smtp.dumpLog
  }

  implicit private class DurationExtension(d: java.time.Duration) {
    def toScala = Duration.fromNanos(d.toNanos)
  }

  object DataLoading extends ConfigurationSection {
    private val dataInitConfig = config.getConfig("data-loading")
    val clazz = dataInitConfig.getString("class")
    val enabled = dataInitConfig.getBoolean("enabled")

    override def dumpLog(implicit logger: Logger) = {
      logger.info(s"data-loading > class > $clazz")
      logger.info(s"data-loading > enabled > $enabled")
    }
  }

  object Database extends ConfigurationSection {
    private val databaseConfig = config.getConfig("database")
    val implClassName = databaseConfig.getString("class")

    override def dumpLog(implicit logger: Logger) = {
      logger.info(s"database > class > $implClassName")
    }

    object H2 extends ConfigurationSection {
      private val h2Config = databaseConfig.getConfig("h2")
      val startWebInterface = h2Config.getBoolean("start-web-interface")

      override def dumpLog(implicit logger: Logger) = {
        logger.info(s"h2 > start web interface: $startWebInterface")
      }
    }

    H2.dumpLog

    object Slick extends ConfigurationSection {
      private val slickConfig = databaseConfig.getConfig("slick")
      val driver = slickConfig.getString("driver")
      val url = slickConfig.getString("url")
      val connectionPool = slickConfig.getString("connectionPool")
      val keepAliveConnection = Try(slickConfig.getBoolean("keepAliveConnection")).getOrElse("-")

      val numThreads = Try(slickConfig.getInt("numThreads")).getOrElse("-")
      val registerMbeans = Try(slickConfig.getBoolean("registerMbeans")).getOrElse("-")
      val poolName = Try(slickConfig.getString("poolName")).getOrElse("-")

      private val properties = Try(slickConfig.getConfig("properties")).toOption
      val user = properties.map(_.getString("user")).getOrElse("-")
      val password = "******"

      override def dumpLog(implicit logger: Logger) = {
        logger.info(s"slick > driver > $driver")
        logger.info(s"slick > url > $url")
        logger.info(s"slick > connectionPool > $connectionPool")
        logger.info(s"slick > keepAliveConnection > $keepAliveConnection")
        logger.info(s"slick > numThreads > $numThreads")
        logger.info(s"slick > registerMbeans > $registerMbeans")
        logger.info(s"slick > poolName > $poolName")
        logger.info(s"slick > user > $user")
        logger.info(s"slick > password > $password")
      }
    }

    Slick.dumpLog
  }

  object Server extends ConfigurationSection {
    private val serverConfig = config.getConfig("server")
    val interface = serverConfig.getString("interface")
    val port = serverConfig.getInt("port")
    val sessionDuration = serverConfig.getDuration("session-duration").toScala
    val uiBaseUrl = serverConfig.getString("ui-base-url")
    val resetDbAtStartup = serverConfig.getBoolean("migrate-db-at-startup")

    override def dumpLog(implicit logger: Logger) = {
      logger.info(s"server > interface > $interface")
      logger.info(s"server > port > $port")
      logger.info(s"server > session duration > $sessionDuration")
      logger.info(s"server > ui base url > $uiBaseUrl")
      logger.info(s"server > reset-db-at-startup > $resetDbAtStartup")
    }
  }

  object Security extends ConfigurationSection {
    private val securityConfig = config.getConfig("security")
    val secret = securityConfig.getString("secret")
    val bcryptWork = securityConfig.getInt("bcrypt-work")
    val jwtTimeToLive = securityConfig.getDuration("jwt-ttl").toScala
    val resetPasswordTokenValidity = securityConfig.getDuration("reset-password-token-validity").toScala

    override def dumpLog(implicit logger: Logger) = {
      logger.info(s"security > application secret > *****")
      logger.info(s"security > bcrypt work > $bcryptWork")
      logger.info(s"security > jwt ttl > $jwtTimeToLive")
      logger.info(s"security > reset password token validity > $resetPasswordTokenValidity")
    }
  }

  object Email extends ConfigurationSection {
    private val emailConfig = config.getConfig("email")
    val templatesPath = emailConfig.getString("templates-path")

    override def dumpLog(implicit logger: Logger) = {
      logger.info(s"email > templates path > $templatesPath")
    }

    object Smtp extends ConfigurationSection {
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

      override def dumpLog(implicit logger: Logger) = {
        logger.info(s"smtp > mail host > $mailHost")
        logger.info(s"smtp > mail port > $mailPort")
        logger.info(s"smtp > start tls > $smtpStartTLS")
        logger.info(s"smtp > auth user > $smtpAuthUser")
        logger.info(s"smtp > password > *****")
        logger.info(s"smtp > mail from > $mailFrom")
        logger.info(s"smtp > admin email > $adminEmail")
        logger.info(s"smtp > disable email > $disableEmail")
        logger.info(s"smtp > connection timeout > $smtpConnectionTimeout")
      }
    }
  }
}