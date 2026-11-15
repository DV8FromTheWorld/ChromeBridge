# Chrome Bridge

Open web links from an Android Work profile directly in Personal Google Chrome. Keep one Chrome instance while isolating apps such as Discord with Island.

Install the **same APK in both profiles**. Healthy forwarding is one tap; recovery is explicit when Android stops the listener. Requires Android 11+, Personal Google Chrome and a profile policy that permits the connection. No root, ADB or privileged cross-profile permission needed.

## Setup / upgrade

1. Install in **Personal**, open Chrome Bridge, allow **Display over other apps** and notifications. If Android blocks a setting, App Info → menu → Allow restricted settings may be available.
2. Copy the pairing code, then **Start listener**. Start also enables automatic boot/update/process-death recovery. A persistent notification should appear.
3. Install/clone the same APK into **Work** using Island. Open that copy, paste/save the code and choose Chrome Bridge as Work's default browser. If cross-profile clipboard is blocked, enter the code manually.
4. Keep Chrome as **Personal** default browser. Disable Discord's in-app browser if it intercepts links.
5. In Personal app settings, set battery usage to **Unrestricted** for best reliability. This is optional and is never changed automatically.

For v1.0 → v1.1, update both copies without uninstalling, then open Personal and press **Start listener once** to enable the new saved restart preference. Pairing codes are retained. The distributed testing APK uses the same debug signing key as v1.0. Other developers' locally built debug APKs have a different key and cannot replace it without uninstalling or signing with the original key.

## If the listener is offline

The sender briefly retries connections, then retains the link on a recovery screen:

- **Retry forwarding**: try again after the listener recovers.
- **Open Personal listener**: uses Android's `CrossProfileApps.startMainActivity()` to open this app's own Personal launcher. If previously enabled, its listener starts. Return with Back and the retained Work request retries. If you intentionally stopped the listener, start it manually first.
- **Share through Personal…**: fallback to Android's share sheet. Select Personal → Chrome Bridge. That opens the shared link in Personal Chrome and restarts an already-enabled listener. It does not re-enable a listener you explicitly stopped. Profile policy may hide/block the Personal tab.
- **Copy link** or **Cancel**: no link is silently discarded. Copy writes to the system clipboard only when requested.

Automatic retries happen only before a socket connection is established. After a URL is submitted, a lost acknowledgement is reported as uncertain rather than blindly resending and opening duplicates.

## Restart guarantees and limits

- **Ordinary process death:** `START_STICKY` asks Android to recreate the foreground service. This is best effort, not an immediate or perpetual availability guarantee.
- **Reboot/update:** a non-direct-boot receiver handles `BOOT_COMPLETED` (after unlock) and `MY_PACKAGE_REPLACED`, only in Personal and only if the listener was enabled and overlay permission remains granted.
- **Explicit Stop in this app/notification:** disables saved restart intent. It stays stopped until Start is pressed.
- **Settings → Force stop:** Android deliberately keeps the app stopped. No hidden watchdog bypasses it. Reopen Personal or use user-triggered profile recovery.
- **Restricted battery mode / OEM task management:** can stop background work and defer boot broadcasts. Unrestricted helps but does not guarantee immunity. Android's Active apps → Stop also stops the process without callbacks.
- **Paused profile, profile policy, VPN, disabled Chrome or revoked permissions:** can still prevent forwarding or recovery.

These behaviors follow platform restrictions, not a claim that overlay permission grants unrestricted background execution. The initial v1.0 forwarding flow worked on the owner's phone; v1.1 restart/recovery flows still need physical-device testing.

## Privacy / security

- Forwarding intentionally moves a URL from Work into Personal. Only use where you are allowed to do that.
- Listener binds only `127.0.0.1:45678`. No cloud server, analytics, browsing-history collection, URL logs or external service.
- Pairing uses a per-install random secret in private app preferences. The sender authenticates the listener with HMAC before sending the URL; the secret is never transmitted. A request proof binds the nonce and URL.
- Only bounded HTTP(S) links are accepted; non-web schemes and credential-bearing URLs are rejected. Chrome is explicitly targeted, with no recursive generic-browser fallback.
- A compromised/rooted OS is outside the threat model. Another local process may deny service by occupying the port. App UI/clipboard privacy depends on Android.
- Success means an authenticated launch request was accepted, not proof Android visibly displayed Chrome.
- The APK is debug-signed for sideload testing, not a Play Store release.

## Build / test

Use Java 17 or 21, Android SDK platform 34 and build tools 34.0.0. Set `ANDROID_HOME` or `sdk.dir` in an untracked `local.properties`. Gradle 8.7 wrapper is included.

```sh
./gradlew assembleDebug lint test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Unit tests cover URL validation, HMAC binding, authenticated local socket delivery, rejecting a false listener without disclosing the URL and ambiguous lost acknowledgements. See [TESTING.md](TESTING.md) for device scenarios that a JVM cannot verify.

## License and provenance

Application code: [MIT](LICENSE). The original user-supplied scaffold and subsequent implementation were AI-assisted; no private conversation transcript is distributed. Third-party components retain their licenses: [notices](THIRD_PARTY_NOTICES.md).

## Platform references

- [Sticky service restart](https://developer.android.com/reference/android/app/Service#START_STICKY)
- [Foreground-service background-start exceptions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Special-use foreground services](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)
- [CrossProfileApps](https://developer.android.com/reference/android/content/pm/CrossProfileApps)
- [Force-stop behavior](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state)
- [Battery restrictions](https://developer.android.com/topic/performance/background-optimization#user-initiated-restrictions)
- [User stopping foreground services](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping)
