package myproject.database

import myproject.Config
import myproject.common.FutureImplicits._

object DatabaseInitBatch extends App {
  implicit val db = ApplicationDatabase.currentDatabaseImpl

  db.init.futureValue

  if(Config.datainit.enabled)
    DataLoader.instanceFromConfig.load.futureValue
}
