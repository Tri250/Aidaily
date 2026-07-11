#!/bin/bash
# Second pass: handle failed downloads and do comprehensive final scan

M2REPO="/root/.m2/repository"
CENTRAL="https://repo1.maven.org/maven2"
GOOGLE="https://dl.google.com/dl/android/maven2"
COUNT_FILE="/workspace/install_count_pass2.txt"
echo "0" > "$COUNT_FILE"

install_count=0

install_artifact() {
    local groupId="$1"
    local artifactId="$2"
    local version="$3"
    local repo="${4:-$CENTRAL}"
    local ext="${5:-jar}"
    
    local groupPath=$(echo "$groupId" | tr '.' '/')
    local dir="$M2REPO/$groupPath/$artifactId/$version"
    local baseName="$artifactId-$version"
    
    local pomFile="$dir/$baseName.pom"
    if [ ! -f "$pomFile" ]; then
        mkdir -p "$dir"
        local pomUrl="$repo/$groupPath/$artifactId/$version/$baseName.pom"
        if curl -sSf --connect-timeout 10 --max-time 30 -o "$pomFile" "$pomUrl" 2>/dev/null; then
            echo "  [POM] $groupId:$artifactId:$version"
            install_count=$((install_count + 1))
        else
            if [ "$repo" = "$CENTRAL" ]; then
                local googleUrl="$GOOGLE/$groupPath/$artifactId/$version/$baseName.pom"
                if curl -sSf --connect-timeout 10 --max-time 30 -o "$pomFile" "$googleUrl" 2>/dev/null; then
                    echo "  [POM-GOOGLE] $groupId:$artifactId:$version"
                    install_count=$((install_count + 1))
                else
                    rm -f "$pomFile"
                    echo "  [POM-FAIL] $groupId:$artifactId:$version"
                fi
            else
                rm -f "$pomFile"
                echo "  [POM-FAIL] $groupId:$artifactId:$version"
            fi
        fi
    fi
    
    if [ "$ext" = "jar" ]; then
        local jarFile="$dir/$baseName.jar"
        if [ ! -f "$jarFile" ]; then
            local jarUrl="$repo/$groupPath/$artifactId/$version/$baseName.jar"
            if curl -sSf --connect-timeout 10 --max-time 30 -o "$jarFile" "$jarUrl" 2>/dev/null; then
                echo "  [JAR] $groupId:$artifactId:$version"
                install_count=$((install_count + 1))
            else
                if [ "$repo" = "$CENTRAL" ]; then
                    local googleUrl="$GOOGLE/$groupPath/$artifactId/$version/$baseName.jar"
                    if curl -sSf --connect-timeout 10 --max-time 30 -o "$jarFile" "$googleUrl" 2>/dev/null; then
                        echo "  [JAR-GOOGLE] $groupId:$artifactId:$version"
                        install_count=$((install_count + 1))
                    else
                        rm -f "$jarFile"
                    fi
                else
                    rm -f "$jarFile"
                fi
            fi
        fi
    fi
    
    echo "$install_count" > "$COUNT_FILE"
}

install_pom_only() {
    install_artifact "$1" "$2" "$3" "${4:-$CENTRAL}" "pom"
}

install_pom_and_jar() {
    install_artifact "$1" "$2" "$3" "${4:-$CENTRAL}" "jar"
}

scan_and_install_missing_parents() {
    local pass_num="$1"
    local found=0
    
    echo "  Pass $pass_num: Scanning all POMs..."
    while IFS= read -r -d '' pomFile; do
        local parentSection=""
        local inParent=false
        
        while IFS= read -r line; do
            if echo "$line" | grep -q "<parent>"; then
                inParent=true
                parentSection="$line"
            elif [ "$inParent" = true ]; then
                parentSection="$parentSection"$'\n'"$line"
                if echo "$line" | grep -q "</parent>"; then
                    inParent=false
                    local pGroupId=$(echo "$parentSection" | grep -oP '(?<=<groupId>)[^<]+' | head -1)
                    local pArtifactId=$(echo "$parentSection" | grep -oP '(?<=<artifactId>)[^<]+' | head -1)
                    local pVersion=$(echo "$parentSection" | grep -oP '(?<=<version>)[^<]+' | head -1)
                    
                    if [ -n "$pGroupId" ] && [ -n "$pArtifactId" ] && [ -n "$pVersion" ]; then
                        local pGroupPath=$(echo "$pGroupId" | tr '.' '/')
                        local pPomFile="$M2REPO/$pGroupPath/$pArtifactId/$pVersion/$pArtifactId-$pVersion.pom"
                        if [ ! -f "$pPomFile" ]; then
                            echo "    [MISSING] $pGroupId:$pArtifactId:$pVersion"
                            install_pom_only "$pGroupId" "$pArtifactId" "$pVersion"
                            found=$((found + 1))
                        fi
                    fi
                fi
            fi
        done < "$pomFile"
    done < <(find "$M2REPO" -name "*.pom" -print0 2>/dev/null)
    
    echo "  Pass $pass_num complete. Found $found new missing parents."
    return $found
}

echo "============================================"
echo "Step A: Verify protobuf BOM dependencies"
echo "============================================"
install_pom_only "com.google.protobuf" "protobuf-kotlin" "3.22.3"
install_pom_only "com.google.protobuf" "protobuf-javalite" "3.22.3"
install_pom_only "com.google.protobuf" "protobuf-parent" "3.22.3"

echo ""
echo "============================================"
echo "Step B: Additional known parent POMs"
echo "============================================"

install_pom_only "org.apache" "apache" "4"
install_pom_only "org.apache" "apache" "7"
install_pom_only "org.apache" "apache" "10"
install_pom_only "org.apache" "apache" "11"
install_pom_only "org.apache" "apache" "12"
install_pom_only "org.apache" "apache" "13"
install_pom_only "org.apache" "apache" "14"
install_pom_only "org.apache" "apache" "15"
install_pom_only "org.apache" "apache" "16"
install_pom_only "org.apache" "apache" "17"
install_pom_only "org.apache" "apache" "20"
install_pom_only "org.apache" "apache" "21"
install_pom_only "org.apache" "apache" "23"
install_pom_only "org.apache" "apache" "24"
install_pom_only "org.apache" "apache" "25"

echo ""
echo "============================================"
echo "Step C: Comprehensive recursive scan for ALL missing parent POMs"
echo "============================================"

pass=1
scan_and_install_missing_parents $pass
result=$?
while [ "$result" -gt 0 ]; do
    pass=$((pass + 1))
    if [ $pass -gt 10 ]; then
        echo "  Maximum passes (10) reached, stopping."
        break
    fi
    scan_and_install_missing_parents $pass
    result=$?
done

echo ""
echo "============================================"
echo "PASS 2 COMPLETE"
echo "============================================"
echo "Additional artifacts installed: $(cat $COUNT_FILE)"
