#!/usr/bin/env bash
# Installs tilfazi on an Android TV over ADB and makes it the TV's home, the same on every maker's set:
#   install/install.sh [tv-ip] [Tilfazi.apk]
# Without an address it finds the TV on this network itself (install/find-tv.sh). Without an APK it downloads the
# latest release. It stops with a line starting «ASK:» when the person has to do something; see docs/INSTALL-AGENT.md. Changes on the TV only what the home needs: tilfazi installed, set
# as the home, the TV's own home switched off (not removed), tilfazi's screensaver chosen. Undo: install/undo.sh <tv-ip>
set -uo pipefail

ip="${1:-}"
apk="${2:-}"
command -v adb > /dev/null || { echo "ASK: adb-missing"; exit 1; }
if [ -z "$ip" ]; then
  echo "== Looking for the TV on this network"
  tvs=$("$(dirname "$0")/find-tv.sh")
  echo "$tvs"
  case "$tvs" in
    NONE*) echo "ASK: no-tv"; exit 3 ;;
  esac
  if [ "$(wc -l <<< "$tvs" | tr -d ' ')" -gt 1 ]; then echo "ASK: which-tv"; exit 2; fi
  ip=${tvs%% *}
fi
pkg=app.tilfazi.tv
home=$pkg/.home.AsHome
tv="$ip:5555"
[[ "$ip" == *:* ]] && tv="$ip"

say() { printf '\n== %s\n' "$*"; }
sh() { adb -s "$tv" shell "$@" 2>&1 | tr -d '\r'; }


say "Connecting to $tv"
for _ in 1 2 3 4 5 6; do
  out=$(adb connect "$tv" 2>&1)
  if adb -s "$tv" get-state 2> /dev/null | grep -q device; then break; fi
  echo "$out"
  echo "Waiting: if the TV asks «Allow USB debugging?», choose «Always allow»…"
  sleep 8
done
adb -s "$tv" get-state 2> /dev/null | grep -q device || {
  if adb devices | grep -q "^$tv[[:space:]]*unauthorized"; then echo "ASK: allow-debugging"; else echo "ASK: no-connection"; fi
  exit 4
}
model=$(sh getprop ro.product.model); sdk=$(sh getprop ro.build.version.sdk)
echo "TV: $model, Android SDK $sdk"

if [ -z "$apk" ]; then
  say "Downloading the latest Tilfazi.apk"
  apk=$(mktemp -d)/Tilfazi.apk
  url=$(curl -fsSL https://api.github.com/repos/TalalRugi/TilfaziTV/releases/latest | grep -o '"browser_download_url": *"[^"]*\.apk"' | head -1 | sed 's/.*"\(http[^"]*\)"/\1/')
  [ -n "$url" ] && curl -fsSL "$url" -o "$apk" || { echo "!! Could not download the APK; pass one: install/install.sh $ip Tilfazi.apk"; exit 1; }
fi

say "Installing"
ok() { [[ "$1" == *Success* ]]; }
result=$(adb -s "$tv" install -r "$apk" 2>&1)
if ! ok "$result"; then
  # Play Protect may refuse an APK sent over ADB: its check is turned off for this one install, then put back
  before=$(sh settings get global verifier_verify_adb_installs)
  sh settings put global verifier_verify_adb_installs 0 > /dev/null
  result=$(adb -s "$tv" install -r "$apk" 2>&1)
  if [ "$before" = null ]; then sh settings delete global verifier_verify_adb_installs > /dev/null
  else sh settings put global verifier_verify_adb_installs "$before" > /dev/null; fi
fi
ok "$result" || { echo "!! Install failed: $result"; exit 1; }
sleep 3
# some operator TVs remove apps that were not installed by the Play Store a moment later: install again as from it
if ! sh pm list packages $pkg | grep -q "$pkg"; then
  adb -s "$tv" install -r -i com.android.vending "$apk" > /dev/null 2>&1
  sh pm list packages $pkg | grep -q "$pkg" || { echo "!! The TV removed tilfazi after installing it."; exit 1; }
fi
echo "Installed."

say "The TV's own home"
# the homes that came with the TV (system apps); a launcher the person installed is theirs and left alone
system=$(sh pm list packages -s | sed 's/^package://')
stock=$(sh cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
  | sed 's/^[[:space:]]*//' | grep / | grep -v -e "^$pkg/" -e com.android.tv.settings -e com.android.settings -e setupwraith -e '^android/' \
  | while read -r a; do grep -qx "${a%%/*}" <<< "$system" && echo "$a"; done | sort -u)
echo "${stock:-none found}"

# A guard on the TV itself: if tilfazi is not confirmed as the home within 5 minutes (ADB lost, black screen), the
# TV's own home comes back by itself.
first=$(head -1 <<< "$stock")
restore=""
while read -r a; do [ -n "$a" ] && restore+="pm enable ${a%%/*}; "; done <<< "$stock"
[ -n "$first" ] && restore+="cmd package set-home-activity --user 0 $first; input keyevent KEYCODE_HOME"
sh "rm -f /data/local/tmp/tilfazi_ok; nohup sh -c 'sleep 300; [ -f /data/local/tmp/tilfazi_ok ] || { $restore; }' > /dev/null 2>&1 &"

say "Making tilfazi the home"
sh input keyevent KEYCODE_WAKEUP   # a TV asleep shows nothing, and nothing can be checked on it
sh cmd package set-home-activity --user 0 $home
[ "${sdk:-0}" -ge 29 ] && sh cmd role add-role-holder --user 0 android.app.role.HOME $pkg
refused=no
while read -r a; do
  [ -z "$a" ] || [ "${a%%/*}" = "$pkg" ] && continue
  r=$(sh pm disable-user --user 0 "${a%%/*}")
  echo "$r"
  grep -q disabled-user <<< "$r" && sh "echo ${a%%/*} >> /data/local/tmp/tilfazi_disabled"   # for undo.sh
  grep -qi illegal <<< "$r" && refused=yes
done <<< "$stock"
sh cmd package set-home-activity --user 0 $home
sh input keyevent KEYCODE_HOME

opened=no
for _ in 1 2 3; do
  sleep 5
  sh dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | grep -q "$pkg" && { opened=yes; break; }
  # asleep again or still dark: Home is ours when Android says so
  sh dumpsys power | grep -q 'mWakefulness=Awake' || { sh cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep -q "^$home" && { opened=yes; break; }; }
  sh input keyevent KEYCODE_HOME
done
if [ "$opened" = yes ]; then
  sh touch /data/local/tmp/tilfazi_ok
  echo "Home opens tilfazi."
elif [ "$refused" = yes ]; then
  sh touch /data/local/tmp/tilfazi_ok
  sh "$restore"
  echo "!! This TV does not let its own home be switched off. It keeps it; tilfazi opens from the apps."
else
  echo "!! Home did not open tilfazi. Leave the TV alone: its own home comes back by itself within 5 minutes."
  echo "   Then ask the person to press Home and choose «تلفازي» then «دائماً», if the TV asks."
fi

say "Screensaver"
sh settings put secure screensaver_components $pkg/.ambient.AmbientDream
sh settings put secure screensaver_enabled 1
echo "Done."

say "To undo"
echo "install/undo.sh $ip"
