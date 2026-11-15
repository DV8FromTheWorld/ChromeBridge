# Verification

## Automated
`./gradlew assembleDebug lint test`

Sixteen JVM tests per variant cover URL parsing, role/nonce/URL HMAC binding, successful socket handshake, rejecting a false server before disclosing a URL, invalid input and lost acknowledgements, plus battery-settings launch/fallback selection. Build verifies Android API use and manifest requirements. APK signing is checked separately with `apksigner verify`.

## Physical-device acceptance checklist (not yet executed for v1.1/v1.2)
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

## Battery shortcut device checks
- In Personal, press battery button: app-specific battery page opens on supported Android; choose Unrestricted yourself. No setting changes just from tapping the button.
- Back/Cancel without changes: app remains usable and does not claim Unrestricted is enabled.
- Unsupported/restricted direct action: falls back to App info; unavailable App info falls back to Settings; no handler gives manual instructions.
- Work copy does not show Personal battery recommendation.
- Changing battery setting does not imply force-stop, OEM or policy restrictions can be bypassed.

## Live setup indicators
- Return after granting/revoking overlay or notifications: checkmarks refresh immediately. Block listener notification channel separately and verify it is reflected.
- Start/Stop: active listener reflects successful socket binding, separately from enabled restart preference.
- Restrict battery mode and return: show restriction accurately. Doze exemption is a separate row; do not show verified Unrestricted.
- Install/pause/hide companion: availability refreshes, with no claim about its permissions/default browser.
- Save code: does not show pairing verified until an authenticated link succeeds. Changing the Work code clears its prior success timestamp.
- Open Work link: receipt/acknowledgement timestamps update; only timestamps, no URLs, persist.

## Redesigned setup screen
- Verify Personal first screen at normal and large font sizes in light/dark mode. Cards scroll without clipped text or buttons; all controls have at least 48dp touch targets.
- App info is the first setup action. It opens this profile's Chrome Bridge details page; no setting is changed automatically. If Android shows the overflow action, the user can choose Allow restricted settings. No completed badge is fabricated for that manually checked setting.
- Return from app info, overlay, notification and battery settings without changing anything: indicators remain truthful and controls still work. Grant/revoke permissions and return: indicators refresh.
- Show/hide diagnostics repeatedly, then rotate and reopen: no crash, no automatic listener start when explicitly stopped.
- Start and Stop use one live-state-aware button. Socket state and restart preference remain distinct.
- Pairing code copying still works; code remains selectable for manual entry. Work keeps its own setup and default-browser action.

### Redesign verification (2026-11-15)
- `assembleDebug lint test`: passed after final UI wording change; lint reported no issues; 16 JVM tests passed in each debug/release variant.
- Development emulator: official AOSP API30 x86_64, 480×800 at 160dpi, software emulation because the host has no `/dev/kvm`. Inspected actual Personal top and scrolled permissions/battery screenshots: cards, text and buttons rendered without clipping. Iterated the oversized headline into a compact single-line title so setup is visible sooner, and replaced the overflow glyph with explicit three-dot-menu wording.
- Tapped the actual App info shortcut and verified Android's Chrome Bridge App info screen. No permission or battery setting was changed. API30 does not expose the later sideloaded-app restricted-settings menu, so that user-controlled menu remains an Android-version/device check.
- Earlier Google APIs/software-emulator attempts had Android framework crashes and SystemUI ANR dialogs; the lightweight dev image eventually produced clean captures after dismissing a SystemUI ANR. This is limited visual/setup-path verification, not full device reliability acceptance. Cross-profile forwarding, reboot recovery, OEM battery behavior and current Android runtime notification prompts still require the device checklist above.
