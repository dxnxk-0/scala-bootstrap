package myproject.database

import myproject.iam.dao.{TokenDAO, UserDAO}
import slick.jdbc.JdbcProfile

object DB extends JdbcProfile with UserDAO with TokenDAO {

  import api._

  lazy val db = Database.forConfig("database")

  val schema = users.schema ++ tokens.schema

  def reset = db.run(DBIO.seq(schema.drop, schema.create))
}
