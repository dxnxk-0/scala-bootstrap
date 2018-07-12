package test

import myproject.api.methods.{WelcomeSpecs, LoginPasswordSpecs}
import org.scalatest.Suites

class TestSuites extends Suites(
  new LoginPasswordSpecs,
  new WelcomeSpecs)
