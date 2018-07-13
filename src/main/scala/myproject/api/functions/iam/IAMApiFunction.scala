package myproject.api.functions.iam

import myproject.api.ApiFunction
import myproject.iam.IAM

trait IAMApiFunction extends ApiFunction {
  val iam = IAM
}
