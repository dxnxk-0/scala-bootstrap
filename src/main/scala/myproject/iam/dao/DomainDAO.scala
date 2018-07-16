package myproject.iam.dao

import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Domains.Domain

trait DomainDAO extends DAO {

  import api._

  protected class DomainsTable(tag: Tag) extends Table[Domain](tag, "DOMAINS") {
    def id = column[UUID]("DOMAIN_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def * = (id, name) <> (Domain.tupled, Domain.unapply)
  }

  protected val domains = TableQuery[DomainsTable]

  def getDomain(id: UUID) = db.run(domains.filter(_.id===id).result) map (_.headOption)
  def insert(domain: Domain) = db.run(domains += domain) map (_ => domain)
  def update(domain: Domain) = db.run(domains.filter(_.id===domain.id).update(domain)) map (_ => domain)
  def deleteDomain(id: UUID) = db.run(domains.filter(_.id===id).delete) map (_ => Done)

}
