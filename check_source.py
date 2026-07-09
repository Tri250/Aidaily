#!/usr/bin/env python3
"""Final check - remaining potential issues in source code."""
import os, re

base = "/workspace/LiveCaptureAndroid/app/src/main/java"

# Check for @Composable in non-@Composable functions
# Check for missing resources
# Check for potential NPE

issues = []

# Walk through all .kt files
for root, dirs, files in os.walk(base):
    for f in files:
        if not f.endswith(".kt"):
            continue
        path = os.path.join(root, f)
        with open(path, "r") as fh:
            content = fh.read()
            # Check for !!. that could NPE
            for match in re.finditer(r"(\w+!!)", content):
                issues.append(f"{path}: potential NPE: {match.group(1)}")
            # Check for System.loadLibrary
            for match in re.finditer(r"System\.load(?:Library)?\(", content):
                issues.append(f"{path}: System.load call: {match.group(0)}")
            # Check for R. references that might be missing
            for match in re.finditer(r"R\.(drawable|raw|string|color|dimen)\.(\w+)", content):
                if "R.drawable" in match.group(0) or "R.raw" in match.group(0):
                    res_name = match.group(2)
                    # Check if resource exists
                    res_path = os.path.join(os.path.dirname(base), "res")
                    found = False
                    for r_root, _, r_files in os.walk(res_path):
                        for rf in r_files:
                            if rf.startswith(res_name) or f"{res_name}." in rf:
                                found = True
                                break
                    if not found:
                        issues.append(f"{path}: potentially missing resource: {match.group(0)}")

if issues:
    print("POTENTIAL ISSUES FOUND:")
    for i in issues:
        print(f"  {i}")
else:
    print("No additional issues found in source code analysis.")

print(f"\nTotal files scanned: {sum(1 for _ in os.walk(base) for __ in _[2] if __.endswith('.kt'))}")