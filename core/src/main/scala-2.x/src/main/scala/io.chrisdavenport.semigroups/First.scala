package io.chrisdavenport.semigroups

final case class First[A](getFirst: A) extends AnyVal

object First extends FirstInstances
