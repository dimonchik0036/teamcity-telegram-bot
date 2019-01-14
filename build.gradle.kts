import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.11"
    kotlin("jvm") version (kotlinVersion)
    id("org.jetbrains.kotlin.kapt") version (kotlinVersion)
}

group = "io.github.dimonchik0036"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    val requeryVersion = "1.5.1"
    compile("io.requery:requery:$requeryVersion")
    compile("io.requery:requery-kotlin:$requeryVersion")
    kapt("io.requery:requery-processor:$requeryVersion")
    compile(kotlin("stdlib-jdk8"))
    compile("org.jetbrains.teamcity:teamcity-rest-client:1.6.2")
    compile("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.xerial:sqlite-jdbc:3.25.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
