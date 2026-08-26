#!/usr/bin/env bash
# Build a zipped iOS Simulator .app for Appetize. Requires macOS + Xcode + CocoaPods.
# SKIP_LINPHONE=1 (default here) links Firebase/Google Sign-In without the Linphone pod.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DERIVED="${DERIVED_DATA_PATH:-$ROOT/build/DerivedData}"
OUT_ZIP="${1:-$ROOT/build/DialerID-iphonesimulator.zip}"
export SKIP_LINPHONE="${SKIP_LINPHONE:-1}"

if [[ ! -f "$ROOT/Secrets.xcconfig" ]]; then
  cp "$ROOT/Secrets.xcconfig.example" "$ROOT/Secrets.xcconfig"
fi

if [[ ! -f "$ROOT/DialerID/GoogleService-Info.plist" ]]; then
  echo "Missing $ROOT/DialerID/GoogleService-Info.plist" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT_ZIP")" "$DERIVED"

(
  cd "$ROOT"
  pod install
)

xcodebuild \
  -workspace "$ROOT/DialerID.xcworkspace" \
  -scheme DialerID \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath "$DERIVED" \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=- \
  ONLY_ACTIVE_ARCH=NO \
  build

APP="$DERIVED/Build/Products/Debug-iphonesimulator/DialerID.app"
if [[ ! -d "$APP" ]]; then
  echo "DialerID.app not found at $APP" >&2
  exit 1
fi

if [[ ! -f "$APP/GoogleService-Info.plist" ]]; then
  echo "GoogleService-Info.plist was not copied into DialerID.app" >&2
  exit 1
fi

rm -f "$OUT_ZIP"
(
  cd "$(dirname "$APP")"
  zip -qry "$OUT_ZIP" DialerID.app
)

echo "$OUT_ZIP"
