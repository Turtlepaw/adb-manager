import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.compose.hot-reload")
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation("com.konyaco:fluent:0.0.1-dev.8")
            implementation("com.konyaco:fluent-icons-extended:0.0.1-dev.8") // If you want to use full fluent icons.

            // https://mvnrepository.com/artifact/com.mayakapps.compose/window-styler
            implementation("com.mayakapps.compose:window-styler:0.3.2")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            // Add this dependency to fix the MutableScatterSet error
            implementation("androidx.collection:collection:1.3.0")
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.turtlepaw.adb.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "ADB Manager"
            packageVersion = "1.0.0"
            windows {
                dirChooser = true  // Allows the user to choose install location
                menuGroup = "Turtlepaw"
                shortcut = true
                perUserInstall = false  // Set to true for AppData\Local install
                iconFile.set(project.file("src/commonMain/resources/icon.ico"))
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.turtlepaw.adb.resources"
    generateResClass = auto
}

tasks.register<ComposeHotRun>("runHot") {
    mainClass.set("com.turtlepaw.adb.MainKt")
}