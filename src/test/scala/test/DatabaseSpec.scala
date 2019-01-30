package test

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.database.ApplicationDatabase.currentDatabaseImpl
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with BeforeAndAfter {

  implicit val db = currentDatabaseImpl

  before {
    Config.dumpLog()
    InitTestData.init
  }
}

private object InitTestData {
  lazy val init = currentDatabaseImpl.migrate.futureValue
}