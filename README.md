# kotlintree

This little project provides Kotlin bindings for the popular [tree-sitter](http://github.com/tree-sitter/tree-sitter) library. Currently it only supports the Kotlin JVM target, but Kotlin native is on the roadmap (see [#3](https://github.com/oxisto/kotlintree/issues/3)).

It currently ships forks of [`tree-sitter`](https://github.com/0x6675636b796f75676974687562/tree-sitter) itself,
as well as [`tree-sitter-cpp`](https://github.com/0x6675636b796f75676974687562/tree-sitter-cpp).
We might want to include more languages (see [#2](https://github.com/oxisto/kotlintree/issues/2)).

## Build

Just run `./gradlew build`, this should build everything you need into a packaged jar, including the necessary native libraries.

## Usage

The latest release is available from _GitHub Packages_.

For `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "0x6675636b796f75676974687562/kotlintree"
        url = uri("https://maven.pkg.github.com/0x6675636b796f75676974687562/kotlintree")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

For `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            name = "0x6675636b796f75676974687562/kotlintree"
            url = uri("https://maven.pkg.github.com/0x6675636b796f75676974687562/kotlintree")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Then add the dependency as usual:

```kotlin
dependencies {
    implementation("io.github.oxisto:kotlin-tree-jna:0.0.1")
}
```
