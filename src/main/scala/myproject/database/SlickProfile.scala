package myproject.database

import myproject.Config
import myproject.common.UnexpectedErrorException
import slick.jdbc.{H2Profile, JdbcProfile, MySQLProfile, PostgresProfile}

trait SlickProfile {
  val slickProfile: JdbcProfile
}

trait H2SlickProfile extends SlickProfile {
  override val slickProfile = H2Profile
}

trait SlickProfileFromConfig extends SlickProfile {
  override val slickProfile = Config.Database.Slick.driver match {
    case "org.postgresql.Driver" => PostgresProfile
    case "org.h2.Driver" => H2Profile
    case "slick.driver.MySQL" => MySQLProfile
    case d => throw UnexpectedErrorException(s"not supported `$d` driver")
  }
}