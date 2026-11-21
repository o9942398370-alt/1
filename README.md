# servers — Standalone Bedrock Manager

An Android project based on the supplied `dev.servers.app` package and visual style. It is restricted to Minecraft Bedrock server folders and Bedrock `.mcpack` archives; Java `.jar` servers are not scanned or imported.

## Preserved from the supplied app

- Package name: `dev.servers.app`
- Original monochrome dark palette: black background, `#121212` cards, `#2C2C2C` borders, white text and `#9A9A9A` secondary text
- Existing icon artwork from the supplied APK
- English UI terminology and the general Servers / Builds / Packs / Settings structure

## Requested build identity

- `versionName`: `0.0.0`
- `versionCode`: `1`
- `minSdk`: 26
- `targetSdk`: 34

## What this source revision does

- Uses Android's Storage Access Framework; imported server data is copied into app-private storage.
- Imports a Bedrock server folder or a Bedrock Linux ZIP.
- Rejects archives that do not contain both `bedrock_server` and `server.properties`.
- Imports Bedrock `.mcpack` archives and validates `manifest.json`.
- Installs behavior/resource packs into the selected Bedrock server and updates the world's corresponding pack list.
- Edits common `server.properties` fields.
- Saves auto-start and auto-restart preferences.
- Has no dependency on Termux or another terminal app. No Termux permission, package query, intent, or service is included.

## Important runtime limitation

This is **not yet a runnable replacement for the old server launcher**. The app can manage, import, configure and install Bedrock data, but its Start action deliberately reports that the embedded Linux runtime is missing. The official Bedrock Dedicated Server download page lists Ubuntu 22.04 LTS or later for Linux, and current Minecraft Help states that official server builds are not available for ARM-based devices. An ARM64 Android phone therefore needs a bundled x86_64 compatibility runtime (such as Box64) and a glibc Linux userspace. Those runtime binaries and root filesystem are not included in this project yet, and the source does not pretend that an x86_64 Linux executable can run directly on Android.

The runtime integration is the remaining blocker before Start / Stop / background supervision can be completed. Existing server files are not modified when the app reports this limitation.

## Current Bedrock networking note

Minecraft Help published a networking migration notice dated October 29, 2026. For affected self-hosted Bedrock Dedicated Servers, the current guidance is to update to BDS 26.60 or newer, open the server port for both TCP and UDP (default 19132), and set `transport=nethernet` in `server.properties`. The Properties editor includes a Transport field for this setting. Confirm the instructions for the exact BDS build you install before changing a live server.

## Build

The project uses Android Gradle Plugin 8.5.2, Gradle 8.7 and Java 17.

```bash
gradle --no-daemon :app:assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Sign with the existing private key

The uploaded PKCS12 keystore was checked against the certificate embedded in the supplied APK; the SHA-256 fingerprints match. **Do not commit `app-keystore.p12`, passwords, or signing details to a public repository.** The `.gitignore` excludes local signing files.

For a local signed release, provide these environment variables without writing their values into source code:

- `ANDROID_KEYSTORE_FILE`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Then run:

```bash
./scripts/build-signed-release.sh
```

For GitHub Actions, add these repository secrets:

- `ANDROID_KEYSTORE_BASE64` — base64 text of the PKCS12 file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The workflow decodes the key into the runner's temporary directory, signs the release APK, and uploads APK artifacts. The private keystore is never put in the Git repository.

## Install/update note

The supplied APK had a higher version code than the requested `1`. Android normally rejects installing a lower version code as an update. To install this requested version, uninstall the previous app first (back up anything important first), or use a development install flow that explicitly permits a version downgrade. The package name and signing certificate are kept compatible with the uploaded app.

## Project scope

This repository is only for managing Minecraft Bedrock Dedicated Server data. It intentionally does not include Java server support, Java `.jar` scanning, or Java server launch commands.
