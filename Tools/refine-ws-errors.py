#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
p = ROOT / 'TMessagesProj/src/main/java/com/libregram/proxy/WsProxyService.java'
s = p.read_text()
changes = [
    ('else fail("native_start_rejected");', 'else fail(WsErrorClassifier.classify(failure));'),
    ('@Override public void onError(String message) { if (ownsNative) fail("native_transport_error"); }',
     '@Override public void onError(String message) { if (ownsNative) fail(message); }'),
]
for old, new in changes:
    if new in s: continue
    if s.count(old) != 1: raise ValueError('Unexpected service source; review error integration')
    s = s.replace(old, new, 1)
p.write_text(s)
print('Safe error classification connected to service.')
