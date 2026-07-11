#!/bin/bash
# Seed key dependencies from the project
ARTIFACTS=(
"androidx.compose:compose-bom:2024.06.00"
"androidx.core:core-ktx:1.13.1"
"androidx.lifecycle:lifecycle-runtime-ktx:2.8.2"
"androidx.activity:activity-compose:1.9.0"
"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2"
"androidx.lifecycle:lifecycle-runtime-compose:2.8.2"
"androidx.navigation:navigation-compose:2.7.7"
"androidx.camera:camera-core:1.3.4"
"androidx.camera:camera-camera2:1.3.4"
"androidx.camera:camera-lifecycle:1.3.4"
"androidx.camera:camera-video:1.3.4"
"androidx.camera:camera-view:1.3.4"
"androidx.camera:camera-extensions:1.3.4"
"org.tensorflow:tensorflow-lite:2.16.1"
"org.tensorflow:tensorflow-lite-nnapi:2.16.1"
"org.tensorflow:tensorflow-lite-support:0.4.4"
"com.google.dagger:hilt-android:2.51"
"com.google.dagger:hilt-compiler:2.51"
"androidx.hilt:hilt-navigation-compose:1.2.0"
"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0"
"androidx.datastore:datastore-preferences:1.1.1"
"androidx.exifinterface:exifinterface:1.3.7"
"io.coil-kt:coil-compose:2.6.0"
"com.android.application:com.android.application.gradle.plugin:8.7.3"
"com.android.tools.build:gradle:8.7.3"
"org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin:1.9.25"
"org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25"
"com.google.dagger.hilt.android:com.google.dagger.hilt.android.gradle.plugin:2.51.1"
"com.google.dagger:hilt-android-gradle-plugin:2.51.1"
"org.jetbrains.kotlin.kapt:org.jetbrains.kotlin.kapt.gradle.plugin:1.9.25"
"junit:junit:4.13.2"
"org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0"
"org.robolectric:robolectric:4.12.2"
"androidx.test:core:1.5.0"
"androidx.test:core-ktx:1.5.0"
"org.mockito:mockito-core:5.11.0"
"org.mockito.kotlin:mockito-kotlin:5.2.1"
"org.mockito:mockito-inline:5.2.0"
"androidx.arch.core:core-testing:2.2.0"
"androidx.test.ext:junit:1.1.5"
"androidx.test.espresso:espresso-core:3.5.1"
)

download_artifact() {
    local gav="$1"
    local gid="${gav%%:*}"
    local rest="${gav#*:}"
    local aid="${rest%%:*}"
    local ver="${rest##*:}"
    local group_path=$(echo "$gid" | tr '.' '/')
    local dir="$HOME/.m2/repository/$group_path/$aid/$ver"
    
    [ -z "$gid" ] || [ -z "$aid" ] || [ -z "$ver" ] && return 1
    
    if [ -f "$dir/$aid-$ver.pom" ]; then
        echo "  [exists] $gav"
        return 0
    fi
    
    mkdir -p "$dir"
    
    for base_url in "https://repo1.maven.org/maven2" "https://dl.google.com/dl/android/maven2"; do
        local url="$base_url/$group_path/$aid/$ver/$aid-$ver.pom"
        if curl -sL --connect-timeout 10 --max-time 30 -o "$dir/$aid-$ver.pom" "$url" 2>/dev/null; then
            if [ -s "$dir/$aid-$ver.pom" ] && ! grep -q "<!DOCTYPE" "$dir/$aid-$ver.pom" 2>/dev/null && ! grep -q "Not Found" "$dir/$aid-$ver.pom" 2>/dev/null; then
                echo "  [downloaded] $gav from $base_url"
                curl -sL --connect-timeout 10 --max-time 60 -o "$dir/$aid-$ver.jar" "$base_url/$group_path/$aid/$ver/$aid-$ver.jar" 2>/dev/null || true
                return 0
            fi
        fi
    done
    
    rm -rf "$dir" 2>/dev/null
    echo "  [FAILED] $gav"
    return 1
}

echo "Seeding ${#ARTIFACTS[@]} core artifacts..."
for artifact in "${ARTIFACTS[@]}"; do
    download_artifact "$artifact"
done
echo "Seeding complete."
echo "POMs: $(find $HOME/.m2/repository -name '*.pom' | wc -l)"
