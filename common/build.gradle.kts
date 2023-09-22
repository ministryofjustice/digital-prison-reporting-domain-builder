plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("io.micronaut.minimal.library") version "3.7.10"
  id("kotlin-kapt")
  id("java-test-fixtures")
  jacoco
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.9.4")
}

dependencies {
  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.3")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.3")
  implementation("io.micronaut:micronaut-jackson-databind:3.9.4")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.3")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}