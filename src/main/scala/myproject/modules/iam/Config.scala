package myproject.modules.iam

import com.typesafe.config.ConfigFactory

object Config {

  private val config = ConfigFactory.load().getConfig("modules.iam")

  val bcryptWork = config.getInt("bcrypt-work")
  val jwtTimeToLive = config.getInt("jwt-ttl")
}
