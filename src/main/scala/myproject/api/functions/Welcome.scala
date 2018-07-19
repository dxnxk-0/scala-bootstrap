package myproject.api.functions

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.User

import scala.concurrent.Future

case object Welcome extends ApiFunction {
  override val name = "welcome"
  override val description = "A bit of kindness in this cold world"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = Future {
    s"Welcome ${user.login} !"
  }
}
