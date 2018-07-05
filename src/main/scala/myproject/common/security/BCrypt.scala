package myproject.common.security

import java.security.SecureRandom

import myproject.modules.iam.Config
import org.bouncycastle.crypto.generators.OpenBSDBCrypt

trait BCrypt {

  def hashPassword(pass: String): String = {
    val salt = new Array[Byte](16)
    new SecureRandom().nextBytes(salt)
    OpenBSDBCrypt.generate(pass.toCharArray, salt, Config.bcryptWork)
  }
}
