package myproject.modules.dummy

import com.typesafe.config.ConfigFactory

object Config {

  private val config = ConfigFactory.load().getConfig("modules.dummy")
}
