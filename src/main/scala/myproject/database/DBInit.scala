package myproject.database

import java.util.UUID

import myproject.common.security.BCrypt

trait DBInit extends App with Database with BCrypt {

  import api._

  val setup = DBIO.seq(
    users.schema.drop.asTry,
    users.schema.create,
    users ++= Seq((UUID.randomUUID, "jdoe", hashPassword("Kondor_123"))))
}
