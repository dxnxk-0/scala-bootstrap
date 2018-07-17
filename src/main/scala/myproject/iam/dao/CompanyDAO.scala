package myproject.iam.dao

import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Companies.Company

trait CompanyDAO extends DAO { self: ChannelDAO =>

  import api._

  protected class CompaniesTable(tag: Tag) extends Table[Company](tag, "COMPANIES") {
    def id = column[UUID]("COMPANY_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def channelId = column[UUID]("DOMAIN_ID", O.SqlType("UUID"))
    def channel = foreignKey("DOMAIN_FK", channelId, channels)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, name, channelId) <> (Company.tupled, Company.unapply)
  }

  protected val companies = TableQuery[CompaniesTable]

  def getCompany(id: UUID) = db.run(companies.filter(_.id===id).result) map (_.headOption)
  def insert(company: Company) = db.run(companies += company) map (_ => company)
  def update(company: Company) = db.run(companies.filter(_.id===company.id).update(company)) map (_ => company)
  def deleteCompany(id: UUID) = db.run(companies.filter(_.id===id).delete) map (_ => Done)
}
