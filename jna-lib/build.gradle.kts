@file:Suppress(
    "UnstableApiUsage",
    "KDocMissingDocumentation",
)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutput.Style.Failure
import org.gradle.internal.logging.text.StyledTextOutput.Style.Success
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
    `maven-publish`
    signing
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

java {
    withSourcesJar()
}

/*
 * This is expected to work some day (but currently it does not),
 * see https://youtrack.jetbrains.com/issue/KT-32608.
 */
tasks.withType<KotlinTest> {
    reports.junitXml.required.set(true)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        showCauses = true
        showExceptions = true
        showStackTraces = true
        exceptionFormat = FULL
        events("passed", "skipped")
    }
    reports.junitXml.required.set(true)
}

tasks.withType<AbstractPublishToMaven> {
    dependsOn(tasks.withType<Sign>())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                artifactId = "kotlin-tree-jna"
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("net.java.dev.jna:jna:5.13.0")
}

testing {
    suites {
        @Suppress("UNUSED_VARIABLE")
        val test by getting(JvmTestSuite::class) {
            useKotlinTest()
        }
    }
}

configurePublishing()

fun Project.configurePublishing() {
    configureGitHubPublishing()
    configurePublications()
    configureSigning()
}

fun Project.configureGitHubPublishing() =
    publishing {
        repositories {
            maven {
                name = "GitHub"
                url = uri("https://maven.pkg.github.com/0x6675636b796f75676974687562/kotlintree")
                credentials {
                    username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

fun Project.configurePublications() {
    val dokkaJar = tasks.create<Jar>("dokkaJar") {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.findByName("dokkaHtml"))
    }

    configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            this.artifact(dokkaJar)
            this.pom {
                val project = this@configurePublications

                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/0x6675636b796f75676974687562/kotlintree")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://opensource.org/license/apache-2-0/")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("oxisto")
                        name.set("Christian Banse")
                        email.set("oxisto@aybaze.com")
                    }
                    developer {
                        id.set("0x6675636b796f75676974687562")
                        name.set("Andrey Shcheglov")
                        email.set("shcheglov.av@phystech.edu")
                    }
                }
                scm {
                    url.set("https://github.com/0x6675636b796f75676974687562/kotlintree")
                    connection.set("scm:git:https://github.com/0x6675636b796f75676974687562/kotlintree.git")
                    developerConnection.set("scm:git:git@github.com:0x6675636b796f75676974687562/kotlintree.git")
                }
            }
        }
    }
}

/**
 * Enables signing of the artifacts if the `signingKey` project property is set.
 *
 * Should be explicitly called after each custom `publishing {}` section.
 */
fun Project.configureSigning() {
    System.getenv("GPG_SEC")?.let {
        extra.set("signingKey", it)
    }
    System.getenv("GPG_PASSWORD")?.let {
        extra.set("signingPassword", it)
    }

    if (hasProperty("signingKey")) {
        /*
         * GitHub Actions.
         */
        configureSigningCommon {
            useInMemoryPgpKeys(property("signingKey") as String?, findProperty("signingPassword") as String?)
        }
    } else if (
        hasProperties(
            "signing.keyId",
            "signing.password",
            "signing.secretKeyRingFile",
        )
    ) {
        /*-
         * Pure-Java signing mechanism via `org.bouncycastle.bcpg`.
         *
         * Requires an 8-digit (short form) PGP key id and a present `~/.gnupg/secring.gpg`
         * (for gpg 2.1, run
         * `gpg --keyring secring.gpg --export-secret-keys >~/.gnupg/secring.gpg`
         * to generate one).
         */
        configureSigningCommon()
    } else if (hasProperty("signing.gnupg.keyName")) {
        /*-
         * Use an external `gpg` executable.
         *
         * On Windows, you may need to additionally specify the path to `gpg` via
         * `signing.gnupg.executable`.
         */
        configureSigningCommon {
            useGpgCmd()
        }
    }
}

/**
 * @param useKeys the block which configures the PGP keys. Use either
 *   [SigningExtension.useInMemoryPgpKeys], [SigningExtension.useGpgCmd], or an
 *   empty lambda.
 * @see SigningExtension.useInMemoryPgpKeys
 * @see SigningExtension.useGpgCmd
 */
@Suppress(
    "MaxLineLength",
    "SpreadOperator",
)
fun Project.configureSigningCommon(useKeys: SigningExtension.() -> Unit = {}) {
    configure<SigningExtension> {
        useKeys()
        val publications = extensions.getByType<PublishingExtension>().publications
        val publicationCount = publications.size
        val message = "The following $publicationCount publication(s) are getting signed: ${publications.map(Named::getName)}"
        val style = when (publicationCount) {
            0 -> Failure
            else -> Success
        }
        styledOut(logCategory = "signing").style(style).println(message)
        sign(*publications.toTypedArray())
    }
}

fun Project.styledOut(logCategory: String): StyledTextOutput =
    serviceOf<StyledTextOutputFactory>().create(logCategory)

/**
 * Determines if this project has all the given properties.
 *
 * @param propertyNames the names of the properties to locate.
 * @return `true` if this project has all the given properties, `false` otherwise.
 * @see Project.hasProperty
 */
fun Project.hasProperties(vararg propertyNames: String): Boolean =
    propertyNames.asSequence().all(this::hasProperty)
