#!/usr/bin/env bash
# Build a zipped iOS Simulator .app for Appetize. Requires macOS + Xcode + CocoaPods.
# SKIP_LINPHONE=1 omits SIP. Default is 0 so Appetize gets REGISTER/INVITE.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DERIVED="${DERIVED_DATA_PATH:-$ROOT/build/DerivedData}"
OUT_ZIP="${1:-$ROOT/build/DialerID-iphonesimulator.zip}"
export SKIP_LINPHONE="${SKIP_LINPHONE:-0}"

if [[ ! -f "$ROOT/Secrets.xcconfig" ]]; then
  cp "$ROOT/Secrets.xcconfig.example" "$ROOT/Secrets.xcconfig"
fi

if [[ ! -f "$ROOT/DialerID/GoogleService-Info.plist" ]]; then
  echo "Missing $ROOT/DialerID/GoogleService-Info.plist" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT_ZIP")" "$DERIVED"

if [[ "$SKIP_LINPHONE" != "1" ]]; then
  bash "$ROOT/scripts/install_linphone_sdk.sh"
fi

python3 "$ROOT/scripts/generate_xcode_project.py"

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
  CODE_SIGN_IDENTITY=- \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=YES \
  ONLY_ACTIVE_ARCH=YES \
  ARCHS=arm64 \
  EXCLUDED_ARCHS=x86_64 \
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

if [[ "$SKIP_LINPHONE" != "1" ]]; then
  if strings "$APP/DialerID" | grep -q "Linphone SDK is not linked"; then
    echo "DialerID compiled the Linphone stub. linphonesw was not visible to Swift." >&2
    exit 1
  fi
fi

rm -f "$OUT_ZIP"
(
  cd "$(dirname "$APP")"
  zip -qry "$OUT_ZIP" DialerID.app
)

echo "$OUT_ZIP"
