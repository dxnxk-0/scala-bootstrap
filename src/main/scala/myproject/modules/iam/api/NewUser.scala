package myproject.modules.iam.api

import java.util.UUID

import myproject.api.ApiFunction
import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.database.Database
import myproject.modules.iam.UserGeneric
import myproject.modules.iam.domain.UserFunctions
import myproject.modules.iam.dto.UserDTO

import scala.concurrent.Future

case object NewUser extends ApiFunction with Database with UserDTO with UserFunctions {
  override val name = "new_user"
  override val description = "Create a new user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val login = p.string("login")
    val password = p.string("password")

    for {
      user  <- Future(newUser(UUID.randomUUID(),login, password))
      saved <- insert(user)
    } yield saved.serialize
  }
}
