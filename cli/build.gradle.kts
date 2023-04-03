plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("io.micronaut.minimal.application") version "3.7.7"
  id("kotlin-kapt")
  id("com.github.johnrengelman.shadow") version "7.1.2"
  // TODO - review global setup and ensure reports are on global coverage
  id("jacoco")
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.8.7")
}

val kotlinVersion = "1.8.10"

dependencies {
  implementation(project(":common"))

  implementation("info.picocli:picocli")
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut:micronaut-runtime")
  implementation("io.micronaut:micronaut-validation")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("info.picocli:picocli-shell-jline3:4.7.1")
  implementation("org.fusesource.jansi:jansi:2.4.0")

  runtimeOnly("ch.qos.logback:logback-classic")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  kapt("io.micronaut:micronaut-inject-java")

  kaptTest("io.micronaut:micronaut-inject-java")

  testImplementation("io.micronaut.test:micronaut-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

application {
  mainClass.set("uk.gov.justice.digital.DomainBuilder")
}

tasks.named<JavaExec>("run") {
  // Ensure we attach STDIN so interactive mode works when using the run task
  standardInput = System.`in`
}
