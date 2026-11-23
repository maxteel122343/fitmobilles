plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.instanthmr.tatvty"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.0"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.mlkit.pose.detection)
    implementation(libs.mlkit.obj.detection)
    implementation(libs.onnxruntime.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
}

val packageProjectZip by tasks.registering(Zip::class) {
    archiveFileName.set("fitverso_projeto_completo.zip")
    from(rootProject.layout.projectDirectory) {
        include("app/src/**")
        include("app/build.gradle.kts")
        include("app/proguard-rules.pro")
        include("gradle/**")
        include("web/**")
        include("build.gradle.kts")
        include("settings.gradle.kts")
        include("gradle.properties")
        include("README.md")
        include("LICENSE")
        include("LICENSE-SAM")
        include("metadata.json")
        include(".gitignore")
        exclude("**/build/**")
        exclude("**/.gradle/**")
        exclude("**/.git/**")
        exclude("**/debug.keystore*")
        exclude("**/.env")
        exclude("**/fitverso_projeto_completo.zip")
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(packageProjectZip) {
            it.destinationDirectory
        }
    }
}

tasks.configureEach {
    if (name == "mergeDebugAssets" || name == "mergeReleaseAssets") {
        dependsOn(packageProjectZip)
    }
}

