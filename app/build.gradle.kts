plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

val versaoCodigo = (System.getenv("VERSION_CODE") ?: "1").toInt()

val robolectricSdk: Configuration by configurations.creating
val robolectricDir = layout.buildDirectory.dir("robolectric-sdk")
val copiarRobolectric by tasks.registering(Copy::class) {
    from(robolectricSdk)
    into(robolectricDir)
}

android {
    namespace = "app.noplano"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.noplano"
        minSdk = 26
        targetSdk = 35
        versionCode = versaoCodigo
        versionName = "0.1.$versaoCodigo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Assinatura fixa (via variáveis de ambiente) para que cada nova versão
        // instale por cima da anterior sem perder os dados.
        val arquivo = System.getenv("NOPLANO_KEYSTORE")
        if (arquivo != null && file(arquivo).exists()) {
            create("pessoal") {
                storeFile = file(arquivo)
                storePassword = System.getenv("NOPLANO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NOPLANO_KEY_ALIAS") ?: "noplano"
                keyPassword = System.getenv("NOPLANO_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Sem ofuscação: o app é pessoal e a prioridade é não quebrar em tempo de execução.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("pessoal") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        // As versões das bibliotecas são fixadas de propósito; atualizá-las é uma decisão à parte.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        abortOnError = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                // O Robolectric usa a cópia do Android baixada pelo Gradle, sem ir à internet por conta própria.
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("robolectric.dependency.dir", robolectricDir.get().asFile.absolutePath)
                it.dependsOn(copiarRobolectric)
            }
        }
    }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.glance.appwidget)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    robolectricSdk("org.robolectric:android-all-instrumented:15-robolectric-12650502-i7") { isTransitive = false }
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.work.testing)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
