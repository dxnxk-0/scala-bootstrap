package myproject.api

import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.{InvalidContextException, InvalidParametersException, NotImplementedException, Runtime}
import myproject.iam.Users.User

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class ApiSummaryDoc(description: String, `return`: String)

trait ApiFunction {
  val name: String
  val doc: ApiSummaryDoc
  val secured: Boolean = true

  protected implicit val ec = Runtime.ec

  protected def checkParamAndProcess(extractors: Try[Any]*)(op: => Future[Any]): Future[Any] = {
    val failedParams =
      extractors.foldLeft(Nil: List[String]){ case (errors, attempt) =>
        attempt match {
          case Success(_) => errors
          case Failure(e) => e.getMessage :: errors
        }
      }

    if(failedParams.isEmpty) op
    else Future.failed(InvalidParametersException("invalid parameters", errors = failedParams))
  }

  def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData): Future[Any] = {
    if(!secured)
      Future.failed(InvalidContextException("This function does not need a valid authentication and should probably be called using the simple signature"))

    Future.failed(NotImplementedException("This function is not implemented"))
  }

  def process(implicit p: ReifiedDataWrapper, auditData: AuditData): Future[Any] = {

    if(secured)
      Future.failed(InvalidContextException("This function needs a valid authentication and should probably be called using the secured signature"))

    Future.failed(NotImplementedException("This function is not implemented"))
  }
}