package myproject.modules.iam.api

import java.util.UUID

import myproject.audit.AuditData
import myproject.common.security.JWT
import myproject.common.serialization.ReifiedDataWrapper
import myproject.web.api.ApiFunction

import scala.concurrent.Future

case object ApiLogin extends ApiFunction {
  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = Future {
    val login = p.string("login")
    val password = p.string("password")

    JWT.createToken(login, UUID.randomUUID(), None)
  }
}
