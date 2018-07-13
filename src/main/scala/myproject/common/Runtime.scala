package myproject.common

import scala.concurrent.ExecutionContext

object Runtime {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
