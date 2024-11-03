plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.poe2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.poe2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyDDKwRyK9dJDF8qxV41F8UDDvl4bSLhfUA\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    dependencies {
        // AndroidX and Material Components
        implementation(libs.androidx.core.ktx.v1101)
        implementation(libs.androidx.appcompat)
        implementation(libs.material.v190)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.navigation.fragment.ktx)
        implementation(libs.androidx.navigation.ui.ktx)
        implementation(libs.androidx.legacy.support.v4)
        implementation(libs.androidx.fragment.ktx)

        // Firebase
        implementation(platform(libs.firebase.bom)) // Use the latest Firebase BoM
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.database.ktx)
        implementation ("com.google.firebase:firebase-messaging-ktx")

        // Google Play Services
        implementation("com.google.android.gms:play-services-maps:18.1.0")
        implementation("com.google.android.gms:play-services-places:17.0.0") // Use version 17.0.0
        implementation("com.google.android.gms:play-services-location:18.0.0") // Use version 18.0.0

        // Networking
        implementation(libs.okhttp)
        implementation(libs.okhttp.v500alpha8)

        // Utilities
        implementation(libs.android.maps.utils)

// AndroidX Test dependencies
androidTestImplementation ("androidx.test.ext:junit:1.1.5")
androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")

// Fragment testing
debugImplementation ("androidx.fragment:fragment-testing:1.6.1")

// JUnit (for unit testing)
testImplementation ("junit:junit:4.13.2") // Or using libs.junit depending on your build system

// Mocking framework
testImplementation ("org.mockito:mockito-core:5.4.0")

// Mockk (optional for Kotlin-based mocking)
testImplementation ("io.mockk:mockk:1.12.3")

// Coroutines (if you are using coroutines in your app)
testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

// Firebase dependencies (Firebase Database)
androidTestImplementation ("com.google.firebase:firebase-database:20.4.0")
androidTestImplementation ("com.google.firebase:firebase-database-ktx:20.4.0")

// Mockito for Android (if needed for instrumentation tests)
androidTestImplementation ("org.mockito:mockito-android:5.4.0")

// ActivityTestRule and other AndroidX test utilities
androidTestImplementation ("androidx.test:runner:1.5.1")
androidTestImplementation ("androidx.test:rules:1.5.0")
androidTestImplementation ("androidx.navigation:navigation-testing:2.5.3")

// Firebase Authentication
implementation ("com.google.firebase:firebase-auth-ktx:21.0.3")

// Google Play Services Authentication (for OAuth, Google sign-in, etc.)
implementation ("com.google.android.gms:play-services-auth:18.0.0")

// Using the new versioning system (optional)
androidTestImplementation(libs.androidx.junit.v115) // If you're using Gradle version catalogs
androidTestImplementation(libs.androidx.espresso.core.v351)
// Material CalendarView
implementation ("com.applandeo:material-calendar-view:1.9.2")

// api
        implementation ("com.squareup.retrofit2:retrofit:2.9.0")
        implementation ("com.squareup.retrofit2:converter-gson:2.9.0")


    }

}
dependencies {

    implementation(libs.places)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.testing)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.core)
    implementation(libs.androidx.legacy.support.v4)
    androidTestImplementation(libs.junit.junit)
    androidTestImplementation(libs.junit.junit)

    implementation ("com.applandeo:material-calendar-view:1.9.2°")

    implementation ("com.auth0:java-jwt:4.2.1")

    // Biometric library for Android
    implementation ("androidx.biometric:biometric:1.1.0")

    testImplementation ("junit:junit:4.13.2")
    testImplementation ("org.mockito:mockito-core:3.12.4")
    androidTestImplementation ("org.mockito:mockito-android:3.12.4")
    androidTestImplementation ("androidx.test:core:1.4.0")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
}
