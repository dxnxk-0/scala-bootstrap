package myproject.database

import myproject.modules.iam.dao.UserDAO
import slick.jdbc.JdbcProfile

trait Database extends UserDAO {
  override val db = DatabaseSingleton.db
}

object DatabaseSingleton extends JdbcProfile {

  import api._

  lazy val db = Database.forConfig("database")
}
