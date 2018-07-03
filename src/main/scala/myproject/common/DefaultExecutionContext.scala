package myproject.common

import scala.concurrent.ExecutionContext

trait DefaultExecutionContext {
  implicit val ExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
