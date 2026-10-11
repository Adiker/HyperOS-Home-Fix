plugins {
    id("com.android.application")
}

val signRelease = providers.environmentVariable("SIGN_RELEASE").orNull == "true"
fun requiredSigningVariable(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: throw GradleException("Signed release requested, but required environment variable $name is missing")

android {
    namespace = "io.github.adiker.hyperoshomefix"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "io.github.adiker.hyperoshomefix"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    if (signRelease) {
        signingConfigs {
            create("release") {
                storeFile = file(requiredSigningVariable("RELEASE_KEYSTORE_PATH"))
                require(storeFile?.isFile == true) { "Release keystore file does not exist" }
                storePassword = requiredSigningVariable("RELEASE_STORE_PASSWORD")
                keyAlias = requiredSigningVariable("RELEASE_KEY_ALIAS")
                keyPassword = requiredSigningVariable("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signRelease) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        // Pin the verified API-36 toolchain; an online update suggestion must not break old commits.
        disable += "AndroidGradlePluginVersion"
    }
    testOptions { unitTests.isReturnDefaultValues = false }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
}
