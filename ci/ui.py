"""Toca en pantalla el elemento que tenga cierto texto (usa uiautomator del emulador)."""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def dump():
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
    out = subprocess.run(["adb", "shell", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout
    start = out.find("<?xml")
    return ET.fromstring(out[start:] if start >= 0 else out)


def find(root, text):
    nodes = list(root.iter("node"))
    for n in nodes:
        if n.get("text") == text or n.get("content-desc") == text:
            return n
    for n in nodes:
        if text in (n.get("text") or ""):
            return n
    return None


def main():
    action, text = sys.argv[1], sys.argv[2]
    node = find(dump(), text)
    if node is None:
        print("no encontrado:", text)
        sys.exit(1)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    if action == "tap":
        subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
    print("ok:", text, node.get("bounds"))


if __name__ == "__main__":
    main()
