package myproject.database

import myproject.Config
import myproject.common.Runtime.ec
import myproject.common.{Done, UnexpectedErrorException}
import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait SlickApplicationDatabaseBase
  extends ApplicationDatabase
    with JdbcProfile
    with SlickChannelDAO
    with SlickGroupDAO
    with SlickUserDAO
    with SlickTokenDAO {

  import api._
  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema
  def reset = db.run(DBIO.seq(schema.drop.asTry, schema.create)).map(_ => Done)
  def close = Future(db.close()).map(_ => Done)
}

class SlickApplicationDatabase extends SlickApplicationDatabaseBase {
  import api._
  lazy val db = Database.forConfig("database.slick")
  override val dbType = Config.database.slick.driver match {
    case "org.postgresql.Driver" => DatabaseType.Postgresql
    case "org.h2.Driver" => DatabaseType.H2
    case "slick.driver.MySQL" => DatabaseType.Mysql
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}

class SlickH2ApplicationDatabase extends SlickApplicationDatabaseBase {
  import api._
  lazy val db = Database.forURL("jdbc:h2:mem:myproject")
  override val dbType = DatabaseType.H2
}
