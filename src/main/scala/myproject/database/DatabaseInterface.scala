package myproject.database

import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future

trait DatabaseInterface {
  def close(): Unit
  def clean(): Unit
  def migrate(): Unit
  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R]
}
