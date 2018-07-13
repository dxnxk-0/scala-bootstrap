package test

import myproject.api.methods.{LoginPasswordSpecs, WelcomeSpecs}
import org.scalatest.Suites

class TestSuites extends Suites(
  new LoginPasswordSpecs,
  new WelcomeSpecs)
