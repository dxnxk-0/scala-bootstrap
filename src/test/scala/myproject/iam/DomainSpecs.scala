package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Companies.CRUD._
import myproject.iam.Domains.CRUD._
import myproject.iam.Domains.UpdateName
import myproject.iam.Users.CRUD._
import myproject.iam.Users.UserRole
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class DomainSpecs extends DatabaseSpec {

  lazy val domain = createDomain("TEST").futureValue
  lazy val company = createCompany(domain.id, "ACME").futureValue
  lazy val user = createUser("smith", "whatever", company.id, UserRole.User)

  it should "create a domain" in {
    domain.name shouldBe "TEST"
  }

  it should "get a domain" in {
    getDomain(domain.id).futureValue.name shouldBe "TEST"
  }

  it should "update a domain" in {
    updateDomain(domain.id, UpdateName("SPECS")).futureValue
    getDomain(domain.id).futureValue.name shouldBe "SPECS"
  }

  it should "delete a domain" in {
    deleteDomain(domain.id).futureValue shouldBe Done
  }
}
