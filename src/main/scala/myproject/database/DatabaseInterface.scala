package myproject.database

import myproject.common.Done

import scala.concurrent.Future

trait DatabaseInterface {
  def reset: Future[Done]
  def close: Future[Done]
}

