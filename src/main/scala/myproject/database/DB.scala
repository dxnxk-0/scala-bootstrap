package myproject.database

import myproject.iam.dao.{CompanyDAO, TokenDAO, UserDAO}
import slick.jdbc.JdbcProfile

object DB extends JdbcProfile with UserDAO with TokenDAO with CompanyDAO {

  import api._

  lazy val db = Database.forConfig("database")

  val schema = companies.schema ++ users.schema ++ tokens.schema

  def reset = db.run(DBIO.seq(schema.drop.asTry, schema.create))
}
