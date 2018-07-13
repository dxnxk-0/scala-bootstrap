package myproject.web.server

import myproject.common.FutureImplicits._
import myproject.database.DB

object EnvInit {

  def initEnv(): Unit = DB.reset.futureValue
}
