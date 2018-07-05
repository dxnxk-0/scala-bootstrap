package myproject.web.server

import myproject.common.FutureImplicits
import myproject.database.DBInit

trait EnvInit extends DBInit with FutureImplicits {

  def initEnv(): Unit = db.run(setup).futureValue
}
