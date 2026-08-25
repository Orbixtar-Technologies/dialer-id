#!/usr/bin/env bash
# Build a zipped iOS Simulator .app for Appetize. Requires macOS + Xcode.
# Does not install CocoaPods; Firebase/Linphone stay optional via canImport.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DERIVED="${DERIVED_DATA_PATH:-$ROOT/build/DerivedData}"
OUT_ZIP="${1:-$ROOT/build/DialerID-iphonesimulator.zip}"

if [[ ! -f "$ROOT/Secrets.xcconfig" ]]; then
  cp "$ROOT/Secrets.xcconfig.example" "$ROOT/Secrets.xcconfig"
fi

mkdir -p "$(dirname "$OUT_ZIP")" "$DERIVED"

xcodebuild \
  -project "$ROOT/DialerID.xcodeproj" \
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

rm -f "$OUT_ZIP"
(
  cd "$(dirname "$APP")"
  zip -qry "$OUT_ZIP" DialerID.app
)

echo "$OUT_ZIP"
