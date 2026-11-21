#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_KEYSTORE_FILE:?Set ANDROID_KEYSTORE_FILE to your private PKCS12 keystore}"
: "${ANDROID_KEYSTORE_PASSWORD:?Set ANDROID_KEYSTORE_PASSWORD}"
: "${ANDROID_KEY_ALIAS:?Set ANDROID_KEY_ALIAS}"
: "${ANDROID_KEY_PASSWORD:?Set ANDROID_KEY_PASSWORD}"

cd "$(dirname "$0")/.."
gradle --no-daemon :app:assembleRelease
printf '\nSigned APK: app/build/outputs/apk/release/app-release.apk\n'
