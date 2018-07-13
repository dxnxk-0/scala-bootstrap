package myproject.common

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FutureImplicits {
  val timeout: Duration = Duration.Inf

  implicit class BlockingFuture[A](f: Future[A]) {
    def futureValue: A = Await.result(f, timeout)
  }
}
