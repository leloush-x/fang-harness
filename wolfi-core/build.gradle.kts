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
