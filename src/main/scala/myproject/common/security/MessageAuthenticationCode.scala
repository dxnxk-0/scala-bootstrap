package myproject.common.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MessageAuthenticationCode {

  object Algorithms extends Enumeration {
    val Sha256 = Value("HmacSHA256")
    val Sha512 = Value("HmacSHA512")
  }

  def sign(data: Array[Byte], secret: Array[Byte], algorithm: Algorithms.Value = Algorithms.Sha256): Array[Byte] = {
    val macSecret = new SecretKeySpec(secret, algorithm.toString)
    val mac = Mac.getInstance(algorithm.toString)
    mac.init(macSecret)
    mac.doFinal(data)
  }
}
