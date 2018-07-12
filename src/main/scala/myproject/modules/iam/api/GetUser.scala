package myproject.modules.iam.api

import java.util.UUID

import myproject.api.ApiFunction
import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.database.Database
import myproject.modules.iam.dto.UserDTO
import myproject.modules.iam.{User, UserGeneric}

import scala.concurrent.Future

object GetUser extends ApiFunction with UserDTO with GetUser {
  override val name = "get_user"
  override val description = "Get an existing user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val userId = p.uuid("id")

    for {
      user <- getUser(userId)
    } yield user.serialize
  }
}

trait GetUser extends Database {
  def getUser(userId: UUID): Future[User] = getById(userId)
}
