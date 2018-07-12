package myproject.api

import myproject.common.UnexpectedErrorException
import myproject.modules.dummy.api.Welcome
import myproject.modules.iam.api.{GetUser, LoginPassword, NewUser, UpdateUser}

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
  register(Welcome)
  register(LoginPassword)
  register(NewUser)
  register(UpdateUser)
  register(GetUser)
}
