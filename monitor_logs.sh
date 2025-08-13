#!/bin/bash

# Android Log Monitor Script
# This script automatically monitors Android logs for crashes and issues

echo "🔍 Starting Android Log Monitor..."
echo "📱 This will show all logs from your Android app in real-time"
echo "🚨 Crashes and errors will be highlighted automatically"
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found! Please make sure Android SDK is installed and in your PATH"
    exit 1
fi

# Check if device is connected
echo "🔌 Checking for connected devices..."
adb devices

echo ""
echo "📊 Starting log monitoring..."
echo "💡 Press Ctrl+C to stop monitoring"
echo ""

# Start monitoring logs with automatic crash detection
adb logcat -s MainActivity:V | while IFS= read -r line; do
    # Highlight crashes and errors
    if [[ $line == *"🚨 CRASH DETECTED"* ]] || [[ $line == *"❌"* ]]; then
        echo -e "\033[31m🚨 CRASH/ERROR DETECTED:\033[0m $line"
    elif [[ $line == *"✅"* ]]; then
        echo -e "\033[32m✅ SUCCESS:\033[0m $line"
    elif [[ $line == *"🔍"* ]] || [[ $line == *"📱"* ]]; then
        echo -e "\033[34m🔍 INFO:\033[0m $line"
    elif [[ $line == *"🔄"* ]]; then
        echo -e "\033[33m🔄 LIFECYCLE:\033[0m $line"
    elif [[ $line == *"🧭"* ]]; then
        echo -e "\033[35m🧭 NAVIGATION:\033[0m $line"
    else
        echo "$line"
    fi
done 