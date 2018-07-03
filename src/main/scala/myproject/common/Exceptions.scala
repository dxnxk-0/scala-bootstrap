package myproject.common

case class ObjectNotFoundException(msg: String) extends Throwable
case class UnexpectedErrorException(msg: String) extends Throwable
case class NotImplementedException(msg: String) extends Throwable
case class AuthenticationNeededException(msg: String) extends Throwable
case class AccessRefusedException(msg: String) extends Throwable
case class InvalidContextException(msg: String) extends Throwable
case class AuthenticationFailedException(msg: String) extends Throwable
