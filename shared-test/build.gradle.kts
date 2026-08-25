import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ncorti.ktfmt)
}

android {
  namespace = "work.bearbrains.joymouse.test"
  compileSdk = 34

  defaultConfig {
    minSdk = 34

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  compileOnly(project(":app"))
}

ktfmt { googleStyle() }

tasks.register<KtfmtFormatTask>("ktfmtPrecommit") {
  source = project.fileTree(projectDir)
  include("**/*.kt")
  exclude("**/build/**")
}
