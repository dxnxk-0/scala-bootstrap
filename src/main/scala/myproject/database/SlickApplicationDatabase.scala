package myproject.database

import myproject.Config
import myproject.common.Runtime.ec
import myproject.common.{Done, UnexpectedErrorException}
import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import slick.jdbc.{H2Profile, JdbcProfile, MySQLProfile, PostgresProfile}

import scala.concurrent.Future

trait SlickApplicationDatabaseBase
  extends ApplicationDatabase
    with SlickChannelDAO
    with SlickGroupDAO
    with SlickUserDAO
    with SlickTokenDAO { self: SlickProfile =>

  import slickProfile.api._
  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema
  def init = db.run(DBIO.seq(schema.drop.asTry, schema.create)).map(_ => Done)
  def close = Future(db.close()).map(_ => Done)
}

trait SlickProfile {
  val slickProfile: JdbcProfile
}

trait H2SlickProfile extends SlickProfile {
  override val slickProfile = H2Profile
}

trait SlickProfileFromConfig extends SlickProfile {
  override val slickProfile = Config.database.slick.driver match {
    case "org.postgresql.Driver" => PostgresProfile
    case "org.h2.Driver" => H2Profile
    case "slick.driver.MySQL" => MySQLProfile
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}

class SlickApplicationDatabase extends SlickProfileFromConfig with SlickApplicationDatabaseBase {
  import slickProfile.api._
  val db = Database.forConfig("database.slick")
  override val dbType = slickProfile match {
    case PostgresProfile => DatabaseType.Postgresql
    case H2Profile => DatabaseType.H2
    case MySQLProfile => DatabaseType.Mysql
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}

class SlickH2ApplicationDatabase extends H2SlickProfile with SlickApplicationDatabaseBase {
  import slickProfile.api._
  val db = Database.forURL("jdbc:h2:mem:myproject;DB_CLOSE_DELAY=-1")
  override val dbType = DatabaseType.H2
}
