package myproject.iam

import java.util.UUID

object Users {
  sealed trait UserGeneric {
    val id: UUID
    val login: String
  }

  case class User(id: UUID, login: String, hashedPassword: String)
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val id = UUID fromString "99999999-9999-9999-9999-999999999999"
    val login = "guest"
  }
}