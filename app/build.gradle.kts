plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt.android)
    kotlin("plugin.serialization") version "2.1.0"
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.baselineprofile)
    // id("com.google.protobuf") version "0.9.5"
}

android {
    namespace = "com.jyotirmoy.musicly"
    compileSdk = 35

    androidResources {
        noCompress.add("tflite")
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            pickFirsts += "**/**.xml"
            pickFirsts += "**/**.png"
        }
        jniLibs {
            pickFirsts += "lib/**/libc++_shared.so"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    defaultConfig {
        applicationId = "com.jyotirmoy.musicly"
        minSdk = 29
        targetSdk = 35
        versionCode = (project.findProperty("APP_VERSION_CODE") as String).toInt()
        versionName = project.findProperty("APP_VERSION_NAME") as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Remove unused resources (languages)
        resConfigs("en") 
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            
            if (!keystorePath.isNullOrEmpty() && !keystorePassword.isNullOrEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                // Fallback or local dev without keys
                println("Release signing keys not found in environment variables. unexpected if running on CI.")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        // ADD THIS BLOCK:
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false // This removes the error you mentioned
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0"
        // To enable composition reports (readable):
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
        // Here is where you should add freeCompilerArgs for Compose compiler reports.
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir.absolutePath}/compose_compiler_reports"
        )
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir.absolutePath}/compose_compiler_metrics"
        )

        //Stability
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=${project.rootDir.absolutePath}/app/compose_stability.conf"
        )
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.paging.common)
    "baselineProfile"(project(":baselineprofile"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.generativeai)
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.navigation.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Baseline Profiles (Macrobenchmark)
    // Make sure libs.versions.toml has androidxBenchmarkMacroJunit4 and androidxUiautomator
    // Example: androidx-benchmark-macro-junit4 = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmarkMacro" }
    // benchmarkMacro = "1.2.4"
    //androidTestImplementation(libs.androidx.benchmark.macro.junit4)
    //androidTestImplementation(libs.androidx.uiautomator)


    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler) // For Dagger Hilt
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler) // This line is crucial and will work now

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    
    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Glance
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    //Gson
    implementation(libs.gson)

    //Serialization
    implementation(libs.kotlinx.serialization.json)

    //Work
    implementation(libs.androidx.work.runtime.ktx)

    //Duktape
    implementation(libs.duktape.android)

    //Smooth corners shape
    implementation(libs.smooth.corner.rect.android.compose)
    implementation(libs.androidx.graphics.shapes)

    //Navigation
    implementation(libs.androidx.navigation.compose)

    //Animations
    implementation(libs.androidx.animation)

    //Material3
    implementation(libs.material3)
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")

    //Coil
    implementation(libs.coil.compose)

    //Capturable
    implementation(libs.capturable) // Check the latest version on GitHub

    //Reorderable List/Drag and Drop
    implementation(libs.compose.dnd)
    implementation(libs.reorderables)

    //CodeView
    implementation(libs.codeview)

    //AppCompat
    implementation(libs.androidx.appcompat)

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer.ffmpeg)

    // Palette API for color extraction
    implementation(libs.androidx.palette.ktx)

    // For foreground service permission (Android 13+)
    implementation(libs.androidx.core.splashscreen) // Not directly for permission, but useful

    //ConstraintLayout
    implementation(libs.androidx.constraintlayout.compose)

    //Foundation
    implementation(libs.androidx.foundation)
    //Wavy slider
    implementation(libs.wavy.slider)

    // Splash Screen API (already included above)

    //Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Protobuf (JavaLite es suficiente para Android y más pequeño)
    // implementation(libs.protobuf.javalite) // Removed Protobuf dependency

    //Material library
    implementation(libs.material)

    // Kotlin Collections
    implementation(libs.kotlinx.collections.immutable) // Check for the latest version

    // Gemini
    implementation(libs.google.genai)

    //permisisons
    implementation(libs.accompanist.permissions)

    // Audio editing
    // Spleeter for audio separation and Amplituda for processing waveforms
    //implementation(libs.tensorflow.lite)
    //implementation(libs.tensorflow.lite.support)
    ///implementation(libs.tensorflow.lite.select.tf.ops)
    implementation(libs.amplituda)

    // Compose-audiowaveform for the UI
    implementation(libs.compose.audiowaveform)

    // Media3 Transformer (should be there, but make sure)
    implementation(libs.androidx.media3.transformer)

    //implementation(libs.pytorch.android)
    //implementation(libs.pytorch.android.torchvision)

    //Checker framework
    implementation(libs.checker.qual)

    // Timber
    implementation(libs.timber)

    // TagLib for metadata editing (supports mp3, flac, m4a, etc.)
    implementation(libs.taglib)
    // VorbisJava for Opus/Ogg metadata editing (TagLib has issues with Opus via file descriptors)
    implementation(libs.vorbisjava.core)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Ktor for HTTP Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.ui.text.google.fonts)

    implementation(libs.accompanist.drawablepainter)
    implementation(kotlin("test"))

    // Android Auto
    implementation(libs.androidx.media)
    implementation(libs.androidx.app)
    implementation(libs.androidx.app.projected)

}

