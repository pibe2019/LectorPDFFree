import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 trae Kotlin integrado: ya no se aplica org.jetbrains.kotlin.android.
    alias(libs.plugins.kotlin.compose)
    // Las rutas de Navigation 3 son @Serializable para que el back stack sobreviva a la muerte del proceso.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.devtools.ksp)
}

/*
 * Firma de release. Las credenciales NUNCA van en el repo: se leen de `keystore.properties` (ignorado por git).
 * Si el archivo no existe, el release se firma con la clave de debug; sirve solo para probar R8 en local,
 * Play Console rechaza un AAB firmado asi. Formato del archivo en docs/PUBLICACION_PLAY_STORE.md.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use(::load)
}

android {
    namespace = "com.antonio.mypdffree"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Definitivo: Play no permite cambiarlo despues de la primera subida.
        applicationId = "com.antonio.mypdffree"
        // PdfRenderer existe desde API 21; 26 cubre ~97 % de dispositivos activos y es el mismo minimo de GuiaMedicaSebas.
        minSdk = 26
        // Play exige targetSdk 36 (Android 16) para apps nuevas desde el 31/08/2026.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // El recorrido de capturas no verifica nada: fuera de las ejecuciones de Gradle (local/CI).
        // Se lanza a mano con `adb shell am instrument` (ver ScreenshotTourTest).
        testInstrumentationRunnerArguments["notClass"] = "com.antonio.mypdffree.ScreenshotTourTest"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Modelo de AGP 9.3+: una sola linea activa R8 para codigo y recursos. Reglas en src/main/keepRules/.
            optimization { enable = true }
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Emulador gestionado por Gradle: `./gradlew pixel6api34DebugAndroidTest` lo descarga, lo arranca,
        // corre los tests instrumentados y lo apaga. Mismo comando en local y en CI.
        managedDevices {
            localDevices {
                create("pixel6api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    // ATD (Automated Test Device): imagen sin apps ni UI del sistema, mas ligera y rapida.
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    lint {
        abortOnError = true      // un error de lint rompe el build
        warningsAsErrors = false // los avisos se revisan, pero no bloquean
        checkReleaseBuilds = false
    }
}

kotlin { jvmToolchain(21) }

// Room exporta el esquema de cada version: sin el historico no se pueden escribir Migration ni sus tests.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    // Coroutines se usan directamente (Mutex, Flow): dependencia directa, no transitiva. El BOM en
    // `implementation` alcanza tambien a los classpaths de test y androidTest.
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // Navigation 3 + ViewModel por entrada del back stack
    implementation(libs.bundles.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
