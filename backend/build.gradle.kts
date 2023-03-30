plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  application
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":common"))
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

application {
  mainClass.set("uk.gov.justice.digital.ExampleAppKt")
}
