plugins {
  id("com.android.library")
}

android {
  namespace = "io.leloush.fang.wolfi"
  compileSdk = 37
  ndkVersion = "30.0.16248370"

  defaultConfig {
    minSdk = 29
    ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    externalNativeBuild {
      cmake {
        cppFlags("")
      }
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "4.1.2"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

// The same toolchain the app uses. Without it the tests run on the JDK that runs Gradle, which is older than the
// class files this module's own compile options produce, and cannot load them.
java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(26)
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
}
