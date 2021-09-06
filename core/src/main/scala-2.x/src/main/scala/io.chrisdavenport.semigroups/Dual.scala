package io.chrisdavenport.semigroups

final case class Dual[A](getDual: A) extends AnyVal

object Dual extends DualInstances