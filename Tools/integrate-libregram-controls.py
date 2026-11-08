#!/usr/bin/env python3
"""Apply narrow, idempotent integration edits to the large inherited Java sources.

Run before Gradle (the feature CI does this). Unexpected source changes fail closed,
not silently overwrite upstream code. Copyright and license headers remain intact.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def replace(source, before, after):
    if after in source:
        return source
    if source.count(before) != 1:
        raise ValueError('Integration anchor missing or ambiguous; review upstream diff.')
    return source.replace(before, after, 1)


def transform_dialogs(source):
    return replace(source,
        '        final boolean isCurrentThemeDark;\n        final boolean hideBottomNavigationBar',
        '        com.libregram.proxy.WsProxyMenu.addTo(io, this);\n\n        final boolean isCurrentThemeDark;\n        final boolean hideBottomNavigationBar')


def transform_settings(source):
    source = replace(source,
        '    private static final String HOST_EXTERALESS = "exteraless";',
        '    private static final String HOST_EXTERALESS = "exteraless";\n    private static final String HOST_LIBREGRAM = "libregramm";')
    source = replace(source,
        '"ayumoments", "navigation", "menus", "media", "hiding"));',
        '"ayumoments", "navigation", "menus", "media", "hiding", "wsproxy"));')
    source = replace(source,
        '''        if (!path.startsWith(HOST_EXTERALESS + "/")) {
            return false;
        }
        return EXTERALESS_SCREENS.contains(path.substring(HOST_EXTERALESS.length() + 1));''',
        '''        int slash = path.indexOf('/');
        if (slash < 0 || !isClientHost(path.substring(0, slash))) return false;
        return EXTERALESS_SCREENS.contains(path.substring(slash + 1));''')
    source = replace(source,
        '    public static String linkPathFor(String key) {',
        '''    private static boolean isClientHost(String host) {
        return HOST_EXTERALESS.equalsIgnoreCase(host)
                || HOST_LIBREGRAM.equalsIgnoreCase(host)
                || "libregram".equalsIgnoreCase(host);
    }

    public static String linkPathFor(String key) {''')
    source = source.replace('return HOST_EXTERALESS + "/', 'return HOST_LIBREGRAM + "/')
    source = replace(source,
        '        final boolean exteraless = HOST_EXTERALESS.equals(segments.get(0));',
        '        final boolean exteraless = isClientHost(segments.get(0));')
    source = replace(source,
        '''                case "settings":
                    fragment = neko_fragment = new OpenExteraSettingsActivity();''',
        '''                case "wsproxy":
                    fragment = neko_fragment = new app.exteraless.settings.LibreGramWsProxySettingsActivity();
                    break;
                case "settings":
                    fragment = neko_fragment = new OpenExteraSettingsActivity();''')
    source = replace(source,
        '        exteralessFragments.add(new OpenExteraSettingsActivity());',
        '        exteralessFragments.add(new OpenExteraSettingsActivity());\n        exteralessFragments.add(new app.exteraless.settings.LibreGramWsProxySettingsActivity());')
    source = replace(source,
        '            case "exteraless":\n                return HOST_LIBREGRAM + "/settings";',
        '            case "libregramWsProxy":\n                return HOST_LIBREGRAM + "/wsproxy";\n            case "exteraless":\n                return HOST_LIBREGRAM + "/settings";')
    return source


def main():
    transforms = {
        'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java': transform_dialogs,
        'TMessagesProj/src/main/java/tw/nekomimi/nekogram/helpers/SettingsHelper.java': transform_settings,
    }
    # Validate every transformation before writing any files.
    updates = []
    for relative, transform in transforms.items():
        path = ROOT / relative
        original = path.read_text(encoding='utf-8')
        updated = transform(original)
        if transform(updated) != updated:
            raise ValueError('Integration is not idempotent.')
        updates.append((path, updated))
    for path, updated in updates:
        path.write_text(updated, encoding='utf-8')
    print('LibreGram menu and compatible settings links integrated.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
