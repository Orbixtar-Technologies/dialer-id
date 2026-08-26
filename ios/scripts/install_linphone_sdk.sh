#!/usr/bin/env bash
# Download the official Linphone iOS zip (no GitLab podspec) and expose
# xcframeworks to the local CocoaPods spec in Vendor/linphone-sdk-pod.
set -euo pipefail

VERSION="${LINPHONE_SDK_VERSION:-5.3.110}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${LINPHONE_CACHE_DIR:-$ROOT/build/cache}"
ZIP="$CACHE/linphone-sdk-${VERSION}.zip"
URL="https://download.linphone.org/releases/ios/linphone-sdk-${VERSION}.zip"
EXTRACT="$ROOT/Vendor/linphone-extracted"
POD_DIR="$ROOT/Vendor/linphone-sdk-pod"
FRAMEWORKS_LINK="$POD_DIR/Frameworks"

mkdir -p "$CACHE" "$POD_DIR"

if [[ ! -f "$ZIP" ]]; then
  echo "Downloading Linphone SDK ${VERSION}..."
  curl -L --fail --retry 3 --retry-delay 2 -o "${ZIP}.partial" "$URL"
  mv "${ZIP}.partial" "$ZIP"
fi

needs_extract=0
if [[ ! -d "$EXTRACT" ]]; then
  needs_extract=1
elif ! find "$EXTRACT" -name 'linphonesw.xcframework' -type d | grep -q .; then
  needs_extract=1
fi

if [[ "$needs_extract" -eq 1 ]]; then
  rm -rf "$EXTRACT"
  mkdir -p "$EXTRACT"
  echo "Extracting Linphone SDK..."
  unzip -q "$ZIP" -d "$EXTRACT"
fi

FOUND="$(find "$EXTRACT" -name 'linphonesw.xcframework' -type d | head -n 1 || true)"
if [[ -z "$FOUND" ]]; then
  echo "linphonesw.xcframework missing from $ZIP" >&2
  find "$EXTRACT" -maxdepth 5 -type d >&2 || true
  exit 1
fi

FRAMEWORKS_DIR="$(cd "$(dirname "$FOUND")" && pwd)"
rm -rf "$FRAMEWORKS_LINK"
mkdir -p "$FRAMEWORKS_LINK"
for fw in "$FRAMEWORKS_DIR"/*.xcframework; do
  ln -s "$fw" "$FRAMEWORKS_LINK/$(basename "$fw")"
done

echo "Linphone frameworks: $FRAMEWORKS_LINK"
