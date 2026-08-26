# DialerID for iOS

Native SwiftUI outbound SIP client that uses the same Firebase, SipUp, and NowPayments backends as the Android app. Bundle id: `com.dialerid.app`.

This tree was authored on Windows. **A Mac with Xcode 15+ is required** to resolve CocoaPods, sign, run on a device, and submit.

## What this build includes

- Email/password and Google Sign-In (Firebase Auth)
- Profile, credit, caller IDs, rates, recents, contacts, favorites
- $50 first-device paywall and wallet top-up via NowPayments
- Outbound Linphone REGISTER/INVITE with CallKit and live per-minute debit
- `tel:` URL handling

Incoming SIP / PushKit is not in this pass. The Android app is also outbound-only.

## Mac setup

1. Install Xcode, CocoaPods (`sudo gem install cocoapods`), and an Apple Developer team.
2. Copy secrets:

   ```bash
   cp ios/Secrets.xcconfig.example ios/Secrets.xcconfig
   ```

   Fill `SIPUP_API_KEY` and `NOW_PAYMENTS_API_KEY` with the same values as the Android `.env`.

3. In [Firebase Console](https://console.firebase.google.com/) open the existing DialerID project:
   - Add an **iOS** app with bundle id `com.dialerid.app`
   - Download `GoogleService-Info.plist` into `ios/DialerID/`
   - Enable Authentication (Email/Password + Google) if not already on
   - Confirm Realtime Database URL is `https://dialerid-default-rtdb.firebaseio.com`
   - Copy `CLIENT_ID` into `ios/Secrets.xcconfig` as `GID_CLIENT_ID`
   - Replace the Google URL scheme in `DialerID/Info.plist` (`CFBundleURLSchemes`) with `REVERSED_CLIENT_ID` from the plist
   - Add `GoogleService-Info.plist` to the DialerID app target in Xcode
   - The Android web client id remains the Google Sign-In **server** client id

4. Install pods from this folder:

   ```bash
   cd ios
   pod install
   open DialerID.xcworkspace
   ```

   Use the **workspace**, not the `.xcodeproj`, after `pod install`.

   Linphone comes from Linphone’s podspec source (`linphone-sdk` 5.3.77, matching Android). If CocoaPods cannot resolve it, add this to `Podfile` (already present):

   ```ruby
   source 'https://gitlab.linphone.org/BC/public/podspec.git'
   pod 'linphone-sdk', '5.3.77'
   ```

5. In Xcode:
   - Signing & Capabilities: select your team
   - Confirm Background Modes → **Audio** (in-call only)
   - Microphone and Contacts usage strings are already in `Info.plist`

6. Run on a **physical iPhone** (Simulator has no usable mic / CallKit audio path for SIP).

7. Tests (domain logic, no device needed):

   ```bash
   xcodebuild -workspace DialerID.xcworkspace -scheme DialerID -destination 'platform=iOS Simulator,name=iPhone 16' test
   ```

## After you add Swift files

Regenerate the Xcode file list from `ios/`:

```bash
python3 scripts/generate_xcode_project.py
```

Then `pod install` again if the project file changed.

## Device fee

The first iPhone is a new hardware record (`identifierForVendor`). It is charged the same **$50** NowPayments device fee as a new Android device.

## SIP behavior on iOS

REGISTER stays up while the app is active. iOS will not run a forever-on 20ms iterate loop in the background. Outbound calls keep the Core alive with the Audio background mode. After suspend, open the app again before dialing (same as Android after reboot).

## Appetize (virtual iPhone in the browser)

Appetize does **not** accept an `.ipa`. Upload a zipped **iOS Simulator** `.app` ([docs](https://docs.appetize.io/platform/app-management/uploading-apps/ios)). That build can only be produced on a Mac.

The Android Appetize app already uploaded from Windows stays on Android. Passing `?device=iphone16pro` on that URL does not turn it into iOS. iPhone devices need a separate iOS upload.

On a Mac, the Appetize script installs Firebase and Google Sign-In (`SKIP_LINPHONE=1` by default):

```bash
cd ios
bash scripts/build_simulator_app.sh
```

Upload the resulting `ios/build/DialerID-iphonesimulator.zip` in the Appetize dashboard, or use Depot CI (`.depot/workflows/appetize-ios.yml`):

1. Add Depot CI secret `APPETIZE_API_TOKEN` (`depot ci secrets add APPETIZE_API_TOKEN`).
2. Push `ios/` plus `.depot/workflows/appetize-ios.yml` and merge to the default branch.
3. Dispatch with `depot ci run --workflow .depot/workflows/appetize-ios.yml --forge origin`. After merge, `depot ci dispatch --repo usmanliaqatdeveloper/dialer-id --workflow appetize-ios.yml --ref main` also works.
4. Open the iOS public URL from the run summary, with `?device=iphone16pro`.

Depot CI sandboxes are Linux only, so the Simulator zip still has to be built on a Mac until Depot adds macOS runners. The workflow fails early with that message if `xcodebuild` is missing.

Optional: after the first iOS upload, store `APPETIZE_IOS_PUBLIC_KEY` so later runs update the same app.

What works there: auth, paywall, wallet, rates, contacts UI.  
The GitHub Actions Appetize job installs Firebase and Google Sign-In pods (`SKIP_LINPHONE=1`). CallKit, microphone, and a real SIP call still will not work there. Use a physical iPhone for that.

## Secrets

Do not commit:

- `ios/Secrets.xcconfig`
- `ios/Pods/`
