package myproject

import myproject.database.ApplicationDatabase
import myproject.iam._
import org.scalatest.{BeforeAndAfter, Suites}

class TestSuite extends Suites(
  new ChannelSpecs,
  new GroupSpecs,
  new UserSpecs,
  new TokenSpecs,
  new OrganizationSpecs,
  new AuthorizationSpecs) with BeforeAndAfter {

  after(ApplicationDatabase.currentDatabaseImpl.close)
}
