package myproject.database

import myproject.iam.dao.{ChannelDAO, GroupDAO, TokenDAO, UserDAO}
import slick.jdbc.JdbcProfile

object DB extends JdbcProfile with UserDAO with TokenDAO with GroupDAO with ChannelDAO {

  import api._

  lazy val db = Database.forConfig("database")

  val schema = channels.schema ++ groups.schema ++ organizations.schema ++ users.schema ++ tokens.schema

  def reset = {
    db.run(DBIO.seq(schema.drop.asTry, schema.create))
  }
}
