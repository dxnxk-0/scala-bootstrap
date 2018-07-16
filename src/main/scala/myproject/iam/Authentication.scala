package myproject.iam

import myproject.common.{AuthenticationFailedException, Done}
import myproject.iam.Users.User
import org.bouncycastle.crypto.generators.OpenBSDBCrypt

object Authentication {

  def loginPassword(user: User, candidate: String): Either[AuthenticationFailedException, Done] =
    if(checkPassword(candidate, user.hashedPassword)) Right(Done) else Left(AuthenticationFailedException("Bad user or password"))

  private def checkPassword(candidate: String, hashedPassword: String): Boolean =
    OpenBSDBCrypt.checkPassword(hashedPassword, candidate.toCharArray)
}
