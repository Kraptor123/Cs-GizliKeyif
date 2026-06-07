import com.android.build.api.dsl.LibraryExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("com.github.recloudstream:gradle:81b1d424d2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: LibraryExtension.() -> Unit) {
    extensions.getByName<LibraryExtension>("android").apply {
        project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            // Use Java 17 toolchain even if a higher JDK runs the build.
            // We still use Java 8 for now which higher JDKs have deprecated.
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }

        configuration()
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions",
                "-Xannotation-default-target=param-property"
            )
        }
    }

    cloudstream {
        // when running through gitHub workflow, GITHUB_REPOSITORY should contain current repository name
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Kraptor123/Cs-GizliKeyif")

        authors = listOf("kraptor")
    }


    android {
        namespace = "com.kraptor.${project.name.lowercase().replace("-", "_").let { if (it.firstOrNull()?.isDigit() == true) "p$it" else it }}"
        compileSdk = 36

        defaultConfig {
            minSdk = 21
        }

        lint {
            targetSdk = 36
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations

        // Stubs for all Cloudstream classes
        cloudstream("com.lagradost:cloudstream3:pre-release")

        // these dependencies can include any of those which are added by the app,
        // but you don't need to include any of them if you don't need them
        // https://github.com/recloudstream/cloudstream/blob/master/app/build.gradle
        implementation(kotlin("stdlib"))                                              // Kotlin'in temel kütüphanesi
        implementation("com.github.Blatzar:NiceHttp:0.4.13")                          // HTTP kütüphanesi
        implementation("org.jsoup:jsoup:1.22.1")                                      // HTML ayrıştırıcı
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.5")   // Kotlin için Jackson JSON kütüphanesi
        implementation("com.fasterxml.jackson.core:jackson-databind:2.13.5")          // JSON-nesne dönüştürme kütüphanesi
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")      // Kotlin için asenkron işlemler
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        implementation("org.mozilla:rhino:1.9.0")
        implementation("me.xdrop:fuzzywuzzy:1.4.0")
        implementation("com.google.code.gson:gson:2.13.2")
        implementation("app.cash.quickjs:quickjs-android:0.9.2")
        implementation("com.github.vidstige:jadb:v1.2.1")
    }
}


tasks.register("derle") {
    group = "help"
    doLast {
        println("Filtreleme modu aktif: status=1 olanlar disindaki eklentiler derleme disi birakildi.")
    }
}

gradle.taskGraph.whenReady {
    if (hasTask(":derle")) {
        allTasks.forEach { task ->
            if (task.project != rootProject) {
                val csExt = task.project.extensions.findByType<CloudstreamExtension>()
                if (csExt != null && csExt.status != 1) {
                    task.enabled = false
                }
            }
        }
    }
}