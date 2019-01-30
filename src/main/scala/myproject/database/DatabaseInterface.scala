package myproject.database

trait DatabaseInterface {
  def close(): Unit
  def clean(): Unit
  def migrate(): Unit
}

