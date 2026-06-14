import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "dev.inflx"

version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        testRuns.all {
            executionTask { useJUnitPlatform() }
        }
    }

    android {
        namespace = "dev.inflx.simpleid"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter:6.1.0")
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "simple-id", version.toString())

    pom {
        name = "Simple ID"
        description = "A simple id format"
        inceptionYear = "2026"
        url = "https://github.com/dklz/simple-id/"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
            }
        }

        developers {
            developer {
                name = "lz"
                email = "helloworld@dklz.ca"
            }
        }

        scm {
            url = "https://github.com/dklz/simple-id"
            connection = "scm:git:git://github.com/dklz/simple-id.git"
            developerConnection = "scm:git:ssh://git@github.com/dklz/simple-id.git"
        }
    }
}
