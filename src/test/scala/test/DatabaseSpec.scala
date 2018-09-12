package test

import myproject.common.FutureImplicits._
import myproject.database.ApplicationDatabase.productionDatabaseImpl
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with BeforeAndAfter {

  implicit val db = productionDatabaseImpl

  before {
    InitTestData.init
  }
}

private object InitTestData {
  lazy val init = productionDatabaseImpl.reset.futureValue
}