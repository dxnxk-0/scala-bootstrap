package myproject

import com.typesafe.config.ConfigFactory

object Config {

  private val config = ConfigFactory.load()
  
  object server {
    private val srvConfig = config.getConfig("server")
    val interface = srvConfig.getString("interface")
    val port = srvConfig.getInt("port")
    val sessionDuration = srvConfig.getDuration("session-duration")
  }

  object security {
    val secret = config.getString("secret")
    val bcryptWork = config.getInt("bcrypt-work")
    val jwtTimeToLive = config.getInt("jwt-ttl")
  }
}
