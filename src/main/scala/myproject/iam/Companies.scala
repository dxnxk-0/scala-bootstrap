package myproject.iam

import java.util.UUID

import myproject.common.Runtime.ec
import myproject.database.DB

object Companies {

  case class Company(id: UUID, name: String)

  object CRUD {

    def createCompany(name: String) = DB.insert(Company(UUID.randomUUID(), name))

    def getCompany(id: UUID) = DB.getCompany(id)

    def updateCompany(company: Company) = for {
      _ <- DB.getCompany(company.id)
      saved <- DB.update(company)
    } yield saved

    def deleteCompany(id: UUID) = DB.deleteCompany(id)
  }
}
