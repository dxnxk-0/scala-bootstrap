package myproject.database

import myproject.common.FutureImplicits._

object DatabaseMigrateBatch extends App {
  implicit val db = ApplicationDatabase.currentDatabaseImpl
  db.migrate.futureValue
}
