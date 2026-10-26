#!/usr/bin/env bash
# Puts the TV's own home back and removes tilfazi:   install/undo.sh [tv-ip]   (without one, it finds the TV)
set -uo pipefail
ip="${1:-}"
if [ -z "$ip" ]; then
  tvs=$("$(dirname "$0")/find-tv.sh"); echo "$tvs"
  case "$tvs" in NONE*) echo "ASK: no-tv"; exit 3 ;; esac
  [ "$(wc -l <<< "$tvs" | tr -d ' ')" -gt 1 ] && { echo "ASK: which-tv"; exit 2; }
  ip=${tvs%% *}
fi
tv="$ip:5555"; [[ "$ip" == *:* ]] && tv="$ip"
sh() { adb -s "$tv" shell "$@" 2>&1 | tr -d '\r'; }
adb connect "$tv" > /dev/null
# only what install.sh switched off, as it wrote down on the TV
for p in $(sh cat /data/local/tmp/tilfazi_disabled 2> /dev/null | sort -u); do sh pm enable "$p"; done
sh rm -f /data/local/tmp/tilfazi_disabled /data/local/tmp/tilfazi_ok
sh settings delete secure screensaver_components > /dev/null
sh pm uninstall app.tilfazi.tv
sh input keyevent KEYCODE_HOME
echo "The TV's own home is back."
