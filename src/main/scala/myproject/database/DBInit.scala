package myproject.database

import java.util.UUID

import myproject.common.security.BCrypt
import myproject.modules.iam.User

trait DBInit extends App with Database with BCrypt {

  import api._

  val setup = DBIO.seq(
    users.schema.drop.asTry,
    users.schema.create,
    users ++= Seq(
      User(UUID.fromString("e498dd4e-2758-4e01-80f6-392f4f43606b"), "admin", hashPassword("Kondor_123"))))
}
