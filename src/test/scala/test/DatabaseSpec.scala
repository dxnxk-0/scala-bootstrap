package test

import myproject.common.FutureImplicits._
import myproject.database.ApplicationDatabase.currentDatabaseImpl
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with BeforeAndAfter {

  implicit val db = currentDatabaseImpl

  before {
    InitTestData.init
  }
}

private object InitTestData {
  lazy val init = currentDatabaseImpl.init.futureValue
}