# Stable signing and CI

The release signing key must never be committed, uploaded as an Actions artifact,
or included in logs. Keep an owner-controlled recoverable backup of the encrypted
keystore and its passwords before publishing the first APK signed with it.
GitHub repository secrets are execution storage, not a recoverable backup.

## GitHub Actions secrets

Configure these repository secrets only after the backup is safely retained:

- `CHROMEBRIDGE_KEYSTORE_BASE64`: base64 of the encrypted JKS file. Base64 is encoding, not encryption.
- `CHROMEBRIDGE_KEYSTORE_PASSWORD`: keystore password.
- `CHROMEBRIDGE_KEY_ALIAS`: alias of the signing key.
- `CHROMEBRIDGE_KEY_PASSWORD`: key password.

The workflow builds/lints/tests every pull request without exposing signing secrets.
Only pushes to `master` and manual runs on `master` in the owner repository can run
the release job after checks pass. Anyone who can change trusted branch workflow or
build code can misuse repository secrets; review such changes carefully.
The key is materialized temporarily with restrictive permissions, removed at exit,
and never cached or uploaded. Actions are pinned to verified commit IDs.

The signed APK and SHA-256 checksum are retained as run artifacts for 30 days.
The debug APK is for testing only and uses an ephemeral runner key. It cannot update
the stable release or the original development-signed installations.

## Installation migration

The first stable release uses a new certificate. Uninstall the old debug copy in
both profiles, install the same new release APK in both, and pair them again. Future
stable releases must retain this signing certificate and increment `versionCode`.

v1.3 also changes the application ID to `net.dv8tion.chromebridge`. It installs
as a separate app, not an update to the former package. See the README migration
steps; settings and pairing are not transferred automatically.

## Local release builds

Set these environment variables in a secure shell/session without printing them:
`CHROMEBRIDGE_KEYSTORE_PATH`, `CHROMEBRIDGE_KEYSTORE_PASSWORD`,
`CHROMEBRIDGE_KEY_ALIAS`, and `CHROMEBRIDGE_KEY_PASSWORD`.
Then run `./gradlew --no-daemon assembleRelease`. With no signing configuration,
local release output is unsigned; do not distribute it as an installable release.

References:
- https://developer.android.com/studio/publish/app-signing
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets
