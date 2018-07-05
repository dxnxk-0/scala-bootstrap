package test

import myproject.common.FutureImplicits
import myproject.database.DBInit
import org.scalatest.BeforeAndAfter

trait DatabaseSpec extends UnitSpec with FutureImplicits with BeforeAndAfter {

  before {
    InitTestData.init
  }
}

private object InitTestData extends DBInit with FutureImplicits {
  lazy val init = db.run(setup).futureValue
}