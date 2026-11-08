#!/usr/bin/env python3
"""Validate LibreGram application identity and launcher resources."""
from __future__ import annotations
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'
RES = ROOT / 'TMessagesProj/src/main/res'
EXPECTED_LABELS = {'LibreGram', 'Graphite', 'Ocean', 'Midnight', 'Mint', 'Violet', 'Rose', 'Ember', 'Silver', 'Cobalt', 'Crimson', 'Lime', 'Sand', 'Iris', 'Ice', 'Mono', 'Night'}

def require(condition, message):
    if not condition:
        raise AssertionError(message)

def main():
    application = ET.parse(ROOT / 'TMessagesProj/src/main/AndroidManifest.xml').getroot().find('application')
    require(application is not None, 'Manifest has no application')
    label = application.get(ANDROID + 'label', '')
    require(label in {'@string/AppName', '@string/NagramX'}, 'Application label must reference a validated LibreGram string')
    require(application.get(ANDROID + 'icon') == '@drawable/libregram_icon_original', 'Application must use LibreGram Original icon')
    aliases = []
    for alias in application.findall('activity-alias'):
        categories = {c.get(ANDROID + 'name') for f in alias.findall('intent-filter') for c in f.findall('category')}
        if 'android.intent.category.LAUNCHER' in categories:
            aliases.append(alias)
    require(aliases, 'No launcher aliases')
    icons = set()
    for alias in aliases:
        name = alias.get(ANDROID + 'label', '')
        icon = alias.get(ANDROID + 'icon', '')
        require(name in EXPECTED_LABELS, f'Unexpected launcher label: {name!r}')
        require(icon.startswith('@drawable/libregram_icon_'), f'Non-LibreGram launcher icon: {icon!r}')
        resource = icon.removeprefix('@drawable/')
        ET.parse(RES / 'drawable' / (resource + '.xml'))
        icons.add(resource)
    for filename, name in [('strings.xml', 'AppName'), ('strings_nax.xml', 'NagramX')]:
        node = ET.parse(RES / 'values' / filename).getroot().find(f"string[@name='{name}']")
        require(node is not None and (node.text or '').strip() == 'LibreGram', f'{name} must be LibreGram')
    require('rootProject.name = "LibreGram"' in (ROOT / 'settings.gradle').read_text(), 'Gradle project must be LibreGram')
    print(f'LibreGram branding OK: {len(aliases)} aliases, {len(icons)} icons')
    return 0

if __name__ == '__main__':
    try:
        sys.exit(main())
    except (AssertionError, ET.ParseError, OSError) as exc:
        print(f'Branding validation failed: {exc}', file=sys.stderr)
        sys.exit(1)
