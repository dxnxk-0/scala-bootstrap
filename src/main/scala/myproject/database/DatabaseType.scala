package myproject.database

object DatabaseType extends Enumeration {
  type DatabaseType = Value
  val H2, Mysql, Postgresql = Value
}
