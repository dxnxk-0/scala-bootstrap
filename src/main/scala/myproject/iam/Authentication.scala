package myproject.iam

import myproject.common.Done
import myproject.iam.Users.User
import org.bouncycastle.crypto.generators.OpenBSDBCrypt

object Authentication {

  def loginPassword(user: User, candidate: String): Either[String, Done] =
    if(checkPassword(candidate, user.hashedPassword)) Right(Done) else Left("Bad user or password")

  private def checkPassword(candidate: String, hashedPassword: String): Boolean =
    OpenBSDBCrypt.checkPassword(hashedPassword, candidate.toCharArray)
}
