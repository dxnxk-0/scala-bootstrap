package myproject.common

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.database.ApplicationDatabase

object EnvInitBatch extends App {
  implicit val db = ApplicationDatabase.currentDatabaseImpl

  db.reset.futureValue

  if(Config.datainit.enabled)
    DataInitializer.Instance.initialize.futureValue
}
