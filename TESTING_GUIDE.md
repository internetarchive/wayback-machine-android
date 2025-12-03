# Quick Testing Guide - Wayback Machine Android App

A concise guide for testing the Wayback Machine Android app.

## Installing the APK

### Step 1: Enable Unknown Sources
- **Android 8.0+**: Settings → Apps → Special app access → Install unknown apps → Enable for your browser/file manager
- **Android 7.1 and earlier**: Settings → Security → Enable "Unknown sources"

### Step 2: Download & Install
1. Download the APK from the shared link
2. Open **Files** app → **Downloads**
3. Tap the APK file → **Install**
4. Tap **Open** to launch

## Quick Test Checklist

### Basic Functionality
- [ ] App launches without crashes
- [ ] All 5 tabs are accessible (Home, Help, About, Account, Upload)
- [ ] User can login with Internet Archive account
- [ ] User can logout

### Home Tab - Save Page Now (SPN2)
- [ ] Dialog appears with animated "Saving page..." title
- [ ] Status updates during save process
- [ ] Title changes to "Save succeeded" or "Save failed" after completion
- [ ] "View the snapshot" button appears after success
- [ ] Close button appears after completion
- [ ] Dialog cannot be dismissed during save
- [ ] Dialog can be dismissed after completion

### Home Tab - View Archives
- [ ] "View Recent Version" works
- [ ] "View First Version" works

### Upload Tab
- [ ] Can select images/videos
- [ ] Upload progress is visible
- [ ] Upload completes successfully

### Error Handling
- [ ] Shows error when not logged in
- [ ] Shows error for invalid URLs
- [ ] Shows error for network issues
- [ ] Error messages are clear

## Common Issues

| Issue | Solution |
|-------|----------|
| APK won't install | Enable "Install from unknown sources" |
| App crashes | Clear app data and reinstall |
| Login fails | Check credentials, verify internet connection |
| Save fails | Ensure logged in, check internet, verify URL |

## Reporting Issues

Include in bug reports:
- Device model and Android version
- App version (check in About tab)
- Steps to reproduce
- Screenshots (if possible)
- Error messages

**Contact:** info@archive.org (Subject: "Wayback Machine Android App - Bug Report")

---

**App Version:** 2.0.0  
**Last Updated:** 2024-11-12

