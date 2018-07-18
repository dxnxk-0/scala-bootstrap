package myproject.common

import myproject.common.Validation.{ValidationError, Validator}

object Updater {

  type FieldUpdater[A] = Function[A, UpdaterPartialResult[A]]
  type UpdaterPartialResult[A] = Either[ValidationError, A]
  type UpdaterResult[A] = Either[ValidationErrorException, A]


  abstract class Updater[A](source: A, target: A) {

    val updaters: List[FieldUpdater[A]]
    val validator: Validator[A]

    final def NOK(error: ValidationError): UpdaterPartialResult[A] = Left(error)
    final def OK(item: A): UpdaterPartialResult[A] = Right(item)

    def update: UpdaterResult[A] = {
      updaters.foldLeft((List[ValidationError](), source)) { case ((errors, updated), op) =>
        op(updated) match {
          case Right(o) => (errors, o)
          case Left(e) => (e :: errors, updated)
        }
      }
    } match {
      case (Nil, o) => validator.validate(o)
      case (errors, _) => Left(ValidationErrorException("Update is not valid", errors))
    }
  }
}
