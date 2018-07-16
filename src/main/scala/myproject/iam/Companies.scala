package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
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
    private def getCompanyFromDb(id: UUID): Future[Company] = DB.getCompany(id).getOrFail(ObjectNotFoundException(s"company with id $id was not found"))
    def createCompany(domainId:UUID, name: String) = DB.insert(newCompany(domainId, name))
    def getCompany(id: UUID) = getCompanyFromDb(id)
    def updateCompany(id: UUID, updates: List[CompanyUpdate]) = getCompanyFromDb(id) map (Companies.updateCompany(_, updates)) flatMap DB.update
    def updateCompany(id: UUID, update: CompanyUpdate): Future[Company] = updateCompany(id, List(update))
    def deleteCompany(id: UUID) = DB.deleteCompany(id)
  }
}
