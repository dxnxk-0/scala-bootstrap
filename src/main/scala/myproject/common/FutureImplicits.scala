package myproject.common

import myproject.common.Runtime.ec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object FutureImplicits {

  val timeout: Duration = Duration.Inf

  implicit class BlockingFuture[A](f: Future[A]) {
    def futureValue: A = Await.result(f, timeout)
  }

  implicit class TryToFuture[A](monad: Try[A]) {
    def toFuture: Future[A] = Future.fromTry(monad)
  }

  implicit class EitherToFuture[A](either: Either[CustomException, A]) {
    def toFuture: Future[A] = either match {
      case Left(e) => Future.failed(e)
      case Right(res) => Future.successful(res)
    }
  }

  implicit class FutureOptionToFuture[A](future: Future[Option[A]]) {
    def getOrFail(exception: Throwable): Future[A] = future flatMap {
      case Some(v) => Future.successful(v)
      case None => Future.failed(exception)
    }

    def getOrFailNotFound(msg: String): Future[A] = future flatMap {
      case Some(v) => Future.successful(v)
      case None => Future.failed(ObjectNotFoundException(msg))
    }
  }
}
