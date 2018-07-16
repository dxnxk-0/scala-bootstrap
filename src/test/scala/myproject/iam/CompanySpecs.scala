package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Companies.CRUD._
import test.DatabaseSpec

class CompanySpecs extends DatabaseSpec {

  lazy val acme = createCompany("Acme").futureValue

  it should "create a company" in {
    acme.name shouldBe "Acme"
  }

  it should "get a company" in {
    getCompany(acme.id).futureValue.name shouldBe "Acme"
  }

  it should "update a company" in {
    updateCompany(acme.copy(name = "Death Star")).futureValue
    getCompany(acme.id).futureValue.name shouldBe "Death Star"
  }

  it should "delete a company" in {
    deleteCompany(acme.id).futureValue shouldBe Done
  }
}
