#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
adb="$sdk/platform-tools/adb"
emulator="$sdk/emulator/emulator"
avd="${AVD:-Pixel_8}"
device_port="${HOMING_PORT:-8080}"
host_port="${HOST_PORT:-18080}"
timeout_seconds="${SMOKE_TIMEOUT:-120}"
package="com.homingsms.app"
receiver="$package/.debug.DebugCommandReceiver"
apk="app/build/outputs/apk/debug/app-debug.apk"
base="http://127.0.0.1:$host_port/api/v1"
started_emulator=0

log() { printf '[smoke] %s\n' "$*"; }
fail() { printf '[smoke] FAIL: %s\n' "$*" >&2; exit 1; }

cleanup() {
  "$adb" forward --remove "tcp:$host_port" >/dev/null 2>&1 || true
  if [[ "$started_emulator" == "1" && -z "${KEEP_EMULATOR:-}" ]]; then
    log "Shutting the emulator down"
    "$adb" emu kill >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }

if [[ -z "${SKIP_BUILD:-}" ]]; then
  log "Building the debug APK"
  ./gradlew -q assembleDebug
fi
[[ -f "$apk" ]] || fail "missing $apk"

emulator_log="${TMPDIR:-/tmp}/homing-emulator.log"
if ! "$adb" devices | grep -q '^emulator-.*device$'; then
  log "Booting AVD $avd"
  "$emulator" -avd "$avd" -no-window -no-audio -no-boot-anim -no-snapshot-save >"$emulator_log" 2>&1 &
  emulator_pid=$!
  started_emulator=1
fi

for _ in $(seq 1 90); do
  "$adb" devices | grep -q '^emulator-.*device$' && break
  if [[ "$started_emulator" == "1" ]]; then
    if grep -q 'hvf is not enabled' "$emulator_log" 2>/dev/null; then
      fail "the emulator cannot use the Mac hypervisor from this shell. Run the script from Terminal.app or iTerm instead of an embedded or sandboxed terminal. Log: $emulator_log"
    fi
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
      reason="$(grep -m1 'FATAL' "$emulator_log" 2>/dev/null | sed -e 's/^FATAL *| *//' -e 's/\.$//')"
      fail "the emulator exited before booting: ${reason:-see the log}. Log: $emulator_log"
    fi
  fi
  sleep 2
done
"$adb" devices | grep -q '^emulator-.*device$' || fail "no emulator came online. Log: $emulator_log"
for _ in $(seq 1 180); do
  [[ "$("$adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
  sleep 2
done
[[ "$("$adb" shell getprop sys.boot_completed | tr -d '\r')" == "1" ]] || fail "emulator did not boot"
serial="$("$adb" get-serialno | tr -d '\r')"
own_number="+1555521${serial#emulator-}"
log "Device $serial, own number $own_number"

log "Installing $apk"
"$adb" install -r -g "$apk" >/dev/null
for permission in SEND_SMS RECEIVE_SMS READ_SMS RECEIVE_MMS RECEIVE_WAP_PUSH READ_PHONE_STATE READ_PHONE_NUMBERS POST_NOTIFICATIONS; do
  "$adb" shell pm grant "$package" "android.permission.$permission" >/dev/null 2>&1 || true
done

"$adb" shell am force-stop "$package"
broadcast() { "$adb" shell am broadcast -n "$receiver" -a "com.homingsms.app.debug.$1" "${@:2}" | tr -d '\r'; }

broadcast ENABLE_LOCAL --ei port "$device_port" | grep -q 'data="ok"' || fail "could not enable the local server"
"$adb" shell am start -W -n "$package/.MainActivity" >/dev/null
sleep 2
broadcast START | grep -q 'data="ok"' || fail "could not start the gateway"

key="$(broadcast CREATE_API_KEY --es name smoke --es scopes admin | sed -n 's/.*data="\(hk_live_[A-Za-z0-9]*\)".*/\1/p')"
[[ -n "$key" ]] || fail "could not create an API key"
log "Created API key ${key:0:12}…"

"$adb" forward "tcp:$host_port" "tcp:$device_port" >/dev/null
for _ in $(seq 1 30); do
  curl -sf "$base/health" >/dev/null 2>&1 && break
  sleep 1
done
health="$(curl -s "$base/health")" || fail "local API is not reachable"
log "Health: $health"

status="$(curl -s -o /dev/null -w '%{http_code}' "$base/messages")"
[[ "$status" == "401" ]] || fail "expected 401 without a key, got $status"
status="$(curl -s -o /dev/null -w '%{http_code}' "$base/docs/openapi.json")"
[[ "$status" == "200" ]] || fail "expected 200 for the OpenAPI document, got $status"

text="Homing smoke $(date +%s)"
response="$(curl -s -X POST "$base/messages" \
  -H "Authorization: Bearer $key" \
  -H 'Content-Type: application/json' \
  -d "{\"type\":\"sms\",\"to\":\"$own_number\",\"text\":\"$text\",\"idempotencyKey\":\"smoke-$(date +%s%N)\"}")"
id="$(printf '%s' "$response" | json "d.get('id','')")"
[[ "$id" == msg_* ]] || fail "send failed: $response"
log "Queued $id ($(printf '%s' "$response" | json "d['state']"))"

state=""
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  state="$(curl -s "$base/messages/$id" -H "Authorization: Bearer $key" | json "d['state']")"
  [[ "$state" == "sent" || "$state" == "delivered" ]] && break
  [[ "$state" == "failed" || "$state" == "expired" || "$state" == "cancelled" ]] && fail "message ended as $state: $(curl -s "$base/messages/$id" -H "Authorization: Bearer $key")"
  sleep 2
done
[[ "$state" == "sent" || "$state" == "delivered" ]] || fail "message stuck in $state"
log "Message $id reached $state"

inbox_id=""
deadline=$((SECONDS + timeout_seconds))
while (( SECONDS < deadline )); do
  inbox_id="$(curl -s "$base/inbox?limit=20" -H "Authorization: Bearer $key" | python3 -c "
import json,sys
text=sys.argv[1]
for item in json.load(sys.stdin)['data']:
    if item.get('text') == text:
        print(item['id']); break
" "$text")"
  [[ -n "$inbox_id" ]] && break
  sleep 2
done
[[ -n "$inbox_id" ]] || fail "inbound message did not show up in /inbox"
log "Inbound message $inbox_id is in /inbox"

detail="$(curl -s "$base/inbox/$inbox_id" -H "Authorization: Bearer $key")"
log "Inbox detail: $detail"
log "PASS"
