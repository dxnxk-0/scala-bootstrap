package test

import scala.util.Random

object RandomFactory {
  def randomString(length: Int = 10) = Random.alphanumeric.take(10).mkString("").toLowerCase
}
