package myproject.identity

import java.util.UUID

sealed trait User {
  val id: UUID
  val login: String
}

case class ApiUser(id: UUID, login: String)
  extends User

case class Guest()
  extends User {
  val id = User.guestId
  val login = "guest"
}

object User {
  val guestId = UUID fromString "99999999-9999-9999-9999-999999999999"
}
