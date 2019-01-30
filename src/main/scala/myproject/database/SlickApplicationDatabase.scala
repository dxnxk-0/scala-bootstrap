package myproject.database

import java.util.UUID

import myproject.Config
import myproject.common.UnexpectedErrorException
import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import org.flywaydb.core.Flyway
import slick.jdbc.{H2Profile, JdbcProfile, MySQLProfile, PostgresProfile}

trait SlickApplicationDatabaseBase
  extends ApplicationDatabase
    with SlickChannelDAO
    with SlickGroupDAO
    with SlickUserDAO
    with SlickTokenDAO { self: SlickProfile =>

  import slickProfile.api._

  lazy val flyway = {
    import Config.Database.{Flyway => FlywayCfg}

    val config = Flyway.configure().dataSource(url, user.getOrElse(""), password.getOrElse(""))
    FlywayCfg.group.map(v => config.group(v))
    FlywayCfg.cleanDisabled.map(v => config.cleanDisabled(v))
    config.load()
  }

  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema
  def close() = db.close()
  def clean() = flyway.clean()
  def migrate() = flyway.migrate()
}

trait SlickProfile {
  val slickProfile: JdbcProfile
}

trait H2SlickProfile extends SlickProfile {
  override val slickProfile = H2Profile
}

trait SlickProfileFromConfig extends SlickProfile {
  override val slickProfile = Config.Database.Slick.driver match {
    case "org.postgresql.Driver" => PostgresProfile
    case "org.h2.Driver" => H2Profile
    case "slick.driver.MySQL" => MySQLProfile
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}

class SlickApplicationDatabase extends SlickProfileFromConfig with SlickApplicationDatabaseBase {
  import slickProfile.api._

  override val url = Config.Database.Slick.url
  override val user = Config.Database.Slick.user
  override val password = Config.Database.Slick.password

  val db = Database.forConfig(config = Config.config, path = "database.slick")

  override val dbType = slickProfile match {
    case PostgresProfile => DatabaseType.Postgresql
    case H2Profile => DatabaseType.H2
    case MySQLProfile => DatabaseType.Mysql
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}

class SlickH2ApplicationDatabase(dbName: Option[String] = None) extends H2SlickProfile with SlickApplicationDatabaseBase {
  import slickProfile.api._

  override val dbType = DatabaseType.H2

  val name = dbName.getOrElse(UUID.randomUUID.toString)

  override val url = s"jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1"
  override val user: Option[String] = None
  override val password: Option[String] = None

  val executor = {
    val maxConnections = 1
    AsyncExecutor(
      name = s"h2-$name-db-async-executor",
      minThreads = maxConnections,
      maxThreads = maxConnections,
      queueSize = 10000,
      maxConnections = maxConnections)
  }

  lazy val db = Database.forURL(
    url = s"jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1",
    driver="org.h2.Driver",
    executor = executor)
}
