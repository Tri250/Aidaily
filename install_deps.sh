#!/bin/bash

install_artifact() {
    local group_id="$1"
    local artifact_id="$2"  
    local version="$3"
    # Safety guard: skip if any argument is empty
    [ -z "$group_id" ] || [ -z "$artifact_id" ] || [ -z "$version" ] && return 1
    local group_path=$(echo "$group_id" | tr '.' '/')
    local dir="$HOME/.m2/repository/$group_path/$artifact_id/$version"
    # Safety guard: dir must be at least 4 levels deep under repository
    local reldir="${dir#$HOME/.m2/repository/}"
    local depth=$(echo "$reldir" | tr -cd '/' | wc -c)
    [ "$depth" -lt 2 ] && return 1
    
    if [ -f "$dir/$artifact_id-$version.pom" ]; then
        return 0
    fi
    
    mkdir -p "$dir"
    
    for base_url in "https://repo1.maven.org/maven2" "https://dl.google.com/dl/android/maven2"; do
        local url="$base_url/$group_path/$artifact_id/$version/$artifact_id-$version.pom"
        if curl -sL --connect-timeout 10 --max-time 30 -o "$dir/$artifact_id-$version.pom" "$url" 2>/dev/null; then
            if [ -s "$dir/$artifact_id-$version.pom" ] && ! grep -q "<!DOCTYPE" "$dir/$artifact_id-$version.pom" 2>/dev/null && ! grep -q "Not Found" "$dir/$artifact_id-$version.pom" 2>/dev/null; then
                curl -sL --connect-timeout 10 --max-time 60 -o "$dir/$artifact_id-$version.jar" "$base_url/$group_path/$artifact_id/$version/$artifact_id-$version.jar" 2>/dev/null || true
                curl -sL --connect-timeout 10 --max-time 30 -o "$dir/$artifact_id-$version.module" "$base_url/$group_path/$artifact_id/$version/$artifact_id-$version.module" 2>/dev/null || true
                return 0
            fi
        fi
    done
    
    # Safety guard: only remove if dir is deep enough (prevents wiping parent dirs)
    [ "$depth" -ge 2 ] && rm -rf "$dir" 2>/dev/null
    return 1
}

echo "Phase 1: Scanning all POMs for missing dependencies..."
count=0
touch /tmp/.scan_marker
find $HOME/.m2/repository -name "*.pom" -not -name "original-*" | while read pom; do
    python3 -c "
import xml.etree.ElementTree as ET
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
try:
    tree = ET.parse('$pom')
    root = tree.getroot()
    parent = root.find('m:parent', ns)
    if parent is not None:
        pg = parent.find('m:groupId', ns)
        pa = parent.find('m:artifactId', ns)
        pv = parent.find('m:version', ns)
        if pg is not None and pa is not None and pv is not None:
            print(f'{pg.text}:{pa.text}:{pv.text}')
    for dep in root.findall('.//m:dependency', ns):
        g = dep.find('m:groupId', ns)
        a = dep.find('m:artifactId', ns)
        v = dep.find('m:version', ns)
        s = dep.find('m:scope', ns)
        if s is not None and s.text in ('test', 'provided'):
            continue
        if g is not None and a is not None and v is not None and '\${' not in v.text:
            print(f'{g.text}:{a.text}:{v.text}')
except:
    pass
" 2>/dev/null | while IFS=: read gid aid ver; do
        install_artifact "$gid" "$aid" "$ver"
    done
done

echo "Phase 2: Run again to catch newly added POM deps..."
find $HOME/.m2/repository -name "*.pom" -not -name "original-*" -newer /tmp/.scan_marker 2>/dev/null | while read pom; do
    python3 -c "
import xml.etree.ElementTree as ET
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
try:
    tree = ET.parse('$pom')
    root = tree.getroot()
    parent = root.find('m:parent', ns)
    if parent is not None:
        pg = parent.find('m:groupId', ns)
        pa = parent.find('m:artifactId', ns)  
        pv = parent.find('m:version', ns)
        if pg is not None and pa is not None and pv is not None:
            print(f'{pg.text}:{pa.text}:{pv.text}')
    for dep in root.findall('.//m:dependency', ns):
        g = dep.find('m:groupId', ns)
        a = dep.find('m:artifactId', ns)
        v = dep.find('m:version', ns)
        s = dep.find('m:scope', ns)
        if s is not None and s.text in ('test', 'provided'):
            continue
        if g is not None and a is not None and v is not None and '\${' not in v.text:
            print(f'{g.text}:{a.text}:{v.text}')
except:
    pass
" 2>/dev/null | while IFS=: read gid aid ver; do
        install_artifact "$gid" "$aid" "$ver"
    done
done

echo "Final counts:"
echo "Total POM files: $(find $HOME/.m2/repository -name '*.pom' | wc -l)"
echo "Total JAR files: $(find $HOME/.m2/repository -name '*.jar' | wc -l)"
