package myproject.api.functions

import myproject.api
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData

import scala.concurrent.Future

class ApiInfo extends ApiFunction {
  override val name = "api_info"
  override val doc = ApiSummaryDoc("get the current API metadata", "an object containing the api metadata")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, auditData: Audit.AuditData) = {
    Future.successful(Map("api_version" -> api.ApiVersion))
  }
}
