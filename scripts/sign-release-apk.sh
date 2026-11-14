#!/usr/bin/env bash
# Signs an unsigned release APK with Fang Harness's release key and checks that the result carries the release certificate.
# .github/workflows/release.yml runs it in a job that holds the signing secrets and nothing else.
#
# Usage: sign-release-apk.sh <unsigned.apk> <signed.apk>
#
# Environment:
#   RELEASE_KEYSTORE_BASE64    the PKCS12 keystore, base64-encoded
#   RELEASE_KEYSTORE_PASSWORD  the keystore's password, which PKCS12 also uses for the key
#   RELEASE_KEY_ALIAS          the key's alias in the keystore
#   RELEASE_CERT_SHA256        the SHA-256 digest of the release certificate, in hex; colons are optional
#   BUILD_TOOLS                the Android build-tools folder holding zipalign and apksigner
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "Usage: $0 <unsigned.apk> <signed.apk>" >&2
  exit 2
fi
unsigned=$1
signed=$2

: "${RELEASE_KEYSTORE_BASE64:?is not set}"
: "${RELEASE_KEYSTORE_PASSWORD:?is not set}"
: "${RELEASE_KEY_ALIAS:?is not set}"
: "${RELEASE_CERT_SHA256:?is not set}"
: "${BUILD_TOOLS:?is not set}"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
keystore="$work/release.p12"
(
  umask 077
  printf '%s' "$RELEASE_KEYSTORE_BASE64" | base64 --decode >"$keystore"
)

# 16 KB page alignment keeps the APK installable on devices with 16 KB memory pages.
"$BUILD_TOOLS/zipalign" -P 16 -f 4 "$unsigned" "$work/aligned.apk"
# The password reaches apksigner through the environment, never the command line.
#
# Every scheme is named instead of left to the build-tools default, which follows the app's minimum and so can
# quietly drop the older schemes when the minimum moves. v1, v2 and v3 each cost a few kilobytes, and each is the
# one scheme some installer reads: a phone whose package manager only understands the JAR signature, or one that
# only understands v2, refuses a v3-only APK with nothing but "the package appears to be invalid". v4 is a separate
# .idsig file that sideloading does not use.
"$BUILD_TOOLS/apksigner" sign \
  --ks "$keystore" \
  --ks-type PKCS12 \
  --ks-pass env:RELEASE_KEYSTORE_PASSWORD \
  --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --min-sdk-version 29 \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --v4-signing-enabled false \
  --out "$signed" \
  "$work/aligned.apk"

# The app's own minimum, Android 10, is where an install has to be possible at all.
verification=$("$BUILD_TOOLS/apksigner" verify --min-sdk-version 29 --verbose --print-certs "$signed")
if [ "$(head -n 1 <<<"$verification")" != "Verifies" ]; then
  echo "$signed does not verify for Android 10, the app's minimum" >&2
  echo "$verification" >&2
  exit 1
fi

# apksigner prints one digest per signature scheme; they must all name the one release certificate.
actual=$(sed -n 's/^.*certificate SHA-256 digest: //p' <<<"$verification" | sort -u)
expected=$(printf '%s' "$RELEASE_CERT_SHA256" | tr -d ':' | tr 'A-F' 'a-f')
if [ "$actual" != "$expected" ]; then
  echo "$signed is signed by $actual, not the release certificate $expected" >&2
  exit 1
fi

# Checked against the oldest minimum apksigner knows, because that is where it reports every scheme the file
# carries: at Android 10 it reports only the v3 signature, the one a phone on Android 10 uses. Missing v1 or v2 is a
# package an installer that reads only those refuses, with no error beyond "the package appears to be invalid".
compat=$("$BUILD_TOOLS/apksigner" verify --min-sdk-version 21 --verbose "$signed")
for scheme in "v1 scheme (JAR signing)" "v2 scheme (APK Signature Scheme v2)" "v3 scheme (APK Signature Scheme v3)"; do
  if ! grep -qx "Verified using $scheme: true" <<<"$compat"; then
    echo "$signed does not carry the $scheme signature" >&2
    echo "$compat" >&2
    exit 1
  fi
done

# apksigner keeps the alignment it was given, and a device with 16 KB memory pages needs it: an APK that lost the
# alignment is refused on those devices.
"$BUILD_TOOLS/zipalign" -c -P 16 -v 4 "$signed"
