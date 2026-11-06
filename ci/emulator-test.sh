#!/bin/bash
# Prueba Filtro en un emulador: instala, concede permisos, simula llamadas y toma capturas.
set -u
PKG=app.filtro
OUT=ci-out
mkdir -p "$OUT"
log() { echo "== $*" | tee -a "$OUT/pasos.txt"; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; log "captura $1"; }
call() { log "llamada entrante $1: $(adb emu gsm call "$1" | tr -d '\r' | tr '\n' ' ')"; }
hangup() { adb emu gsm cancel "$1" >/dev/null 2>&1; }
tap_text() { python3 ci/ui.py tap "$1" | tee -a "$OUT/pasos.txt"; }

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
sleep 2

log "Instalando"
adb install -r Filtro.apk 2>&1 | tee -a "$OUT/pasos.txt"
adb shell pm list packages | grep -q "$PKG" || { log "no se instaló"; exit 1; }

log "Primera apertura (sin permisos)"
adb shell am start -W -n $PKG/.MainActivity --es pais 56 >/dev/null
sleep 5
shot 01-inicio-sin-activar

log "Concediendo rol y permisos"
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING $PKG
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
log "rol: $(adb shell cmd role get-role-holders android.app.role.CALL_SCREENING | tr -d '\r')"
adb shell input keyevent KEYCODE_HOME
sleep 1
adb shell am start -W -n $PKG/.MainActivity --es tab inicio >/dev/null
sleep 3
shot 02-inicio-activo

adb logcat -c
log "800: debe bloquearse sin sonar"
call 800123456
sleep 6
shot 03-durante-800
hangup 800123456
sleep 2

log "809: debe bloquearse"
call 8091234567
sleep 5
hangup 8091234567
sleep 2

log "Celular: suena con identificador"
call 912345678
sleep 6
shot 04-celular-identificador
hangup 912345678
sleep 4
shot 05-despues-de-cortar

log "600: suena con identificador"
call 6006002020
sleep 6
shot 06-empresa-600
hangup 6006002020
sleep 3

log "Internacional"
call +18765551234
sleep 6
shot 07-internacional
hangup +18765551234
sleep 3

adb shell dumpsys telecom > "$OUT/telecom.txt" 2>&1
adb logcat -d -v time -s Filtro:* AndroidRuntime:E > "$OUT/logcat-filtro.txt"

log "Notificaciones"
adb shell cmd statusbar expand-notifications
sleep 2
shot 08-notificaciones
adb shell cmd statusbar collapse
sleep 1

log "Pantallas"
adb shell am start -W -n $PKG/.MainActivity --es tab registro >/dev/null
sleep 3
shot 09-registro
tap_text "Internacional · Jamaica"
sleep 3
shot 10-detalle
adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell am start -W -n $PKG/.MainActivity --es tab reglas >/dev/null
sleep 3
shot 11-reglas
adb shell input swipe 540 1900 540 600 500
sleep 2
shot 12-reglas-medio
adb shell input swipe 540 1900 540 600 500
sleep 2
shot 13-reglas-abajo

log "Consultar un número seleccionado en otra app"
adb shell "am start -W -n $PKG/.MainActivity -a android.intent.action.PROCESS_TEXT -t text/plain --es android.intent.extra.PROCESS_TEXT 6006002020" >/dev/null
sleep 3
adb shell input swipe 540 1700 540 800 500
sleep 2
shot 14-consulta

log "Modo oscuro"
adb shell cmd uimode night yes
sleep 3
adb shell am start -W -n $PKG/.MainActivity --es tab inicio >/dev/null
sleep 3
shot 15-oscuro-inicio
call 452123456
sleep 6
shot 16-oscuro-identificador
hangup 452123456
sleep 2
adb shell cmd uimode night no

adb logcat -d -v time > "$OUT/logcat-completo.txt"
grep -n "FATAL EXCEPTION" -A 40 "$OUT/logcat-completo.txt" > "$OUT/errores.txt" || echo "sin errores" > "$OUT/errores.txt"
log "listo"
