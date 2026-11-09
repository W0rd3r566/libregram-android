#!/usr/bin/env python3
"""Targeted, idempotent branding corrections; preserve legacy keys and attribution."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'TMessagesProj/src/main/res'


def replace_once(source, before, after):
    if after in source:
        return source
    if source.count(before) != 1:
        raise ValueError('Branding source anchor missing or ambiguous')
    return source.replace(before, after, 1)


def main():
    updates = {}
    # Exact UI keys only; do not rewrite licenses, credits, URLs or saved keys.
    keys = {'OpenExteraPreferences', 'AppIconExteraless'}
    for path in RES.glob('values*/strings_oe.xml'):
        source = path.read_text(encoding='utf-8')
        def change(match):
            if match.group(2) not in keys:
                return match.group(0)
            value = re.sub('exteraless', 'LibreGram', match.group(3), flags=re.I)
            return match.group(1) + value + match.group(4)
        updates[path] = re.sub(r'(<string\b[^>]*name="([^"]+)"[^>]*>)(.*?)(</string>)', change, source, flags=re.S)
    path = ROOT / 'TMessagesProj/src/main/java/app/exteraless/appicons/AppIcons.java'
    source = path.read_text(encoding='utf-8')
    source = replace_once(source,
        'int bleed = size / 4;',
        'int bleed = 0; // LibreGram preview assets are complete icons, not adaptive layers.')
    updates[path] = source
    path = ROOT / 'TMessagesProj/src/main/java/org/telegram/ui/LauncherIconController.java'
    source = path.read_text(encoding='utf-8')
    source = replace_once(source,
        'EXTERALESS("ExteralessIcon", R.drawable.exteraless_icon_background, R.drawable.exteraless_icon_foreground, R.string.AppIconExteraless)',
        'EXTERALESS("ExteralessIcon", R.drawable.libregram_icon_original, R.drawable.app_icon_empty_foreground, R.string.AppIconExteraless)')
    updates[path] = source
    path = ROOT / 'TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java'
    source = path.read_text(encoding='utf-8')
    before = '''        try {
            return ApplicationLoader.applicationContext.getString(R.string.OpenExtera);
        } catch (Exception ignore) {
            return "exteraless";
        }'''
    source = replace_once(source, before, '        return "LibreGram"; // Stable app identity, independent of locale or startup timing.')
    updates[path] = source
    # Keep the intro renderer/animations, replacing only its two-layer legacy logo.
    path = ROOT / 'TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java'
    source = path.read_text(encoding='utf-8')
    source = replace_once(source,
        'for (int resId : new int[]{R.drawable.exteraless_icon_background, R.drawable.exteraless_icon_foreground}) {',
        'for (int resId : new int[]{R.drawable.libregram_icon_original}) {')
    source = replace_once(source,
        'layer.setBounds(-bleed, -bleed, size + bleed, size + bleed);',
        'layer.setBounds(0, 0, size, size);')
    updates[path] = source
    for path, source in updates.items():
        path.write_text(source, encoding='utf-8')
    print('LibreGram branding: full-vector previews, intro and stable app identity updated.')


if __name__ == '__main__':
    main()
