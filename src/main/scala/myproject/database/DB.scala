package myproject.database

import myproject.iam.dao.{SlickChannelDAO, SlickGroupDAO, SlickTokenDAO, SlickUserDAO}
import slick.jdbc.JdbcProfile

object DB extends JdbcProfile with SlickUserDAO with SlickTokenDAO with SlickGroupDAO with SlickChannelDAO {

  import api._

  lazy val db = Database.forConfig("database")

  val schema = channels.schema ++ groups.schema ++ users.schema ++ tokens.schema

  def reset = {
    db.run(DBIO.seq(schema.drop.asTry, schema.create))
  }
}
