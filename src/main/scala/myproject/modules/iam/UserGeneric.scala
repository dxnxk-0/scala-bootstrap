package myproject.modules.iam

import java.util.UUID

sealed trait UserGeneric {
  val id: UUID
  val login: String
}

case class User(id: UUID, login: String, password: Option[String])
  extends UserGeneric

case class Guest() extends UserGeneric {
  val id = UUID fromString "99999999-9999-9999-9999-999999999999"
  val login = "guest"
}
