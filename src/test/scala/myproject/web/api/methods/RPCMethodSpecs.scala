package myproject.web.api.methods

import akka.http.scaladsl.model.StatusCodes
import myproject.web.api.RPCApiTestHelper

abstract class RPCMethodSpecs(val function: String, val needAuthentication: Boolean = true) extends RPCApiTestHelper {

  val functionName = "function"

  behavior of functionName

  it should "forbid or allow unauthenticated access" in {
    postRpc(generateRPCPayload(function, Map())) ~> check {
      if (needAuthentication)
        status shouldBe StatusCodes.Unauthorized
      else
        status shouldNot be(StatusCodes.Unauthorized)
    }
  }
}
