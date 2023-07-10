val toolkitV             = "0.1.3"
val skunkV               = "0.6.0-5-9b3691f-SNAPSHOT"
val testcontainersV      = "1.17.6"
val testcontainersScalaV = "0.40.12"
val scalatestV           = "3.2.15"
val catsEffectTestingV   = "1.5.0"
val logbackV             = "1.2.11"

ThisBuild / scalaVersion                   := "2.13.11"
libraryDependencies += "org.tpolecat"      %% "skunk-core"                      % skunkV
libraryDependencies += "org.typelevel"     %% "toolkit"                         % toolkitV
libraryDependencies += "org.testcontainers" % "postgresql"                      % testcontainersV      % Test
libraryDependencies += "com.dimafeng"      %% "testcontainers-scala-scalatest"  % testcontainersScalaV % Test
libraryDependencies += "com.dimafeng"      %% "testcontainers-scala-postgresql" % testcontainersScalaV % Test
libraryDependencies += "org.scalatest"     %% "scalatest"                       % scalatestV           % Test
libraryDependencies += "org.typelevel"     %% "cats-effect-testing-scalatest"   % catsEffectTestingV   % Test
libraryDependencies += "ch.qos.logback"     % "logback-classic"                 % logbackV
