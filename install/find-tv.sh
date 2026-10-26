#!/usr/bin/env bash
# Finds the Android TVs on this computer's network that accept ADB, without the person typing an address:
#   install/find-tv.sh
# Prints one line per TV found: «<ip> <model>», or the first word NONE when there is none. A TV whose model cannot be
# read yet (it is waiting for «Allow USB debugging?» on its screen) shows as «<ip> waiting».
set -uo pipefail

# this computer's private networks (home Wi-Fi or cable), on macOS, Linux and Windows (Git Bash)
nets=$( { ifconfig 2> /dev/null; ip -4 addr 2> /dev/null; ipconfig 2> /dev/null | tr -d '\r'; } \
  | grep -oE '192\.168\.[0-9]+\.[0-9]+|10\.[0-9]+\.[0-9]+\.[0-9]+|172\.(1[6-9]|2[0-9]|3[01])\.[0-9]+\.[0-9]+' | grep -vE '\.(255|0)$' | sed 's/\.[0-9]*$//' | sort -u)
if [ -z "$nets" ]; then echo "NONE no-network"; exit 0; fi

# no ADB call may hang the search (a TV that went away leaves adb waiting): each gets a few seconds
limit() { local t=$1; shift; perl -e 'alarm shift; exec @ARGV' "$t" "$@"; }

found=()
# TVs this computer reached before (adb keeps them), wherever they are
while read -r ip; do found+=("$ip"); done < <(limit 5 adb devices 2> /dev/null | grep -oE '^([0-9]{1,3}\.){3}[0-9]{1,3}')
# TVs that announce themselves (Wireless debugging, Android 11+)
while read -r line; do
  ip=$(grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' <<< "$line" | head -1)
  [ -n "$ip" ] && found+=("$ip")
done < <(limit 5 adb mdns services 2> /dev/null | grep -i '_adb')

# every address on those /24 networks with port 5555 open, all probed at once, each given two seconds
tmp=$(mktemp -d)
(
  for net in $nets; do
    for h in $(seq 1 254); do
      ( exec 3<> "/dev/tcp/$net.$h/5555" ) && touch "$tmp/$net.$h" &
    done
  done
  sleep 2
  kill $(jobs -p)
) 2> /dev/null
for f in "$tmp"/*; do [ -e "$f" ] && found+=("$(basename "$f")"); done
rm -rf "$tmp"

if [ ${#found[@]} -eq 0 ]; then echo "NONE $(echo $nets | sed 's/ /.x, /g').x"; exit 0; fi
for ip in $(printf '%s\n' "${found[@]}" | sort -u); do
  # an open port is not yet a TV (a router may answer too): it counts only once ADB itself connects
  grep -q 'connected' <<< "$(limit 6 adb connect "$ip:5555" 2>&1)" || { limit 3 adb disconnect "$ip:5555" > /dev/null 2>&1; continue; }
  state=$(limit 5 adb -s "$ip:5555" get-state 2>&1)
  if [ "$state" = device ]; then
    echo "$ip $(limit 5 adb -s "$ip:5555" shell getprop ro.product.model 2> /dev/null | tr -d '\r')"
  else
    echo "$ip waiting"   # «Allow USB debugging?» is on the TV's screen
  fi
  any=yes
done
[ -n "${any:-}" ] || echo "NONE $(echo $nets | sed 's/ /.x, /g').x"
