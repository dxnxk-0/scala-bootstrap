package myproject.api.functions.iam

import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers.UserSerialization

case object LoginPassword extends IAMApiFunction {

  override val name = "login"
  override val description = "Retrieve a JWT token on a successful authentication"
  override val secured = false

  override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {
    val login = p.string("login")
    val candidate = p.string("password")

    iam.loginPassword(login, candidate) map {
      case (user, token) => Map("whoami" -> user.serialize, "token" -> token)
    }
  }
}
