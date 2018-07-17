package myproject.database

import myproject.iam.dao.{CompanyDAO, ChannelDAO, TokenDAO, UserDAO}
import slick.jdbc.JdbcProfile

object DB extends JdbcProfile with UserDAO with TokenDAO with CompanyDAO with ChannelDAO {

  import api._

  lazy val db = Database.forConfig("database")

  val schema = channels.schema ++ companies.schema ++ users.schema ++ tokens.schema

  def reset = db.run(DBIO.seq(schema.drop.asTry, schema.create))
}
