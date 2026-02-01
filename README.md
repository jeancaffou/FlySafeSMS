# FlySafeSMS

FlySafeSMS is an open source companion to FlySafe focused on emergency SMS location sharing between trusted friends. It lets you add friends, control sharing permissions, and auto-reply to verified SMS requests with GPS or last-known location.

## Features
- Add/remove friends and manage per-friend location sharing
- Auto-reply to verified SMS requests (GPS or last-known)
- Emergency SMS request shortcuts
- Edge-to-edge UI with dark mode support

## How It Works
FlySafeSMS listens for incoming SMS requests that match FlySafe’s format and verifies the friend code against your cached friends list. If allowed, it replies with your location by SMS.

### Critical Behavior Details
- **Incoming request format**: `<#> <replyPhone> #FlySafe <uid> <lockey> <type> 0qgXwnSWj2t`
- **Types supported**: `GPS`, `LastKnownLocation`, `ALL`
- **Verification**: requests are only honored if the sender’s UID + lockey match the cached friends list **and** location sharing for that friend is enabled.
- **Caching dependency**: friend verification uses the locally cached friends list. Ensure you have refreshed friends after login.
- **Roaming guard**: if “Allow SMS replies while roaming” is off, requests are ignored while roaming.
- **GPS timeout**: GPS requests wait up to **2 minutes** for a fix, then stop listening.
- **Fallback**: LastKnownLocation uses cached GPS/network provider results and will not trigger a new fix.

## Permissions
- SMS: `RECEIVE_SMS`, `READ_SMS`, `SEND_SMS`
- Location: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- Background location (Android 10+): `ACCESS_BACKGROUND_LOCATION`

On Android 10+, background location is a separate permission. The app will prompt you to allow “**Allow all the time**” so it can respond when running in the background.

## Privacy
- Uses local storage for session and preferences
- Does not include any hardcoded secrets
- Only responds to verified friend codes

## Debugging
Logcat tag is `FlySafeSMS`. The SMS receiver logs incoming PDUs, parsed messages, and early exits (permissions/roaming/format).

## Build
Open in Android Studio and run `assembleDebug` or `assembleRelease`.

## Contributing
Issues and pull requests are welcome.

## License
MIT License. See `LICENSE`.
