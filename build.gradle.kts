import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.11"
    kotlin("jvm") version (kotlinVersion)
}

group = "io.github.dimonchik0036.tcbot"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("com.github.pengrad:java-telegram-bot-api:4.1.0")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.jetbrains.teamcity:teamcity-rest-client:1.6.2")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

task<Jar>("fatJar") {
    baseName = "${project.name}-fatJar"
    manifest {
        attributes(
            mapOf(
                "Implementation-Version" to project.version,
                "Main-Class" to "${project.group}.MainKt"
            )
        )
    }

    from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}