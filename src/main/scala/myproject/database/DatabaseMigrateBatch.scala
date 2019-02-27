package myproject.database

object DatabaseMigrateBatch extends App {
  implicit val db = ApplicationDatabase.fromConfig
  db.migrate()
}
