package myproject.api

import myproject.api.functions._
import myproject.audit.Audit.AuditData
import myproject.common.UnexpectedErrorException
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper

import scala.concurrent.Future

object ApiFunctionsRegistry {

  object ApiHelp extends ApiFunction {

    override val name = "help"
    override val secured = false
    override val description = "An overview of the functions available in API"

    override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {

      def buildDoc(fn: ApiFunction) = Map(
        "name" -> fn.name,
        "description" -> fn.description,
        "secured" -> fn.secured)

      Future.successful {
        ApiFunctionsRegistry.Functions.map { fn =>
          buildDoc(fn)
        }
      }
    }
  }

  private val Functions = scala.collection.mutable.Set[ApiFunction]()

  private def register(function: ApiFunction) = {
    if (Functions.exists(_.name == function.name))
      throw UnexpectedErrorException(s"Class ${function.getClass.getSimpleName} cannot be loaded. A function with name ${function.name} is already registered.")
    else
      Functions.add(function)
  }

  def find(functionName: String): Option[ApiFunction] = Functions.find(_.name.trim == functionName)

  ////////////// Register API functions below
  register(Welcome)
  register(ApiHelp)
  register(NewPlatformUser)
  register(NewChannelUser)
  register(NewGroupUser)
  register(NewSimpleUser)
  register(UpdateUser)
  register(LoginPassword)
  register(GetUser)
}
