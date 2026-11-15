# Verification

## Automated
`./gradlew assembleDebug lint test`

Seven JVM tests per variant cover URL parsing, role/nonce/URL HMAC binding, successful socket handshake, rejecting a false server before disclosing a URL, invalid input and lost acknowledgements. Build verifies Android API use and manifest requirements. APK signing is checked separately with `apksigner verify`.

## Physical-device acceptance checklist (not yet executed for v1.1)
- Update both profiles without uninstalling; pairing survives; press Start once in Personal.
- Tap Work Discord link; Personal Chrome opens once.
- Reboot/unlock; listener returns; link works.
- Ordinary OS process kill (not force-stop); wait for sticky restart; link works.
- App notification Stop; reboot; listener remains disabled.
- Force-stop Personal; Work link shows recovery and retains URL. Open Personal listener; return; link opens once.
- If explicitly stopped, opening Personal must not silently re-enable it. Press Start to resume.
- Restrict battery mode; verify instructions/recovery when platform blocks background work.
- Revoke overlay permission; no crash or restart loop; grant and Start to recover.
- Wrong pairing key / another process owning port; no URL leaks to unauthenticated listener.
- Retry repeatedly; buttons disabled during active transmission; no duplicate automatic resend after uncertain acknowledgement.
- Rotate/recreate recovery screen; URL is retained, no automatic duplicate dispatch. Cancel/Back stops UI without falsely claiming delivery.
- Profile paused/hidden or profile switching denied; Share/manual fallback works or gives truthful guidance.
- Share to Personal Chrome Bridge; link opens and already-enabled listener restarts. Explicitly stopped listener stays stopped.
- Uninstall/disable Personal Chrome; clear error, no recursive browser intent.
- Upgrade APK while listener enabled; package-replaced recovery works.

Device policy, OEM, Android version and battery settings can change results. Report which scenario failed and what the recovery screen said; do not include confidential URLs or pairing codes in public issues.
