package myproject.api

import myproject.api.functions._
import myproject.common.UnexpectedErrorException
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.database.ApplicationDatabase
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Users.User

import scala.concurrent.Future

object ApiFunctionsRegistry {

  implicit val db = ApplicationDatabase.currentDatabaseImpl
  implicit val iamAuthz = (u: User) => new DefaultIAMAccessChecker(u)

  class ApiHelp extends ApiFunction {

    override val name = "help"
    override val secured = false
    override val doc = ApiSummaryDoc(
      description = "An overview of the functions available in API",
      `return` = "a list of objects containing function description data")

    override def process(implicit p: ReifiedDataWrapper) = {

      def buildDoc(fn: ApiFunction) = {
        Map(
          "name" -> fn.name,
          "description" -> fn.doc.description,
          "return" -> fn.doc.`return`,
          "secured" -> fn.secured)
      }

      Future.successful {
        Map("jsonrpc" -> "see: https://www.jsonrpc.org/specification") ++ Map(
          "functions" -> ApiFunctionsRegistry.Functions.map { fn =>
            buildDoc(fn)
          }
        )
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
  register(new ApiInfo)
  register(new Welcome)
  register(new ApiHelp)
  register(new GetPlatformUsers)
  register(new CreateChannel)
  register(new GetChannelUsers)
  register(new GetChannels)
  register(new GetChannel)
  register(new UpdateChannel)
  register(new DeleteChannel)
  register(new CreateGroup)
  register(new GetGroups)
  register(new GetGroup)
  register(new UpdateGroup)
  register(new GetGroupUsers)
  register(new GetGroupChildren)
  register(new DeleteGroup)
  register(new CreatePlatformUser)
  register(new CreateChannelUser)
  register(new CreateGroupUser)
  register(new CreateSimpleUser)
  register(new UpdateUser)
  register(new DeleteUser)
  register(new LoginPassword)
  register(new GetUser)
}
