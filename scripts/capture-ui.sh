#!/usr/bin/env bash
set -euo pipefail
mkdir -p ui-preview
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell input keyevent 82
adb shell cmd uimode night no
adb shell am start -W -n com.example.chromebridge/.MainActivity
sleep 3
adb exec-out screencap -p > ui-preview/personal-light.png
adb shell uiautomator dump /sdcard/window.xml
adb pull /sdcard/window.xml ui-preview/personal-light.xml
# Locate and tap the actual App info button rather than launching Settings ourselves.
python3 - <<'PY'
import re,subprocess,xml.etree.ElementTree as E
root=E.parse('ui-preview/personal-light.xml')
for node in root.iter('node'):
    if node.get('text') == 'Open Chrome Bridge app info':
        a,b,c,d=map(int,re.findall(r'\d+',node.get('bounds')))
        subprocess.run(['adb','shell','input','tap',str((a+c)//2),str((b+d)//2)],check=True)
        break
else:
    raise RuntimeError('App info shortcut not visible')
PY
sleep 2
adb shell dumpsys activity activities > ui-preview/app-info-activity.txt
adb exec-out screencap -p > ui-preview/app-info.png
adb shell input keyevent 4
sleep 1
adb shell input swipe 500 1600 500 500 500
sleep 1
adb exec-out screencap -p > ui-preview/personal-steps.png
adb shell am force-stop com.example.chromebridge
adb shell cmd uimode night yes
adb shell am start -W -n com.example.chromebridge/.MainActivity
sleep 2
adb exec-out screencap -p > ui-preview/personal-dark.png
adb logcat -d -s AndroidRuntime:E > ui-preview/app-errors.txt
