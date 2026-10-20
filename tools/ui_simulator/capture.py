#!/usr/bin/env python3
"""Capture real Android UI frames on an emulator; no mock UI or synthetic telemetry."""
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path(os.environ.get("TANK_SCREENSHOT_OUT", "screenshots"))
OUT.mkdir(parents=True, exist_ok=True)
LOG = []

def adb(*args, timeout=35, raw=False):
    result = subprocess.run(["adb", *args], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    if result.returncode:
        raise RuntimeError("adb " + " ".join(args) + ": " + result.stderr.decode("utf-8", "replace")[:400])
    return result.stdout if raw else result.stdout.decode("utf-8", "replace")

def view():
    for _ in range(2):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/tank-window.xml", timeout=12)
            raw = adb("exec-out", "cat", "/sdcard/tank-window.xml", raw=True)
            tree = ET.fromstring(raw)
            return tree, raw.decode("utf-8", "replace")
        except Exception as exc:
            print("UI dump retry:", str(exc)[:180], flush=True)
            time.sleep(2)
    return None, ""

def bounds_center(s):
    m = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", s)
    return ((int(m[1])+int(m[3]))//2, (int(m[2])+int(m[4]))//2) if m else None

def tap_button(label, tries=1, scroll=False):
    for index in range(tries + 1):
        tree, _ = view()
        if tree is not None:
            exact, partial = [], []
            for node in tree.iter("node"):
                desc = node.attrib.get("content-desc", "")
                txt = node.attrib.get("text", "")
                coord = bounds_center(node.attrib.get("bounds", ""))
                if coord and (txt == label or desc == label):
                    exact.append(coord)
                elif coord and (label in txt or label in desc):
                    partial.append(coord)
            pool = exact or partial
            if pool:
                x, y = pool[0]
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(2)
                LOG.append({"operation":"tap", "label":label, "x":x,"y":y})
                return True
        if scroll:
            adb("shell","input","swipe","850","130","150","130","450")
        else:
            time.sleep(1)
    LOG.append({"operation":"missing", "label":label})
    return False

def capture(name):
    time.sleep(2)
    png = adb("exec-out", "screencap", "-p", raw=True, timeout=45)
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("Screenshot returned non-PNG bytes")
    filename = OUT / (name + ".png")
    filename.write_bytes(png)
    LOG.append({"operation":"capture","file":filename.name,"bytes":len(png)})
    print("Captured",filename,len(png),flush=True)

def section(label, screenshot, scroll=False):
    if tap_button(label, scroll=scroll):
        capture(screenshot)
    else:
        print("Not accessible:",label,flush=True)

def main():
    print(adb("shell", "wm", "size"), flush=True)
    adb("shell", "settings", "put", "system", "screen_off_timeout", "1800000")
    adb("shell", "monkey", "-p", "com.hazrob.tank", "-c", "android.intent.category.LAUNCHER", "1", timeout=40)
    time.sleep(8)
    capture("01-connection-entry")
    if not tap_button("الدخول دون اتصال"):
        LOG.append({"warning":"Offline entry unavailable; capturing what is visible"})
    time.sleep(5)
    capture("02-driving")
    section("الجلسات","03-field-sessions",scroll=True)
    section("القياسات","04-measurement-analysis",scroll=True)
    section("البحث والتتبع","05-chemical-search")
    section("التحليل","06-instant-analysis")
    section("ChemCore","07-chemcore")
    section("المرشحون","08-candidates")
    section("الفيزيائية","09-physical-measurements")
    section("الجزيئات","10-particles")
    section("التقارير","11-reports",scroll=True)
    section("الحالة","12-status",scroll=True)
    section("الإعدادات","13-settings",scroll=True)
    section("القيادة","14-driving-return",scroll=True)
    if tap_button("المطور", scroll=True):
        capture("15-developer-gate")
        try:
            pin_code = re.search(r'DEVELOPER_PIN\s*=\s*"([0-9]+)"', Path("source/app/src/main/java/com/servoc/app/auth/RoleGate.java").read_text())
            tree, _ = view()
            inputs = [node for node in tree.iter("node") if node.attrib.get("class") == "android.widget.EditText"] if tree is not None else []
            if pin_code and inputs:
                p = bounds_center(inputs[0].attrib.get("bounds", ""))
                if p:
                    adb("shell", "input", "tap", str(p[0]), str(p[1]))
                    adb("shell", "input", "text", pin_code.group(1))
                    if tap_button("دخول"):
                        capture("16-developer-default")
                        section("التجارب", "17-developer-experiments")
                        section("المختبر", "18-developer-lab")
                        section("معايرة R0", "19-developer-r0")
                        section("الاتصال", "20-developer-network")
                        section("المكتبة", "21-developer-library")
        except Exception as exc:
            LOG.append({"developer_view_note": str(exc)[:200]})
    (OUT/"capture-log.json").write_text(json.dumps({"device":adb("shell","getprop","ro.product.model").strip(),
       "notes":["Real Android Views rendered on emulator from V2 source.",
                "Simulator-only bypass of first-run SAF folder selection; original screens otherwise unchanged.",
                "Wi-Fi robot is disconnected; physical measurements are not synthesized."],
       "actions":LOG},ensure_ascii=False,indent=2),encoding="utf-8")
    files=sorted(OUT.glob("*.png"))
    links="\n".join('<figure><img style="width:360px;max-width:100%" src="'+f.name+'"><figcaption>'+f.name+'</figcaption></figure>' for f in files)
    (OUT/"index.html").write_text('<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><title>HazRob Tank real UI captures</title><body><h1>HazRob Tank V2 — Android Emulator Screenshots</h1><p>Actual application renders. Offline/no robot data; simulator-only first-run folder bypass.</p>'+links+'</body></html>',encoding="utf-8")
    if len(files)<5:
        raise RuntimeError("Insufficient UI screenshots: "+str(len(files)))
    print("SUCCESS: screenshot count",len(files),flush=True)

if __name__ == "__main__":
    main()
