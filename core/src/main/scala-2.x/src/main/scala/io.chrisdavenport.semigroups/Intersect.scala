package io.chrisdavenport.semigroups

final case class Intersect[A](getIntersect: Set[A]) extends AnyVal

object Intersect extends IntersectInstances
