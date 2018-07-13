package myproject.iam

import myproject.iam.Users.User
import org.bouncycastle.crypto.generators.OpenBSDBCrypt

object Authentication {

  type AccessGranted = Unit

  def loginPassword(user: User, candidate: String): Either[String, AccessGranted] =
    if(checkPassword(candidate, user.hashedPassword)) Right(Unit) else Left("Bad user or password")

  private def checkPassword(candidate: String, hashedPassword: String): Boolean =
    OpenBSDBCrypt.checkPassword(hashedPassword, candidate.toCharArray)
}
