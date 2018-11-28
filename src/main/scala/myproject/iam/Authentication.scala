package myproject.iam

import myproject.common.{AuthenticationFailedException, Done}
import myproject.iam.Users.User
import org.bouncycastle.crypto.generators.OpenBSDBCrypt

import scala.util.{Failure, Success}

object Authentication {

  def loginPassword(user: User, candidate: String) = {
    if(checkPassword(candidate, user.password))
      Success(Done)
    else
      Failure(AuthenticationFailedException("Bad user or password"))
  }

  private def checkPassword(candidate: String, hashedPassword: String) = OpenBSDBCrypt.checkPassword(hashedPassword, candidate.toCharArray)
}
