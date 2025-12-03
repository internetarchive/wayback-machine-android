# Changelog

All notable changes to the Wayback Machine Android App will be documented in this file.

## [2.0.0] - 2024-11-12

### Added
- **Save Page Now 2 (SPN2) Feature**
  - Implemented dialog-based Save Page Now interface with real-time status updates
  - Replaced browser-based saving with in-app dialog for seamless experience
  - Added system status checks for better error diagnostics (save/status/system)
  - Added user status checks for quota limits and rate limiting (save/status/user)
  - Content-adaptive dialog sizing that adjusts to message length
  - "View the snapshot" button for easy access to saved pages
  - Enhanced error handling with specific messages based on system/user status
  - Prevention of multiple simultaneous login attempts

- **API Integration Improvements**
  - Added system status endpoint integration
  - Added user status endpoint integration
  - Handled "same snapshot" messages to skip unnecessary polling
  - Improved 404 error handling for expired/invalid job IDs
  - Status polling with automatic retry and timeout handling


### Fixed
- **Save Page Now Issues**
  - Fixed API request format mismatches
  - Fixed cookie encoding issues
  - Fixed status polling for completed jobs
  - Fixed dialog size changing after errors
  - Fixed "same snapshot" message handling


### Changed
- **Code Quality**
  - Implemented SavePageNowDialog class with polling mechanism
  - Added comprehensive logging for debugging
  - Thread-safe UI updates using Handler
  - Proper cleanup of handlers and runnables
  - Lifecycle-aware dialog management
  - Enhanced error recovery mechanisms


### Technical Details
- Updated version to 2.0.0
- Implemented SPN2 API integration matching IOS app functionality
- Added system and user status checks for comprehensive error reporting
- Enhanced network error handling and recovery
- Improved dialog lifecycle management
- Added extensive logging for production debugging

---

## [1.9.0] - 2024-09-28

### Added
- **Real Internet Archive Upload Functionality**
  - Implemented actual file uploads to Internet Archive's S3 storage
  - Added proper S3 authentication with user credentials
  - Comprehensive metadata headers for uploaded files (title, description, creator, etc.)
  - Support for both images and videos uploads

- **Enhanced User Experience**
  - Professional loading dialog with prominent progress indicator
  - Smooth fade-in/fade-out animations for loading states
  - Navigation control during uploads (prevents accidental navigation)
  - Automatic state clearing after successful uploads
  - Thumbnail clearing functionality for better visual feedback

- **Improved Error Handling**
  - Enhanced login error messages with user-friendly descriptions
  - Better file preparation error handling with detailed diagnostics
  - Robust file extension detection using MIME types
  - State persistence across fragment lifecycle changes

### Fixed
- **Upload Issues**
  - Fixed "Could not prepare file for upload" error
  - Resolved "You need to attach photo or video" false positive
  - Corrected file extension extraction from content URIs
  - Fixed fragment state loss during app lifecycle changes

- **Login Issues**
  - Fixed missing error messages for incorrect email/password
  - Ensured error messages display on UI thread
  - Improved error message clarity and user-friendliness

### Changed
- **Code Quality**
  - Removed all debug logs and test code for production readiness
  - Cleaned up unused variables and imports
  - Optimized code structure and performance
  - Enhanced code maintainability

- **UI/UX Improvements**
  - Updated loading indicator design for better visibility
  - Improved upload button state management
  - Enhanced progress feedback during uploads

### Technical Details
- Updated to target SDK 35
- Enhanced file handling with proper MIME type detection
- Implemented SharedPreferences for state persistence
- Added comprehensive error logging for production debugging
- Optimized network requests with proper timeout handling

---

## [1.8.0] - Previous Release

### Previous Features
- Basic Wayback Machine functionality
- User authentication
- File upload interface
- Image and video picker integration

---

## Version History

- **2.0.0** - Save Page Now 2 (SPN2) feature with dialog-based UI, enhanced error handling, and system/user status checks
- **1.9.0** - Real upload functionality, enhanced UX, production-ready code
- **1.8.0** - Basic upload interface and authentication and target latest Android SDK
- **1.7.0** - Initial Wayback Machine functionality
