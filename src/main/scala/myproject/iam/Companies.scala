package myproject.iam

import java.util.UUID

import myproject.common.Runtime.ec
import myproject.database.DB

import scala.concurrent.Future

object Companies {

  case class Company(id: UUID, name: String, domainId: UUID)

  sealed trait CompanyUpdate
  case class UpdateName(name: String) extends CompanyUpdate

  def updateCompany(company: Company, updates: List[CompanyUpdate]) = updates.foldLeft(company) { case (updated, upd) =>
    upd match {
      case UpdateName(name) => updated.copy(name = name)
    }
  }

  def newCompany(domainId: UUID, name: String) = Company(UUID.randomUUID(), name, domainId)

  object CRUD {
    def createCompany(domainId:UUID, name: String) = DB.insert(newCompany(domainId, name))
    def getCompany(id: UUID) = DB.getCompany(id)
    def updateCompany(companyId: UUID, updates: List[CompanyUpdate]) =
      DB.getCompany(companyId) map (Companies.updateCompany(_, updates)) flatMap DB.update
    def updateCompany(companyId: UUID, update: CompanyUpdate): Future[Company] = updateCompany(companyId, List(update))
    def deleteCompany(id: UUID) = DB.deleteCompany(id)
  }
}
