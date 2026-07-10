import re, os
root = '/workspace/LiveCaptureAndroid/app/src/main/java'
files = []
for d, _, fs in os.walk(root):
    for f in fs:
        if f.endswith('.kt'):
            files.append(os.path.join(d, f))

pkg_re = re.compile(r'^package\s+([\w.]+)')
# top-level name after class/object/interface/fun/val/var, ignoring annotations/modifiers
name_re = re.compile(r'^(?:[^\w\s]*\s*)*(?:class|object|interface|fun|val|var)\s+(<|([A-Za-z_]\w*))')

qualified = {}
for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    pkg_m = pkg_re.search(text)
    pkg = pkg_m.group(1) if pkg_m else ''
    for line in text.splitlines():
        if not line or line[0].isspace():
            continue
        m = name_re.match(line)
        if not m:
            continue
        name = m.group(2)
        if not name:
            continue
        q = f'{pkg}.{name}' if pkg else name
        qualified[q] = path

import_re = re.compile(r'^import\s+([\w.*]+)')
issues = []
for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        for i, line in enumerate(f, 1):
            m = import_re.match(line)
            if not m:
                continue
            imp = m.group(1)
            if imp.startswith('com.livecompose.livecapture.'):
                if imp.endswith('.*'):
                    base = imp[:-2]
                    if not any(q.startswith(base + '.') for q in qualified):
                        issues.append((path, i, imp, 'star import no matching package'))
                else:
                    if imp not in qualified:
                        parent = '.'.join(imp.split('.')[:-1])
                        if parent not in qualified:
                            issues.append((path, i, imp, 'unresolved internal import'))

print(f'Total files: {len(files)}')
print(f'Internal declarations: {len(qualified)}')
print(f'Internal import issues: {len(issues)}')
for path, line, imp, msg in issues[:300]:
    print(f'{path}:{line}  {imp}  -> {msg}')
