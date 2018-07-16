package myproject.common

abstract class CustomException(msg: String) extends Exception(msg)
case class ObjectNotFoundException(msg: String) extends CustomException(msg)
case class UnexpectedErrorException(msg: String) extends CustomException(msg)
case class NotImplementedException(msg: String) extends CustomException(msg)
case class AuthenticationNeededException(msg: String) extends CustomException(msg)
case class AccessRefusedException(msg: String) extends CustomException(msg)
case class InvalidContextException(msg: String) extends CustomException(msg)
case class AuthenticationFailedException(msg: String) extends CustomException(msg)
case class TokenExpiredException(msg: String) extends CustomException(msg)
case class IllegalOperationException(msg: String) extends CustomException(msg)
