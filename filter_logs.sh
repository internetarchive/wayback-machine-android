#!/bin/bash

# Android Log Filter Script
# This script shows only important logs: crashes, errors, and key events

echo "🔍 Starting Android Log Filter..."
echo "📱 This will show only important logs from your Android app"
echo "🚨 Crashes, errors, and key events will be displayed"
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
echo "📊 Starting filtered log monitoring..."
echo "💡 Press Ctrl+C to stop monitoring"
echo ""

# Start monitoring only important logs
adb logcat -s MainActivity:E MainActivity:W | while IFS= read -r line; do
    # Show all error and warning logs
    if [[ $line == *"🚨 CRASH DETECTED"* ]]; then
        echo -e "\033[31m🚨 CRASH DETECTED:\033[0m $line"
    elif [[ $line == *"❌"* ]]; then
        echo -e "\033[31m❌ ERROR:\033[0m $line"
    elif [[ $line == *"⚠️"* ]]; then
        echo -e "\033[33m⚠️ WARNING:\033[0m $line"
    else
        echo "$line"
    fi
done 