package myproject.database

import myproject.common.Done

import scala.concurrent.Future

trait DatabaseInterface {
  def init: Future[Done]
  def close: Future[Done]
}

