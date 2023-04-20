import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
}

repositories {
  mavenCentral()
}

subprojects {
  group = "uk.gov.justice"
  version = "1.0-SNAPSHOT"

  tasks {
    withType<KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
    }
  }

  tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
  }

}
