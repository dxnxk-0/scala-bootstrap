package myproject.web.api

import myproject.common.UnexpectedErrorException
import myproject.identity.ApiLogin
import myproject.modules.dummy.api.ApiWelcome

object ApiFunctionsRegistry {
  val Functions = scala.collection.mutable.Set[ApiFunction]()

  def register(function: ApiFunction) = {
    if (Functions.exists(_.name == function.name))
      throw UnexpectedErrorException(s"Class ${function.getClass.getSimpleName} cannot be loaded. A function with name ${function.name} is already registered.")
    else
      Functions.add(function)
  }

  ////////////// Register API functions below
  register(ApiWelcome)
  register(ApiLogin)
}
