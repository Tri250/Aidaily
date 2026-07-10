import os
root = '/workspace/LiveCaptureAndroid/app/src/main/java'
files = []
for d, _, fs in os.walk(root):
    for f in fs:
        if f.endswith('.kt'):
            files.append(os.path.join(d, f))

modifiers = {'internal','public','private','protected','abstract','sealed','open','data','enum','annotation','inline','value','expect','actual','external','suspend','operator','infix','tailrec','const'}

def get_package(text):
    for line in text.splitlines():
        if line.startswith('package '):
            return line[len('package '):].strip().rstrip(';')
    return ''

qualified = {}
for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    pkg = get_package(text)
    for line in text.splitlines():
        if not line or line[0].isspace():
            continue
        # strip leading annotations roughly
        s = line
        while s.startswith('@'):
            # skip annotation possibly with parentheses
            idx = s.find(')')
            if idx != -1:
                s = s[idx+1:].lstrip()
            else:
                s = s.split(None, 1)[1] if ' ' in s else ''
                s = s.lstrip()
        parts = s.split()
        # remove modifiers
        while parts and parts[0] in modifiers:
            parts = parts[1:]
        if len(parts) < 2:
            continue
        kw = parts[0]
        name = parts[1]
        if kw in ('class','object','interface','fun','val','var'):
            # remove generic params or backticks
            name = name.strip('`')
            if name.startswith('<') or not name[0].isalpha() and name[0] != '_':
                continue
            q = f'{pkg}.{name}' if pkg else name
            qualified[q] = path

import_re = None
issues = []
for path in files:
    with open(path, 'r', encoding='utf-8') as f:
        for i, line in enumerate(f, 1):
            s = line.strip()
            if not s.startswith('import '):
                continue
            imp = s[len('import '):].strip().rstrip(';')
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
