package myproject.modules.dummy.api

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.modules.iam.User
import myproject.web.api.ApiFunction

import scala.concurrent.Future

case object ApiWelcome extends ApiFunction {
  override val name = "welcome"
  override val description = "A bit of kindness in this cold world"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: User, auditData: AuditData) = Future {
    s"Welcome ${effectiveUser.login} !"
  }

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = Future {
    s"Welcome guest !"
  }
}
