package myproject.web.api

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.common.serialization.ReifiedDataWrapper._

import scala.concurrent.Future

/**
  * The API help functions.
  */
trait ApiHelp {

  val Functions: Iterable[ApiFunction]

  trait ApiHelpFunction extends ApiFunction

  case object ApiHelp extends ApiHelpFunction {
    override val name = "help"
    override val secured = false
    override val description = "An overview of the functions available in API"

    override def process(implicit p: ReifiedDataWrapper, auditData: AuditData) = {

      def buildDoc(fn: ApiFunction) = Map(
        "name" -> fn.name,
        "description" -> fn.description,
        "secured" -> fn.secured)

      Future.successful {
        Functions map { fn =>
          buildDoc(fn)
        }
      }
    }
  }
}
