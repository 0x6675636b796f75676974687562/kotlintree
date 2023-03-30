import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.dokka") version "1.6.0"

    kotlin("jvm") version "1.8.20" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<KotlinCompile> {
    }

    tasks.dokkaHtml.configure {
        // intentionally hide inherited members, otherwise we will have a lot of functions inherited from JNA
        suppressInheritedMembers.set(true)
    }
}

repositories {
    mavenCentral()
}

allprojects {
    group = "io.github.oxisto"
    version = "0.0.0-SNAPSHOT"
}

tasks.dokkaHtmlCollector.configure{
    // intentionally hide inherited members, otherwise we will have a lot of functions inherited from JNA
    suppressInheritedMembers.set(true)
}
