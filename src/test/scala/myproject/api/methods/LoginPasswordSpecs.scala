package myproject.api.methods

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.DoNotDiscover

@DoNotDiscover
class LoginPasswordSpecs extends RPCMethodSpecs("login", needAuthentication = false) {

  it should "authenticate the admin" in {

    val payload = generateRPCPayload(function, Map("login" -> "admin", "password" -> "Kondor_123"))

    postRpc(payload) ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
