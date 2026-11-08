#!/usr/bin/env python3
"""Normalize the existing Actions secret without logging its contents."""
import base64
import os
import re
import sys


def main():
    supplied = os.environ.get('INPUT_LOCAL_PROPERTIES', '').strip()
    if not supplied:
        raise ValueError('LOCAL_PROPERTIES secret is missing.')
    if re.search(r'^\s*TELEGRAM_APP_ID\s*[=:]', supplied, re.MULTILINE):
        text = supplied
    else:
        compact = ''.join(supplied.split())
        text = base64.b64decode(compact, validate=True).decode('utf-8-sig')
    values = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith(('#', '!')):
            continue
        match = re.match(r'([^:=\s]+)\s*[=:]\s*(.*)', line)
        if match:
            values[match[1]] = match[2].strip()
    if not re.fullmatch(r'[0-9]+', values.get('TELEGRAM_APP_ID', '')):
        raise ValueError('LOCAL_PROPERTIES requires numeric TELEGRAM_APP_ID.')
    if not re.fullmatch(r'[0-9a-fA-F]{32}', values.get('TELEGRAM_APP_HASH', '')):
        raise ValueError('LOCAL_PROPERTIES requires a 32-character TELEGRAM_APP_HASH.')
    # Gradle uses the Java basic Base64 decoder, so emit one canonical line.
    normalized = base64.b64encode(text.encode('utf-8')).decode('ascii')
    print('::add-mask::' + normalized)
    with open(os.environ['GITHUB_ENV'], 'a', encoding='utf-8') as stream:
        stream.write('LOCAL_PROPERTIES=' + normalized + '\n')
    print('Private Telegram properties validated and normalized; values hidden.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, UnicodeError):
        print('::error::LOCAL_PROPERTIES must contain plain properties or valid Base64 properties with TELEGRAM_APP_ID and TELEGRAM_APP_HASH. Values were not printed.')
        sys.exit(1)
