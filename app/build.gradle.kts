import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)

  alias(libs.plugins.ncorti.ktfmt)
}

ktfmt { googleStyle() }

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
  source = project.fileTree(rootDir)
  include("**/*.kt")
}

android {
  namespace = "work.bearbrains.joymouse"
  compileSdk = 34

  defaultConfig {
    applicationId = "work.bearbrains.joymouse"
    minSdk = 34
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.15"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.core.ktx)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.google.truth)
  testImplementation(libs.mockito)
  testImplementation(project(":app"))
  testImplementation(project(":shared-test"))

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.google.truth)
  androidTestImplementation(project(":app"))
  androidTestImplementation(project(":shared-test"))

  // Jetpack Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.ui)

  implementation(libs.androidx.material3)
}