package myproject.database

import myproject.common.FutureImplicits._

object DataLoaderBatch extends App {
  implicit val db = ApplicationDatabase.currentDatabaseImpl
  DataLoader.instanceFromConfig.load.futureValue
}
