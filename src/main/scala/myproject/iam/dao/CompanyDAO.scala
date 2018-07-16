package myproject.iam.dao

import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.DAO
import myproject.iam.Companies.Company

trait CompanyDAO extends DAO { self: DomainDAO =>

  import api._

  protected class CompaniesTable(tag: Tag) extends Table[Company](tag, "COMPANIES") {
    def id = column[UUID]("COMPANY_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def domainId = column[UUID]("DOMAIN_ID", O.SqlType("UUID"))
    def domain = foreignKey("DOMAIN_FK", domainId, domains)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, domainId) <> (Company.tupled, Company.unapply)
  }

  protected val companies = TableQuery[CompaniesTable]

  def getCompany(id: UUID) = db.run(companies.filter(_.id===id).result) map {
    case Nil => throw ObjectNotFoundException(s"company with id $id was not found")
    case c +: _ => c
  }

  def insert(company: Company) = db.run(companies += company) map (_ => company)
  def update(company: Company) = db.run(companies.filter(_.id===company.id).update(company)) map (_ => company)
  def deleteCompany(id: UUID) = db.run(companies.filter(_.id===id).delete) map (_ => Done)
}
