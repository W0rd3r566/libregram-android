#!/usr/bin/env python3
"""Validate the LibreGram app identity and launcher icon resources."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"
RES = ROOT / "TMessagesProj" / "src" / "main" / "res"
MANIFEST = ROOT / "TMessagesProj" / "src" / "main" / "AndroidManifest.xml"

EXPECTED_LABELS = {
    "LibreGram", "Graphite", "Ocean", "Midnight", "Mint", "Violet",
    "Rose", "Ember", "Silver", "Cobalt", "Crimson", "Lime",
    "Sand", "Iris", "Ice", "Mono", "Night",
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    manifest = ET.parse(MANIFEST).getroot()
    application = manifest.find("application")
    require(application is not None, "AndroidManifest.xml has no application element")

    require(
        application.get(ANDROID + "label") == "@string/NagramX",
        "Application label must resolve through @string/NagramX",
    )
    require(
        application.get(ANDROID + "icon") == "@drawable/libregram_icon_original",
        "Application icon must use the LibreGram Original vector",
    )

    aliases = []
    for alias in application.findall("activity-alias"):
        categories = {
            item.get(ANDROID + "name")
            for intent_filter in alias.findall("intent-filter")
            for item in intent_filter.findall("category")
        }
        if "android.intent.category.LAUNCHER" in categories:
            aliases.append(alias)

    require(bool(aliases), "No launcher activity-alias entries were found")

    checked_icons: set[str] = set()
    for alias in aliases:
        label = alias.get(ANDROID + "label", "")
        icon = alias.get(ANDROID + "icon", "")
        require(label in EXPECTED_LABELS, f"Unexpected launcher icon label: {label!r}")
        require(
            icon.startswith("@drawable/libregram_icon_"),
            f"Launcher alias {label!r} still uses a non-LibreGram icon: {icon!r}",
        )
        resource_name = icon.removeprefix("@drawable/")
        resource_file = RES / "drawable" / f"{resource_name}.xml"
        require(resource_file.is_file(), f"Missing vector drawable: {resource_file}")
        ET.parse(resource_file)
        checked_icons.add(resource_name)

    strings_nax = ET.parse(RES / "values" / "strings_nax.xml").getroot()
    nagram_x = strings_nax.find("string[@name='NagramX']")
    require(nagram_x is not None and (nagram_x.text or "").strip() == "LibreGram",
            "NagramX launcher label must be LibreGram")

    strings = ET.parse(RES / "values" / "strings.xml").getroot()
    app_name = strings.find("string[@name='AppName']")
    require(app_name is not None and (app_name.text or "").strip() == "LibreGram",
            "AppName must be LibreGram")

    settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
    require('rootProject.name = "LibreGram"' in settings,
            "Gradle root project name must be LibreGram")

    print(
        f"LibreGram branding OK: {len(aliases)} launcher aliases, "
        f"{len(checked_icons)} distinct vector icons."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, ET.ParseError, OSError) as exc:
        print(f"Branding validation failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
