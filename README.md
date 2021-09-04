# semigroups ![Continuous Integration](https://github.com/typelevel/semigroups/workflows/Continuous%20Integration/badge.svg) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/semigroups_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/semigroups_2.12)

Set of Generic Semigroup Types and Accompanying Instances very useful for abstract programming.

Exposes instances for

- `Dual` inverts the combine operation.
- `Max` exposes a Max that given an `Order` will return the maximum value.
- `Min` exposes a Min that given an `Order` will returh the minimum value.

## Quick Start

To use this project in an existing SBT project with Scala 2.12 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "semigroups" % "<version>"
)
```
