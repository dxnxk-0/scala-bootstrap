package test

import myproject.Config
import myproject.database.{ApplicationDatabase, DatabaseInterface}
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with BeforeAndAfter {

  implicit val db = ApplicationDatabase.fromConfig

  before {
    Config.dumpLog()
    InitTestData.initOnce(db)
  }
}

private object InitTestData {
  var initialized = false
  def initOnce(db: DatabaseInterface) = {
    if(initialized)
      Unit
    else {
      db.migrate()
      initialized = true
    }
  }
}
