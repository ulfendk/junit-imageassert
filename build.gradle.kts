plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

group = "dk.ulfen"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    
    // JUnit 5 for testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Image processing libraries
    implementation("org.imgscalr:imgscalr-lib:4.2")
}

kotlin {
    jvmToolchain(11)
}

tasks.test {
    useJUnitPlatform()
}
