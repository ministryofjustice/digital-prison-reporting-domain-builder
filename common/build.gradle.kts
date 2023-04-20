plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("io.micronaut.minimal.library") version "3.7.7"
  id("kotlin-kapt")
  id("java-test-fixtures")
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.8.8")
}

dependencies {
  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.2")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.2")
  implementation("io.micronaut:micronaut-jackson-databind:2.14.2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
