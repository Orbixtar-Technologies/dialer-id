#!/usr/bin/env bash
# Download the official Linphone iOS zip and copy xcframeworks plus the
# Swift wrapper into Vendor/linphone-sdk-pod. Copies, not symlinks — CocoaPods
# globs do not reliably follow links, which left DialerID compiling the stub.
set -euo pipefail

VERSION="${LINPHONE_SDK_VERSION:-5.3.110}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${LINPHONE_CACHE_DIR:-$ROOT/build/cache}"
ZIP="$CACHE/linphone-sdk-${VERSION}.zip"
URL="https://download.linphone.org/releases/ios/linphone-sdk-${VERSION}.zip"
EXTRACT="$ROOT/Vendor/linphone-extracted"
POD_DIR="$ROOT/Vendor/linphone-sdk-pod"
FRAMEWORKS_DIR_OUT="$POD_DIR/Frameworks"
SWIFT_DIR_OUT="$POD_DIR/linphonesw"

mkdir -p "$CACHE" "$POD_DIR"

if [[ ! -f "$ZIP" ]]; then
  echo "Downloading Linphone SDK ${VERSION}..."
  curl -L --fail --retry 3 --retry-delay 2 -o "${ZIP}.partial" "$URL"
  mv "${ZIP}.partial" "$ZIP"
fi

if [[ ! -d "$EXTRACT" ]] || ! find "$EXTRACT" -name 'linphone.xcframework' -type d | grep -q .; then
  rm -rf "$EXTRACT"
  mkdir -p "$EXTRACT"
  echo "Extracting Linphone SDK..."
  unzip -q "$ZIP" -d "$EXTRACT"
fi

FOUND="$(find "$EXTRACT" -name 'linphone.xcframework' -type d | head -n 1 || true)"
if [[ -z "$FOUND" ]]; then
  echo "linphone.xcframework missing from $ZIP" >&2
  exit 1
fi

FRAMEWORKS_SRC="$(cd "$(dirname "$FOUND")" && pwd)"
SWIFT_SRC="$(find "$EXTRACT" -path '*/share/linphonesw' -type d | head -n 1 || true)"

rm -rf "$FRAMEWORKS_DIR_OUT" "$SWIFT_DIR_OUT"
mkdir -p "$FRAMEWORKS_DIR_OUT"
for fw in "$FRAMEWORKS_SRC"/*.xcframework; do
  name="$(basename "$fw")"
  case "$name" in
    *tester*) continue ;;
  esac
  cp -R "$fw" "$FRAMEWORKS_DIR_OUT/$name"
done

if [[ -n "$SWIFT_SRC" ]]; then
  mkdir -p "$SWIFT_DIR_OUT"
  cp -R "$SWIFT_SRC"/. "$SWIFT_DIR_OUT/"
fi

echo "=== Linphone wrapper files ==="
find "$SWIFT_DIR_OUT" -type f 2>/dev/null | head -n 40 || true
SWIFT_COUNT="$(find "$SWIFT_DIR_OUT" -name '*.swift' 2>/dev/null | wc -l | tr -d ' ')"
XC_COUNT="$(find "$FRAMEWORKS_DIR_OUT" -name '*.xcframework' -maxdepth 1 | wc -l | tr -d ' ')"
echo "xcframeworks=$XC_COUNT swift_files=$SWIFT_COUNT"

if [[ "$XC_COUNT" -lt 1 ]]; then
  echo "No Linphone xcframeworks were copied" >&2
  exit 1
fi
if [[ "$SWIFT_COUNT" -lt 1 ]]; then
  echo "No linphonesw Swift sources were copied from $SWIFT_SRC" >&2
  find "$EXTRACT" -iname '*.swift' | head -n 40 >&2 || true
  exit 1
fi

