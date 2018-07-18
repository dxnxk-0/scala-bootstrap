package myproject.common

object Validation {

  trait ValidationError

  type FieldValidator[A] = Function[A, ValidatorPartialResult]
  type ValidatorPartialResult = Either[ValidationError, Done]
  type ValidatorResult[A] = Either[ValidationErrorException, A]

  trait Validator[A] {

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
