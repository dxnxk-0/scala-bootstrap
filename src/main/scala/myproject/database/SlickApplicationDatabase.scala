package myproject.database

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

class SlickApplicationDatabase()
  extends ApplicationDatabase
    with JdbcProfile
    with SlickChannelDAO
    with SlickGroupDAO
    with SlickUserDAO
    with SlickTokenDAO {

  import SlickConfig.driver.api._

  lazy val db = Database.forConfig("database.slick")

  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema

  def reset = {
    db.run(DBIO.seq(schema.drop.asTry, schema.create)).map(_ => Done)
  }

  def close = Future(db.close()).map(_ => Done)
}
