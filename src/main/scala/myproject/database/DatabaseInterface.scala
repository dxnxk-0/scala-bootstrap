package myproject.database

import myproject.common.Done

import scala.concurrent.Future

trait DatabaseInterface {
  def close: Future[Done]
  def clean: Future[Done]
  def migrate: Future[Done]
}

