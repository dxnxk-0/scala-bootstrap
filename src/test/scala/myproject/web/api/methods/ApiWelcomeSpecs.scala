package myproject.web.api.methods

import akka.http.scaladsl.model.StatusCodes

class ApiWelcomeSpecs extends RPCMethodSpecs("welcome", needAuthentication = false) {

  val payload = generateRPCPayload(functionName, Map())

  it should "say welcome" in {
    postRpc(payload) ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
