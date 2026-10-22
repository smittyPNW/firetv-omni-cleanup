#!/usr/bin/env bash
# Build the My Home launcher, install it on a Fire TV and turn on its Home redirect.
#
# Uses cleanyourtv's pure-Python ADB tools (macOS blocks the platform-tools adb binary on the local network).
# Clone https://github.com/Gathin23/cleanyourtv next to this repo, set up its venv (see its README), or point
# CLEANYOURTV at your clone.
#
#   TV_HOST=192.168.1.50 launcher/build-and-install.sh
#
# Amazon's launcher is never touched: it also hosts Settings. Amazon's screensaver is left alone too, because
# Fire OS 7 starts only its own screensaver; My Home shows your photos itself when idle (SlideshowActivity).
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 $0}"
export TV_HOST
CLEANYOURTV="${CLEANYOURTV:-../../cleanyourtv}"
PY="$CLEANYOURTV/tools/venv/bin/python"
[ -x "$PY" ] || { echo "cleanyourtv tools not found at $CLEANYOURTV (set CLEANYOURTV)"; exit 1; }
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[ -f local.properties ] || echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug -q --console=plain
APK=app/build/outputs/apk/debug/app-debug.apk
"$PY" -I "$CLEANYOURTV/tools/b64push.py" "$APK" /data/local/tmp/tvhome.apk
# Updating the APK unbinds the accessibility service even though the setting still reads enabled, so it is
# switched off and on again. This replaces any other enabled accessibility services.
printf '%s\n' \
  "pm install -r /data/local/tmp/tvhome.apk" \
  "rm -f /data/local/tmp/tvhome.apk" \
  "pm grant com.gathin.tvhome android.permission.WRITE_SECURE_SETTINGS" \
  "settings put secure enabled_accessibility_services null" \
  "settings put secure accessibility_enabled 0" \
  "settings put secure enabled_accessibility_services com.gathin.tvhome/com.gathin.tvhome.HomeRedirectService" \
  "settings put secure accessibility_enabled 1" \
  "am start -n com.gathin.tvhome/.MainActivity" \
  | "$PY" -I "$CLEANYOURTV/tools/tv.py" --batch
echo "Installed. Press Home: My Home should open in under a second. If Amazon's home stays up, move the"
echo "My Home tile to the front of Amazon's apps row (highlight it, Menu, Move to front)."
