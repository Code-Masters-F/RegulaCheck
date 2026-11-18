import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Endereço da API do backend. Padrão: emulador (10.0.2.2 = computador hospedeiro).
// Aparelho físico: ./gradlew assembleDebug -PapiBaseUrl=http://<IP-do-computador>:8080
val apiBaseUrl: String = (findProperty("apiBaseUrl") as String?)?.trim()?.trimEnd('/')
    ?: "http://10.0.2.2:8080"
require(Regex("^https?://[A-Za-z0-9.\\-:\\[\\]]+$").matches(apiBaseUrl)) {
    "apiBaseUrl inválida: use http(s)://host[:porta], sem caminho."
}

android {
    namespace = "br.com.regulacheck.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.regulacheck.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-rc18"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            // A variante de entrega não é gerada nesta história; sem texto claro (só o debug tem
            // network_security_config permissivo).
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
