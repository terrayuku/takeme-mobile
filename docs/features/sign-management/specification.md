# Sign Management Feature Specification

## Overview
Crowdsourcing feature enabling users to contribute taxi sign photos by capturing images, selecting route information, and uploading to the shared database.

## Business Value
- Core content generation mechanism
- Builds network effects and database value
- User engagement and retention
- Community building
- Data asset creation

## User Stories

### As a contributor
- I want to add photos of taxi signs I see
- I want to specify the route (from/to) for the sign
- I want to add price information
- I want to see upload progress
- I want confirmation that my contribution was successful
- I want my contributions to help other users

### As a verified user
- I want only verified users to contribute to maintain quality
- I want to ensure contributors are legitimate

## Functional Requirements

### FR-1: Access Sign Upload
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Floating action button visible on main screen
- Button labeled with add/plus icon
- Only verified email users can access
- Unverified users see verification reminder
- Button launches AddSignForDirections activity
- Analytics tracked on button click

#### Implementation Details
```java
// Location: MainActivity.java
- Component: FloatingActionButton (R.id.addDirections)
- Verification Check: auth.getCurrentUser().isEmailVerified()
- Intent: Launches AddSignForDirections activity
- Error: Snackbar with R.string.emailVerification
- Analytics: "Add Sign" event
```

### FR-2: Camera Permission Request
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Camera permission requested on activity start
- Rationale shown if previously denied
- Permission dialog follows Android guidelines
- User can grant or deny permission
- Appropriate feedback for both outcomes
- Permission state persisted by system

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Permission: Manifest.permission.CAMERA
- Request Code: REQUEST_CAMERA (0)
- Method: requestCameraPermission()
- Rationale: R.string.permission_camera_rationale
- Callback: onRequestPermissionsResult()
```

### FR-3: Route Selection (Origin)
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Autocomplete search for origin location
- Same functionality as main search
- Placeholder: "From..."
- Location stored for sign metadata
- Error handling for selection failures

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Component: AutocompleteSupportFragment (R.id.from)
- API: Google Places API
- Fields: ID, NAME, ADDRESS, LAT_LNG
- Storage: Place from
- Error: R.string.location_error
```

### FR-4: Route Selection (Destination)
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Autocomplete search for destination location
- Same functionality as main search
- Placeholder: "To..."
- Location stored for sign metadata
- Error handling for selection failures

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Component: AutocompleteSupportFragment (R.id.to)
- API: Google Places API
- Fields: ID, NAME, ADDRESS, LAT_LNG
- Storage: Place destination
- Error: R.string.location_error
```

### FR-5: Price Input
**Priority**: P1 (High)
**Status**: Implemented

#### Acceptance Criteria
- Text input field for price
- Numeric keyboard
- Optional field (can be empty)
- Stored with sign data
- Displayed in search results

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Component: EditText (R.id.price)
- Input Type: Numeric
- Storage: String priceValue
- Database: sign.setPrice(priceValue)
```

### FR-6: Capture Photo
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Camera button visible and accessible
- Clicking launches device camera
- Photo saved to app-specific storage
- Photo preview shown after capture
- File path stored for upload
- Timestamp-based filename
- JPEG format with compression

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Button: uploadSign(View view)
- Intent: MediaStore.ACTION_IMAGE_CAPTURE
- File Creation: createImageFile()
- Storage: getExternalFilesDir(Environment.DIRECTORY_PICTURES)
- Filename: "JPEG_yyyyMMdd_HHmmss_.jpg"
- Provider: FileProvider for secure file access
- Authority: "com.takeme.takemeto.provider"
- Result: onActivityResult() with REQUEST_IMAGE_CAPTURE
```

#### UI Components
- Camera icon button
- ImageView for preview
- File provider for camera intent

### FR-7: Photo Preview
**Priority**: P1 (High)
**Status**: Implemented

#### Acceptance Criteria
- Captured photo displayed in ImageView
- Image scaled appropriately
- Glide library used for efficient loading
- Preview updates after capture
- Clear visual confirmation of captured image

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Component: ImageView (R.id.newImage)
- Loading: Glide.with(context).load(currentPhotoPath)
- Scaling: setPic() method with BitmapFactory
- Optimization: inSampleSize for memory efficiency
```

### FR-8: Upload Sign
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Upload button visible and labeled
- Validation before upload:
  - Photo captured
  - Origin selected
  - Destination selected
- Progress indicator during upload
- Percentage progress displayed
- Success message on completion
- Error handling for failures
- User redirected after success

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Button: addSign(View view)
- Validation: destination != null && from != null && currentPhotoPath != null
- Method: uploadFile(String newSignPath)
- Storage: Firebase Storage
- Database: Firebase Realtime Database
- Progress: OnProgressListener with percentage
- Compression: JPEG quality 20% for performance
```

### FR-9: Image Compression
**Priority**: P1 (High)
**Status**: Implemented

#### Acceptance Criteria
- Images compressed before upload
- JPEG format with 20% quality
- Reduces upload time and storage costs
- Maintains sufficient quality for recognition
- Byte array conversion for upload

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Method: uploadFile()
- Compression: Bitmap.compress(JPEG, 20, baos)
- Input: Original photo file
- Output: Byte array for Firebase upload
```

### FR-10: Firebase Storage Upload
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Image uploaded to Firebase Storage
- Unique filename (timestamp-based)
- Metadata includes route information
- Download URL generated
- Progress tracking
- Pause/resume capability
- Error handling

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Storage Path: BuildConfig.BUCKET + filename
- Metadata:
  - contentType: "image/jpeg"
  - customMetadata: from, destination
- Upload: reference.child(path).putBytes(data, metadata)
- Listeners:
  - OnProgressListener: Progress updates
  - OnPausedListener: Upload paused
  - OnFailureListener: Upload failed
  - OnSuccessListener: Upload complete
```

### FR-11: Database Entry Creation
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Sign data saved to Firebase Realtime Database
- Organized by destination name (uppercase)
- Includes all metadata:
  - Origin place
  - Destination place
  - Download URL
  - Price
  - User UID
- Auto-generated unique ID
- Success confirmation

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Database Path: BuildConfig.DB/{DESTINATION}/{auto-id}
- Object: Sign model
- Fields:
  - destination: Place object
  - from: Place object
  - downloadUrl: String
  - price: String
  - userUID: String
- Method: databaseReference.child(destination).push().setValue(sign)
```

### FR-12: Upload Progress Display
**Priority**: P1 (High)
**Status**: Implemented

#### Acceptance Criteria
- Progress bar visible during upload
- Percentage text updated in real-time
- Format: "Uploading {percentage}%"
- Progress bar hidden when complete
- Smooth progress updates

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Component: ProgressBar (R.id.simpleProgressBar)
- TextView: message (R.id.message)
- Calculation: (bytesTransferred / totalBytes) * 100
- Update: OnProgressListener callback
- Format: R.string.uploadPrefix + progress + R.string.uploadSurfix
```

### FR-13: Success/Error Feedback
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Success message on successful upload
- Error messages for various failure scenarios:
  - No image captured
  - Invalid route selection
  - Upload failure
  - Database save failure
- User returned to main screen with message
- Analytics tracked for all outcomes

#### Implementation Details
```java
// Location: AddSignForDirections.java
- Success: success() method
  - Message: R.string.uploadSuccess
  - Intent: MainActivity with THANKYOU extra
- Errors:
  - noImageCaptured(): R.string.noImageCaptured
  - invalidValues(): R.string.noValidDirections
  - error(): R.string.genericFailure
```

## Non-Functional Requirements

### NFR-1: Performance
- Photo capture: Instant (device camera)
- Image compression: < 2 seconds
- Upload time: < 10 seconds (average)
- Progress updates: Every 100ms
- Database write: < 1 second

### NFR-2: Usability
- Clear step-by-step flow
- Visual feedback at each step
- Intuitive camera integration
- Easy route selection
- Progress visibility
- Clear error messages

### NFR-3: Reliability
- Crash-free upload flow
- Network error handling
- Retry mechanism for failures
- Data integrity validation
- Atomic operations (storage + database)

### NFR-4: Storage Efficiency
- Image compression (20% quality)
- Efficient file naming
- Automatic cleanup of temp files
- Optimized storage paths
- Metadata for organization

### NFR-5: Security
- Email verification required
- User attribution (UID)
- Secure file provider
- HTTPS for uploads
- Permission-based access

## Technical Architecture

### Components
```
AddSignForDirections Activity
├── Camera Integration
│   ├── Permission Request
│   ├── Camera Intent
│   └── File Provider
├── Route Selection
│   ├── Autocomplete (From)
│   └── Autocomplete (To)
├── Price Input
├── Photo Preview
│   └── Glide Image Loading
├── Upload Manager
│   ├── Image Compression
│   ├── Firebase Storage
│   ├── Progress Tracking
│   └── Database Write
└── Feedback System
    ├── Success Messages
    └── Error Handling
```

### Data Flow
```
User Action → Camera → Photo Capture → Preview → 
Route Selection → Validation → Compression → 
Storage Upload → Database Write → Success Feedback
```

### File Management
```
Photo Capture
├── createImageFile()
│   ├── Timestamp filename
│   ├── External storage (Pictures)
│   └── File path stored
├── galleryAddPic()
│   └── Media scanner notification
└── setPic()
    └── Bitmap scaling and display
```

### Upload Process
```
1. Validate inputs (photo, from, to)
2. Load photo as Bitmap
3. Compress to JPEG (20% quality)
4. Convert to byte array
5. Create storage metadata
6. Upload to Firebase Storage
   ├── Track progress
   ├── Handle pause/resume
   └── Get download URL
7. Create Sign object
8. Save to Realtime Database
9. Show success/error message
10. Navigate to MainActivity
```

## UI/UX Design

### Add Sign Screen
```
┌─────────────────────────┐
│  ← Add Sign             │
├─────────────────────────┤
│  ┌───────────────────┐  │
│  │ 📍 From...        │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │ 📍 To...          │  │
│  └───────────────────┘  │
│  ┌───────────────────┐  │
│  │ Price (Optional)  │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │  [Photo Preview]  │  │
│  │   or Placeholder  │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  [ 📷 Take Photo ]      │
│  [ ✓ Upload Sign ]      │
│                         │
│  [Status Message]       │
│  [Progress: 45%]        │
│                         │
│  [      Ad       ]      │
└─────────────────────────┘
```

## Error Handling

### Error Scenarios
| Error | Message | Action |
|-------|---------|--------|
| Camera permission denied | "Camera permission required" | Show rationale, request again |
| No photo captured | "Please capture a photo of the taxi sign" | Stay on screen, highlight camera button |
| No origin selected | "Please enter valid directions" | Stay on screen, highlight from field |
| No destination selected | "Please enter valid directions" | Stay on screen, highlight to field |
| Upload failed | "Upload failed. Please try again." | Return to main screen |
| Database save failed | "Something went wrong" | Return to main screen |
| Network error | Firebase error message | Return to main screen |
| Camera not available | "Camera not available" | Return to main screen |

## Analytics Events

### Tracked Events
```java
// Upload Flow
- "Add Sign" - User clicks add sign button
  - Parameters: verified status
- "Photo Captured" - Photo taken successfully
- "Upload Started" - Upload initiated
  - Parameters: from, to, has_price
- "Upload Progress" - Progress milestones (25%, 50%, 75%)
- "Upload Success" - Sign uploaded successfully
  - Parameters: from, to, price, file_size
- "Upload Failed" - Upload failed
  - Parameters: error_type, from, to
- "Database Save Success" - Sign saved to database
- "Database Save Failed" - Database save failed
```

## Testing Requirements

### Unit Tests
- Input validation logic
- Image compression
- File path generation
- Progress calculation
- Error message generation

### Integration Tests
- Camera intent flow
- Firebase Storage upload
- Database write operations
- Places API integration
- File provider access

### UI Tests (Espresso)
- TestAddingSign.java (Implemented)
- Camera permission flow
- Photo capture flow
- Route selection
- Upload button interaction
- Progress display
- Success message

## Dependencies
```gradle
// Camera & Images
implementation 'androidx.core:core:1.x.x'
implementation 'com.github.bumptech.glide:glide:4.10.0'

// Firebase
implementation 'com.google.firebase:firebase-storage:19.1.0'
implementation 'com.google.firebase:firebase-database:19.2.0'
implementation 'com.google.firebase:firebase-auth:19.2.0'

// Places
implementation 'com.google.android.libraries.places:places:2.1.0'
```

## Configuration

### File Provider (AndroidManifest.xml)
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="com.takeme.takemeto.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Firebase Storage Rules
```javascript
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{imageId} {
      allow read: if true;
      allow write: if request.auth != null 
                   && request.auth.token.email_verified == true
                   && request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

### Firebase Database Rules
```json
{
  "rules": {
    "signs": {
      ".read": true,
      "$destination": {
        ".write": "auth != null && auth.token.email_verified == true",
        ".validate": "newData.hasChildren(['from', 'destination', 'downloadUrl', 'userUID'])"
      }
    }
  }
}
```

## Future Enhancements

### Phase 2
- Multiple photo upload per sign
- Photo editing (crop, rotate, enhance)
- Duplicate detection
- Sign verification by community
- Contributor reputation system
- Offline upload queue

### Phase 3
- AI-powered sign text extraction
- Automatic route detection from photo
- Quality scoring for photos
- Contributor rewards/gamification
- Bulk upload capability
- Photo moderation workflow

## Success Metrics
- Upload completion rate: > 75%
- Average upload time: < 15 seconds
- Photo quality acceptance: > 85%
- Contributor retention: > 40%
- Signs per active contributor: > 3
- Upload error rate: < 5%

## Known Issues
- Large photos may cause memory issues on low-end devices (mitigated by compression)
- Upload may fail on very slow networks (no retry mechanism yet)
- No duplicate detection (same sign uploaded multiple times)

## Related Documentation
- [Search Directions Feature](../search-directions/specification.md)
- [Authentication Feature](../authentication/specification.md)
- [Firebase Architecture](../../technical/architecture.md)
- [Image Optimization Guide](../../technical/image-optimization.md)
