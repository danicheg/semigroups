package io.chrisdavenport.semigroups

final case class Min[A](getMin: A) extends AnyVal

object Min extends MinInstances
