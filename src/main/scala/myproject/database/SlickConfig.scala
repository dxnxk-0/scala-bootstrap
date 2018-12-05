package myproject.database

import myproject.Config
import myproject.common.UnexpectedErrorException
import slick.jdbc.{H2Profile, JdbcProfile, MySQLProfile, PostgresProfile}

object SlickConfig {
  lazy val driver: JdbcProfile = Config.database.slick.driver match {
    case "org.postgresql.Driver" => PostgresProfile
    case "org.h2.Driver" => H2Profile
    case "slick.driver.MySQL" => MySQLProfile
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}
