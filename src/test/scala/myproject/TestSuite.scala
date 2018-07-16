package myproject

import myproject.database.DB
import myproject.iam.{CompanySpecs, DomainSpecs, TokenSpecs, UserSpecs}
import org.scalatest.{BeforeAndAfter, Suites}

class TestSuite extends Suites(
  new DomainSpecs,
  new CompanySpecs,
  new UserSpecs,
  new TokenSpecs) with BeforeAndAfter {

  after(DB.db.close())
}
