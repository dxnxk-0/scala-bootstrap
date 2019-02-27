package myproject.common

import java.util.UUID

import scala.reflect.ClassTag

object OptionImplicits {
  implicit class OptionExtension[A](opt: Option[A])(implicit ct: ClassTag[A]) {
    def getOrNotFound(id: UUID) = opt.getOrElse(throw ObjectNotFoundException(s"${ct.getClass.getSimpleName.toLowerCase} with $id was not found"))
  }
}
