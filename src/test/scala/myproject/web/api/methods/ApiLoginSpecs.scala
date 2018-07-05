package myproject.web.api.methods

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.DoNotDiscover

@DoNotDiscover
class ApiLoginSpecs extends RPCMethodSpecs("login", needAuthentication = false) {

  it should "authenticate John Doe" in {

    val payload = generateRPCPayload(function, Map("login" -> "jdoe", "password" -> "Kondor_123"))

    postRpc(payload) ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
