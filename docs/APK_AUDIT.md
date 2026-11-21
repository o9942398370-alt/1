# Supplied APK audit

Inspected the uploaded APK archive, binary Android manifest, DEX strings/class metadata and signing certificate.

## Identity

- Application ID: `dev.servers.app`
- Launcher Activity: `dev.servers.app.MainActivity`
- Original manifest version: `1.2` / version code `3`
- Original minimum Android API: 26
- Original target Android API: 34
- Original UI title text: `servers`
- Original launcher label: `֍`

## Confirmed Termux coupling in the original APK

- Declares `com.termux.permission.RUN_COMMAND`.
- Declares a package query for `com.termux`.
- Calls `com.termux.app.RunCommandService` through `com.termux.RUN_COMMAND`.
- Uses Termux run-command extras for command, working directory and background execution.
- Contains shell scripts using `bash`, `pkill`, `kill`, `sleep`, `tail`, and `termux-wake-lock`.
- Checks whether Termux is installed and whether its run-command permission is granted.

These references are not present in the new app manifest or Java sources.

## Bedrock-only scope changes

The original DEX also contains Java `.jar` detection and a `java -jar` command. The new project deliberately excludes `.jar` detection and Java launch support. It only accepts server folders with both `bedrock_server` and `server.properties`.

## Visual constants recovered

- Main background: `#000000`
- Card background: `#121212`
- Card border: `#2C2C2C`
- Main text: `#FFFFFF`
- Secondary text: `#9A9A9A`
- Status green: `#2E7D32`
- Status orange: `#B26A00`
- Muted status gray: `#616161`

The supplied APK launcher icon bitmaps were copied to the corresponding Android density folders.

## Signing key check

The SHA-256 fingerprint from the uploaded PKCS12 keystore matches the signer certificate embedded in the supplied APK. The keystore itself and its passwords are intentionally excluded from this source tree.

## Reconstruction note

The uploaded APK contained compiled DEX and resources, not the original Java/Kotlin project. This repository is a clean source reconstruction of the requested Bedrock-only app and its UI style; it is not a byte-for-byte recovery of the original source code.
