package myproject.common

object Validation {

  trait ValidationError

  type FieldValidator[A] = Function[A, ValidatorPartialResult]
  type ValidatorPartialResult = Either[ValidationError, Done]
  type ValidatorResult[A] = Either[ValidationErrorException, A]

  implicit class ValidatorResultExtensions[A](v: ValidatorResult[A]) {
    def ifValid[B](op: A => B): B = v match {
      case Right(value) => op(value)
      case Left(e) => throw e
    }
  }

  trait Validator[A] {

    private val alphaNumericSequence = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet ++ Set('_', '-')
    def isAlphaNumericString(s: String) = s.forall(alphaNumericSequence.contains)

    val validators: List[FieldValidator[A]]

    final def NOK(error: ValidationError): ValidatorPartialResult = Left(error)
    final val OK: ValidatorPartialResult = Right(Done)

    def validate(item: A): ValidatorResult[A] = {
      val results = validators map (_(item))
      results filter (_.isLeft) match {
        case Nil => Right(item)
        case errors => Left(ValidationErrorException(s"validation error ($item)", errors.map(_.left.get)))
      }
    }
  }
}
