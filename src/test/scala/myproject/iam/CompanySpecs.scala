package myproject.iam

import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.iam.Companies.CRUD._
import myproject.iam.Companies.UpdateName
import myproject.iam.Channels.CRUD._
import org.scalatest.DoNotDiscover
import test.DatabaseSpec

@DoNotDiscover
class CompanySpecs extends DatabaseSpec {

  lazy val channel = createChannel("TESTS").futureValue
  lazy val acme = createCompany(channel.id, "Acme").futureValue

  it should "create a company" in {
    acme.name shouldBe "Acme"
  }

  it should "get a company" in {
    getCompany(acme.id).futureValue.name shouldBe "Acme"
  }

  it should "update a company" in {
    updateCompany(acme.id, UpdateName("Death Star")).futureValue
    getCompany(acme.id).futureValue.name shouldBe "Death Star"
  }

  it should "delete a company" in {
    deleteCompany(acme.id).futureValue shouldBe Done
  }
}
