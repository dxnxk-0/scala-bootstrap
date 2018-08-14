package myproject

import myproject.database.DB
import myproject.iam._
import org.scalatest.{BeforeAndAfter, Suites}

class TestSuite extends Suites(
  new ChannelSpecs,
  new GroupSpecs,
  new UserSpecs,
  new TokenSpecs,
  new OrganizationSpecs) with BeforeAndAfter {

  after(DB.db.close())
}
