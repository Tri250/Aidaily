import os, re
root = '/workspace/LiveCaptureAndroid/app/src/main/java'
files = []
for d, _, fs in os.walk(root):
    for f in fs:
        if f.endswith('.kt'):
            files.append(os.path.join(d, f))

# Match top-level declarations: optional annotations/modifiers then keyword then name
# Capture name group 3
decl_re = re.compile(
    r'^(?:\s*(?:@[\w.]+(?:\([^)]*\))?\s*)|(?:\s*(?:internal|public|private|protected|abstract|sealed|open|data|enum|annotation|inline|value|expect|actual|external|suspend|operator|infix|tailrec|const)\s+))*\s*'
    r'(class|object|interface|fun|val|var)\s+'
    r'(`?)([A-Za-z_]\w*)\2'
)

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
        m = decl_re.match(line)
        if not m:
            continue
        name = m.group(3)
        q = f'{pkg}.{name}' if pkg else name
        qualified[q] = path

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
                    # strip alias
                    name_imp = imp.split(' as ')[0].strip()
                    if name_imp not in qualified:
                        parent = '.'.join(name_imp.split('.')[:-1])
                        if parent not in qualified:
                            issues.append((path, i, imp, 'unresolved internal import'))

print(f'Total files: {len(files)}')
print(f'Internal declarations: {len(qualified)}')
print(f'Internal import issues: {len(issues)}')
for path, line, imp, msg in issues[:400]:
    print(f'{path}:{line}  {imp}  -> {msg}')
