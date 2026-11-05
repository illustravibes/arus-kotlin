plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.illustravibes.arus"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.illustravibes.arus"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    sourceSets.getByName("androidTest").assets.srcDir("schemas")

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.paging:paging-compose:3.4.1")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.12.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.12.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.12.0")
}
