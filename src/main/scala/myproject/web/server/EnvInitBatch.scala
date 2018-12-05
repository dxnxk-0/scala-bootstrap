package myproject.web.server

import myproject.common.FutureImplicits._
import myproject.database.ApplicationDatabase
import myproject.web.server.EnvInit.initEnv

object EnvInitBatch extends App {
  implicit val db = ApplicationDatabase.currentDatabaseImpl

  initEnv.futureValue
}
