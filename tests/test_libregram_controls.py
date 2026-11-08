#!/usr/bin/env python3
"""Host checks; these do not compile Java or replace Android device tests."""
import runpy
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INTEGRATION = runpy.run_path(str(ROOT / 'Tools/integrate-libregram-controls.py'))


class IntegrationTests(unittest.TestCase):
    def test_dialogs_patch_is_idempotent(self):
        path = ROOT / 'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'
        transform = INTEGRATION['transform_dialogs']
        result = transform(path.read_text())
        self.assertEqual(transform(result), result)
        self.assertEqual(result.count('com.libregram.proxy.WsProxyMenu.addTo(io, this);'), 1)

    def test_links_preserve_legacy_and_emit_libregram(self):
        path = ROOT / 'TMessagesProj/src/main/java/tw/nekomimi/nekogram/helpers/SettingsHelper.java'
        transform = INTEGRATION['transform_settings']
        result = transform(path.read_text())
        self.assertEqual(transform(result), result)
        self.assertIn('HOST_EXTERALESS.equalsIgnoreCase(host)', result)
        self.assertIn('HOST_LIBREGRAM.equalsIgnoreCase(host)', result)
        self.assertNotIn('return HOST_EXTERALESS + "/', result)
        self.assertIn('return HOST_LIBREGRAM + "/settings";', result)
        self.assertIn('case "wsproxy":', result)

    def test_menu_callback_order(self):
        source = (ROOT / 'TMessagesProj/src/main/java/com/libregram/proxy/WsProxyMenu.java').read_text()
        self.assertLess(source.index('() -> fragment.presentFragment(new LibreGramWsProxySettingsActivity())'),
                        source.index('() -> toggle(fragment)'))


if __name__ == '__main__':
    unittest.main()
