package test

import myproject.common.FutureImplicits._
import myproject.database.DB
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with BeforeAndAfter {

  before {
    InitTestData.init
  }
}

private object InitTestData {
  lazy val init = DB.reset.futureValue
}