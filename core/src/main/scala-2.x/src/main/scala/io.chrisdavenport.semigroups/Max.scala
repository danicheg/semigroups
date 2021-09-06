package io.chrisdavenport.semigroups

final case class Max[A](getMax: A) extends AnyVal

object Max extends MaxInstances
