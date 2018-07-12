package myproject.api

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.common.{DefaultExecutionContext, InvalidContextException, NotImplementedException}
import myproject.modules.iam.UserGeneric

import scala.concurrent.Future
import scala.language.implicitConversions

trait ApiFunction extends DefaultExecutionContext {
  val name: String
  val description: String
  val secured: Boolean = true

  def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData): Future[Any] = {
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