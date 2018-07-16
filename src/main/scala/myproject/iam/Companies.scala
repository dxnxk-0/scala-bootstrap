package myproject.iam

import java.util.UUID

import myproject.common.Runtime.ec
import myproject.database.DB

import scala.concurrent.Future

object Companies {

  case class Company(id: UUID, name: String)

  sealed trait CompanyUpdate
  case class UpdateName(name: String) extends CompanyUpdate

  def updateCompany(company: Company, updates: List[CompanyUpdate]) = updates.foldLeft(company) { case (updated, upd) =>
    upd match {
      case UpdateName(name) => updated.copy(name = name)
    }
  }

  object CRUD {

    def createCompany(name: String) = DB.insert(Company(UUID.randomUUID(), name))

    def getCompany(id: UUID) = DB.getCompany(id)

    def updateCompany(companyId: UUID, updates: List[CompanyUpdate]): Future[Company] = for {
      updated <- DB.getCompany(companyId) map (Companies.updateCompany(_, updates))
      saved <- DB.update(updated)
    } yield saved

    def updateCompany(companyId: UUID, update: CompanyUpdate): Future[Company] = updateCompany(companyId, List(update))

    def deleteCompany(id: UUID) = DB.deleteCompany(id)
  }
}
