package myproject.api.methods

import akka.http.scaladsl.model.StatusCodes
import myproject.common.security.JWT
import org.scalatest.DoNotDiscover

@DoNotDiscover
class WelcomeSpecs extends RPCMethodSpecs("welcome") with JWT {

  it should "say welcome" in {
    val user = getByLoginName("jdoe").futureValue
    val token = createToken(user.login, user.id, None)
    val payload = generateRPCPayload(function, Map())

    postRpc(payload, Some(token)) ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
