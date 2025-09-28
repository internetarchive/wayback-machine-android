# Changelog

All notable changes to the Wayback Machine Android App will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

- **1.9.0** - Real upload functionality, enhanced UX, production-ready code
- **1.8.0** - Basic upload interface and authentication
- **1.7.0** - Initial Wayback Machine functionality
