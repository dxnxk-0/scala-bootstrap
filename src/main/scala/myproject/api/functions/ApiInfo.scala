package myproject.api.functions

import buildmeta.BuildInfo
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData

import scala.concurrent.Future

class ApiInfo extends ApiFunction {
  override val name = "api_info"
  override val doc = ApiSummaryDoc("get the current API metadata", "an object containing the api metadata")
  override val secured = false

  override def process(implicit p: OpaqueData.ReifiedDataWrapper) = {
    Future.successful(Map(
      "name" -> BuildInfo.name,
      "api_version" -> BuildInfo.version,
      "build_number" -> BuildInfo.buildInfoBuildNumber,
      "build_time" -> BuildInfo.builtAtString)
    )
  }
}
