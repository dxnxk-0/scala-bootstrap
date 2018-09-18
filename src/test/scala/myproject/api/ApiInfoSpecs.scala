package myproject.api

import buildmeta.BuildInfo
import myproject.api.functions.ApiInfo
import myproject.common.FutureImplicits._
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import test.UnitSpec

class ApiInfoSpecs extends UnitSpec {
  it should "return the api info" in {
    new ApiInfo().process(new ReifiedDataWrapper(null)).futureValue.get("version") shouldBe Some(BuildInfo.version)
  }
}
