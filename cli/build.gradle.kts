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
  version.set("3.8.8")
}

val kotlinVersion = "1.8.10"

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("info.picocli:picocli")
  implementation("info.picocli:picocli-shell-jline3:4.7.1")
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.2")
  implementation("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut:micronaut-runtime")
  implementation("io.micronaut:micronaut-validation")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("org.fusesource.jansi:jansi:2.4.0")
  implementation(project(":common"))
  implementation("io.micronaut.reactor:micronaut-reactor")
  implementation("io.micronaut.reactor:micronaut-reactor-http-client")

  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.2")
  kapt("io.micronaut:micronaut-inject-java")

  kaptTest("io.micronaut:micronaut-inject-java")

  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.2")

  testImplementation("io.micronaut.test:micronaut-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
  testImplementation(testFixtures(project(":common")))
  testImplementation("io.micronaut:micronaut-http-server-netty")
  testImplementation("io.mockk:mockk:1.13.5")

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
