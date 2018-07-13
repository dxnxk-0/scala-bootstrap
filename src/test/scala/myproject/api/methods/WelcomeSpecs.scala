package myproject.api.methods

import akka.http.scaladsl.model.StatusCodes
import myproject.common.FutureImplicits._
import myproject.common.security.JWT
import myproject.database.DB
import org.scalatest.DoNotDiscover

@DoNotDiscover
class WelcomeSpecs extends RPCMethodSpecs("welcome") {

  it should "say welcome" in {
    val user = DB.getByLoginName("admin").futureValue
    val token = JWT.createToken(user.login, user.id, None)
    val payload = generateRPCPayload(function, Map())

    postRpc(payload, Some(token)) ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
