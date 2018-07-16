package myproject.api.functions

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.{Guest, User, UserGeneric}

import scala.concurrent.Future

case object Welcome extends ApiFunction with Welcome {
  override val name = "welcome"
  override val description = "A bit of kindness in this cold world"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = Future {
    sayWelcome(effectiveUser)
  }

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = Future {
    sayWelcome(Guest())
  }
}

trait Welcome {

  def sayWelcome(user: UserGeneric): Future[String] = Future.successful {
    user match {
      case u: Guest => s"Welcome guest !"
      case u: User => s"Welcome ${u.login} !"
    }
  }
}
