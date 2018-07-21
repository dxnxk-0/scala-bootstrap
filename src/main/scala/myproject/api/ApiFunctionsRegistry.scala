package myproject.api

import myproject.api.functions._
import myproject.audit.Audit.AuditData
import myproject.common.UnexpectedErrorException
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper

import scala.concurrent.Future

object ApiFunctionsRegistry {

  class ApiHelp extends ApiFunction {

    override val name = "help"
    override val secured = false
    override val description = "An overview of the functions available in API"

    import ApiParameters._

    import scala.reflect.runtime.universe._

    def getApiFunctionParameters(function: ApiFunction) = {
      val typeMirror = runtimeMirror(function.getClass.getClassLoader)
      val instanceMirror = typeMirror.reflect(function)
      val members = instanceMirror.symbol.typeSignature.members

      val parameters = members.filter(_.typeSignature <:< typeOf[ApiParameter])

      parameters.map { symbol ⇒
        val paramMirror = instanceMirror.reflectField(symbol.asTerm)

        paramMirror.get match {
          case param: ApiParameter ⇒ param
        }
      }.toList
    }

    override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {

      def buildDoc(fn: ApiFunction) = {
        val params = getApiFunctionParameters(fn).map { param =>
          Map(
            "name" -> param.name,
            "type" -> param.`type`.toString,
            "description" -> param.description,
            "nullable" -> param.nullable,
            "optional" -> param.optional,
            "path" -> param.path.map(_.mkString("/")).getOrElse(None),
            "values" -> param.withEnum.map{
              case e if param.`type`==ApiParameterType.EnumString => e.values.mkString(",")
              case e if param.`type`==ApiParameterType.EnumId => e.values.map(_.id).mkString(",")
              case _ => None
            })
        }

        Map(
          "name" -> fn.name,
          "description" -> fn.description,
          "secured" -> fn.secured,
          "parameters" -> params)
      }

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
  register(new Welcome)
  register(new ApiHelp)
  register(new NewPlatformUser)
  register(new NewChannelUser)
  register(new NewGroupUser)
  register(new NewSimpleUser)
  register(new UpdatePlatformUser)
  register(new UpdateChannelUser)
  register(new UpdateGroupUser)
  register(new UpdateSimpleUser)
  register(new LoginPassword)
  register(new GetUser)
  register(new ApiPlayGround)
}
