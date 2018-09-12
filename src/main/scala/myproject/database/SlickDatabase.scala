package myproject.database

import myproject.common.Done
import myproject.common.Runtime.ec
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

class SlickDatabase() extends ApplicationDatabase with JdbcProfile {

  import api._

  lazy val db = Database.forConfig("database.slick")

  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema

  def reset = {
    db.run(DBIO.seq(schema.drop.asTry, schema.create)).map(_ => Done)
  }

  def close = Future(db.close()).map(_ => Done)
}
