package myproject

import myproject.database.DB
import myproject.iam.{ChannelSpecs, GroupSpecs, TokenSpecs, UserSpecs}
import org.scalatest.{BeforeAndAfter, Suites}

class TestSuite extends Suites(
  new ChannelSpecs,
  new GroupSpecs,
  new UserSpecs,
  new TokenSpecs) with BeforeAndAfter {

  after(DB.db.close())
}
