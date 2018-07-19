package myproject.web.server

import myproject.common.FutureImplicits._
import myproject.database.DB
import myproject.iam.Users
import myproject.iam.Users.CRUD

object EnvInit {

  def initEnv(): Unit = {
    DB.reset.futureValue
    CRUD.createUser(Users.Root).futureValue
  }
}
