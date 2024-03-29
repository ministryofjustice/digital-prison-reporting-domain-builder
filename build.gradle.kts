import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  jacoco
  id("org.sonarqube") version "4.3.1.3277"
  id("org.owasp.dependencycheck") version "8.2.1"
  id("jacoco-report-aggregation")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
  mavenCentral()
}

allprojects {
  tasks.withType<Test>().configureEach {
    finalizedBy(tasks.withType<JacocoReport>()) // report is always generated after tests run
  }
  tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.test) // tests are required to run before generating the report
  }
}

subprojects {
  group = "uk.gov.justice"
  version = if (version != "unspecified") version else "0.0.1-SNAPSHOT"

  tasks {
    // Force Java 11 for this project
    withType<KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
    }
    // Allow tests to run in parallel in each module
    withType<Test>().configureEach {
      maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
    }
    withType<Tar> {
      duplicatesStrategy = DuplicatesStrategy.WARN
    }
    withType<Zip> {
      duplicatesStrategy = DuplicatesStrategy.WARN
    }
  }
}

tasks.check {
    dependsOn(tasks.withType(JacocoReport::class))
}

dependencies {
  implementation(project(":common"))
  implementation(project(":backend"))
  implementation(project(":cli"))
}

dependencyCheck {
  suppressionFile = "dependency-check-suppressions.xml"
  failBuildOnCVSS = 4.0F
}