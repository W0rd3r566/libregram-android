#!/usr/bin/env python3
"""Integrate opt-in startup and settings row, then the existing quick controls."""
from pathlib import Path
import runpy

ROOT = Path(__file__).resolve().parents[1]
module = runpy.run_path(str(ROOT / 'Tools/integrate-libregram-controls.py'))
replace = module['replace']

def startup(source):
    return replace(source, '        new ForegroundDetector(this) {',
        '        com.libregram.proxy.WsProxyAutoStart.register(this);\n        new ForegroundDetector(this) {')

def settings(source):
    edits = [
        ('private int headerRow, enabledRow, statusRow, infoRow, connectRow, tcpRow;',
         'private int headerRow, enabledRow, statusRow, infoRow, connectRow, tcpRow, autoStartRow;'),
        ('        connectRow = addRow("libregramWsConnect");',
         '        autoStartRow = addRow("libregramWsAutoStart");\n        connectRow = addRow("libregramWsConnect");'),
        ('        } else if (!idle()) {',
         '        } else if (position == autoStartRow) {\n            prefs().edit().putBoolean("auto_start", !prefs().getBoolean("auto_start", false)).apply();\n            observer.run();\n        } else if (!idle()) {'),
        ('if (position == enabledRow || position == connectRow || position == tcpRow) return TYPE_CHECK;',
         'if (position == enabledRow || position == connectRow || position == tcpRow || position == autoStartRow) return TYPE_CHECK;'),
        ('return p == enabledRow || p == connectRow || p == tcpRow || p == portRow',
         'return p == autoStartRow || p == enabledRow || p == connectRow || p == tcpRow || p == portRow'),
        ('getString(position == enabledRow ? R.string.LibreGramWsEnable',
         'getString(position == autoStartRow ? R.string.LibreGramWsAutoStart : position == enabledRow ? R.string.LibreGramWsEnable'),
        ('position == enabledRow ? enabled : prefs().getBoolean(',
         'position == autoStartRow ? prefs().getBoolean("auto_start", false) : position == enabledRow ? enabled : prefs().getBoolean('),
    ]
    for before, after in edits:
        source = replace(source, before, after)
    return source

updates = []
for relative, transform in [
    ('TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java', startup),
    ('TMessagesProj/src/main/java/app/exteraless/settings/LibreGramWsProxySettingsActivity.java', settings),
]:
    path = ROOT / relative
    result = transform(path.read_text(encoding='utf-8'))
    if transform(result) != result:
        raise ValueError('Startup integration is not idempotent')
    updates.append((path, result))
module['main']()
for path, result in updates:
    path.write_text(result, encoding='utf-8')
print('Opt-in WS startup integrated.')
