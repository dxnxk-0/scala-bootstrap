package myproject

import myproject.database.DB
import myproject.iam.{CompanySpecs, TokenSpecs, UserSpecs}
import org.scalatest.{BeforeAndAfter, Suites}

class TestSuite extends Suites(
  new CompanySpecs,
  new UserSpecs,
  new TokenSpecs) with BeforeAndAfter {

  after(DB.db.close())
}
