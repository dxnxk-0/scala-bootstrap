package myproject.web.api

import myproject.common.UnexpectedErrorException
import myproject.modules.dummy.api.ApiWelcome
import myproject.modules.iam.api.{ApiGetUser, ApiLogin, ApiNewUser, ApiUpdateUser}

trait ApiFunctionsRegistry {
  val Functions = scala.collection.mutable.Set[ApiFunction]()

  def register(function: ApiFunction) = {
    if (Functions.exists(_.name == function.name))
      throw UnexpectedErrorException(s"Class ${function.getClass.getSimpleName} cannot be loaded. A function with name ${function.name} is already registered.")
    else
      Functions.add(function)
  }
}

object ApiFunctionsRegistry extends ApiFunctionsRegistry {

  ////////////// Register API functions below
  register(ApiWelcome)
  register(ApiLogin)
  register(ApiNewUser)
  register(ApiUpdateUser)
  register(ApiGetUser)
}
