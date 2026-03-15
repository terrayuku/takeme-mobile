# Technical Architecture

## Overview
TakeMe Mobile is built as a native Android application with Firebase as the backend-as-a-service (BaaS) platform, providing authentication, database, storage, and analytics capabilities.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Application                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │  Activity  │  │  Activity  │  │  Activity  │            │
│  │   Layer    │  │   Layer    │  │   Layer    │            │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘            │
│        │                │                │                    │
│  ┌─────▼────────────────▼────────────────▼──────┐           │
│  │         Business Logic Layer                  │           │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │           │
│  │  │Analytics │  │ Location │  │  Image   │   │           │
│  │  │          │  │          │  │Processing│   │           │
│  │  └──────────┘  └──────────┘  └──────────┘   │           │
│  └───────────────────────────────────────────────┘           │
│                                                               │
│  ┌───────────────────────────────────────────────┐           │
│  │         Data Access Layer                     │           │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │           │
│  │  │ Firebase │  │  Google  │  │  Local   │   │           │
│  │  │  Client  │  │   APIs   │  │ Storage  │   │           │
│  │  └──────────┘  └──────────┘  └──────────┘   │           │
│  └───────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    External Services                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Firebase   │  │    Google    │  │   AdMob      │      │
│  │   Platform   │  │  Maps/Places │  │              │      │
│  │              │  │     API      │  │              │      │
│  │ ┌──────────┐ │  └──────────────┘  └──────────────┘      │
│  │ │   Auth   │ │                                           │
│  │ └──────────┘ │                                           │
│  │ ┌──────────┐ │                                           │
│  │ │ Database │ │                                           │
│  │ └──────────┘ │                                           │
│  │ ┌──────────┐ │                                           │
│  │ │ Storage  │ │                                           │
│  │ └──────────┘ │                                           │
│  │ ┌──────────┐ │                                           │
│  │ │Analytics │ │                                           │
│  │ └──────────┘ │                                           │
│  └──────────────┘                                           │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Frontend (Android)
- **Language**: Java
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 29 (Android 10)
- **Build Tool**: Gradle
- **Architecture**: Activity-based (transitioning to MVVM in Phase 2)

### Backend (Firebase)
- **Authentication**: Firebase Authentication
- **Database**: Firebase Realtime Database
- **Storage**: Firebase Cloud Storage
- **Analytics**: Firebase Analytics
- **Crash Reporting**: Firebase Crashlytics
- **Cloud Functions**: Not yet implemented (planned for Phase 2)

### Third-Party Services
- **Maps**: Google Maps API
- **Places**: Google Places API
- **Location**: Google Location Services
- **Ads**: Google AdMob
- **Image Loading**: Glide

### Development Tools
- **IDE**: Android Studio
- **Version Control**: Git (GitHub)
- **CI/CD**: Travis CI
- **Testing**: JUnit, Espresso
- **Monitoring**: Firebase Crashlytics

## Component Architecture

### 1. Presentation Layer

#### Activities
```
MainActivity
├── Purpose: Main search interface
├── Components:
│   ├── AutocompleteSupportFragment (From)
│   ├── AutocompleteSupportFragment (To)
│   ├── FloatingActionButton (Search)
│   └── FloatingActionButton (Add Sign)
└── Navigation: DisplaySignActivity, AddSignForDirections

LoginActivity
├── Purpose: Authentication
├── Components:
│   ├── Email/Password inputs
│   ├── Sign up/Sign in toggle
│   └── Password reset
└── Navigation: MainActivity

AddSignForDirections
├── Purpose: Sign upload
├── Components:
│   ├── Camera integration
│   ├── AutocompleteSupportFragment (From/To)
│   ├── Price input
│   └── Upload progress
└── Navigation: MainActivity (with result)

DisplaySignActivity
├── Purpose: Show sign results
├── Components:
│   ├── ImageView (sign photo)
│   ├── TextView (route info)
│   ├── TextView (price)
│   └── Button (Got it)
└── Navigation: MainActivity (with feedback)

AboutActivity
├── Purpose: App information
└── Components: Static content

SettingsActivity
├── Purpose: User preferences
└── Components: Preference fragments

SplashActivity
├── Purpose: App initialization
└── Navigation: LoginActivity or MainActivity
```

### 2. Business Logic Layer

#### Analytics (impl/Analytics.java)
```java
public class Analytics {
    public void setAnalytics(
        FirebaseAnalytics analytics,
        String eventName,
        String itemId,
        String itemName
    )
}
```
- Centralized analytics tracking
- Consistent event logging
- Firebase Analytics integration

#### Location (impl/Location.java)
```java
public class Location {
    public AutocompleteSupportFragment setPlace(
        AutocompleteSupportFragment fragment,
        String hint
    )
    
    public SpannableStringBuilder message(
        String key,
        Intent intent
    )
}
```
- Location autocomplete configuration
- South Africa location bias
- Message formatting utilities

#### FragmentSupport (impl/FragmentSupport.java)
- Fragment management utilities
- UI helper methods

### 3. Data Layer

#### Models

**Sign.java**
```java
public class Sign {
    Place destination;
    String downloadUrl;
    Place from;
    String userUID;
    String price;
}
```

**About.java**
```java
public class About {
    // App information model
}
```

#### Data Sources

**Firebase Realtime Database**
```
Structure:
/signs
  /{DESTINATION_NAME}
    /{auto-generated-id}
      - from: Place object
      - destination: Place object
      - downloadUrl: String
      - price: String
      - userUID: String
```

**Firebase Storage**
```
Structure:
/{environment}/images
  /JPEG_{timestamp}_.jpg
```

**Local Storage**
- Temporary photo files
- Glide image cache
- Shared preferences (minimal)

## Data Flow

### 1. User Authentication Flow
```
User Input → LoginActivity
    ↓
Firebase Authentication
    ↓
Success → MainActivity
Failure → Error Message
```

### 2. Search Flow
```
User Input (From/To) → MainActivity
    ↓
Google Places API (Autocomplete)
    ↓
Place Selection → Validation
    ↓
Firebase Database Query
    ↓
Sign Found → DisplaySignActivity
Sign Not Found → Error Message
```

### 3. Upload Flow
```
User Action → AddSignForDirections
    ↓
Camera Intent → Photo Capture
    ↓
Route Selection (Places API)
    ↓
Image Compression
    ↓
Firebase Storage Upload
    ↓
Get Download URL
    ↓
Firebase Database Write
    ↓
Success → MainActivity (with message)
```

## Security Architecture

### Authentication
- Firebase Authentication handles all auth
- Email verification required for uploads
- Session tokens managed by Firebase
- Secure password storage (Firebase)

### Data Access
- Database rules enforce read/write permissions
- Storage rules require authentication for uploads
- User UID tracked for all contributions
- No sensitive data stored locally

### Network Security
- HTTPS for all API calls
- Certificate pinning (not yet implemented)
- API key restrictions (Google APIs)
- Firebase security rules

### Firebase Security Rules

**Database Rules**
```json
{
  "rules": {
    "signs": {
      ".read": true,
      "$destination": {
        ".write": "auth != null && auth.token.email_verified == true",
        ".validate": "newData.hasChildren(['from', 'destination', 'downloadUrl', 'userUID'])",
        ".indexOn": ["destination/name", "from/name"]
      }
    }
  }
}
```

**Storage Rules**
```javascript
service firebase.storage {
  match /b/{bucket}/o {
    match /{environment}/images/{imageId} {
      allow read: if true;
      allow write: if request.auth != null 
                   && request.auth.token.email_verified == true
                   && request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

## Performance Optimization

### Image Optimization
- **Compression**: 20% JPEG quality for uploads
- **Caching**: Glide automatic caching
- **Loading**: Progressive loading with placeholders
- **Sizing**: Bitmap scaling for display

### Database Optimization
- **Indexing**: Indexed by destination name
- **Query Optimization**: Filter at database level
- **Data Structure**: Denormalized for read performance
- **Caching**: Firebase local persistence

### Network Optimization
- **Request Batching**: Minimize API calls
- **Retry Logic**: Automatic retry on failure
- **Timeout Handling**: Graceful timeout handling
- **Offline Support**: Firebase offline persistence

### App Performance
- **Lazy Loading**: Load data on demand
- **Memory Management**: Proper lifecycle handling
- **Background Tasks**: Async operations
- **ProGuard**: Code minification and obfuscation

## Scalability Considerations

### Current Limitations
- Single region (Firebase US)
- No CDN for images
- No caching layer
- Monolithic app architecture

### Phase 2 Improvements
- Multi-region Firebase
- CDN integration (Cloudflare/Fastly)
- Redis caching layer
- Microservices architecture (Cloud Functions)

### Phase 3 Improvements
- Global edge network
- Database sharding
- Load balancing
- Auto-scaling infrastructure

## Monitoring & Observability

### Crash Reporting
- Firebase Crashlytics
- Real-time crash alerts
- Stack trace analysis
- User impact tracking

### Analytics
- Firebase Analytics
- Custom event tracking
- User behavior analysis
- Funnel tracking

### Performance Monitoring
- Firebase Performance Monitoring (planned)
- Network request tracking
- Screen rendering metrics
- App startup time

### Logging
- Android Logcat
- Firebase Crashlytics logs
- Custom logging framework (planned)

## Development Workflow

### Environment Configuration
```gradle
buildTypes {
    release {
        buildConfigField("String", 'BUCKET', "\"prod/images\"")
        buildConfigField("String", 'DB', "\"prod/signs\"")
    }
    debug {
        buildConfigField("String", 'BUCKET', "\"images\"")
        buildConfigField("String", 'DB', "\"signs\"")
    }
}
```

### CI/CD Pipeline (Travis CI)
```yaml
1. Code Push → GitHub
2. Travis CI Triggered
3. Build & Test
4. Sign APK (release only)
5. Deploy to Google Play (manual)
```

### Testing Strategy
- **Unit Tests**: Business logic
- **Integration Tests**: Firebase integration
- **UI Tests**: Espresso tests
- **Manual Testing**: Device testing

## API Integration

### Google Maps API
```java
// Initialization
Places.initialize(context, API_KEY);

// Usage
AutocompleteSupportFragment fragment;
fragment.setPlaceFields(Arrays.asList(
    Place.Field.ID,
    Place.Field.NAME,
    Place.Field.ADDRESS,
    Place.Field.LAT_LNG
));
```

### Firebase APIs
```java
// Authentication
FirebaseAuth.getInstance()
    .signInWithEmailAndPassword(email, password);

// Database
FirebaseDatabase.getInstance()
    .getReference("signs")
    .child(destination)
    .addValueEventListener(...);

// Storage
FirebaseStorage.getInstance()
    .getReference()
    .child(path)
    .putBytes(data, metadata);
```

## Dependencies

### Core Dependencies
```gradle
// AndroidX
implementation 'androidx.appcompat:appcompat:1.1.0'
implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
implementation 'com.google.android.material:material:1.2.0'

// Firebase
implementation 'com.google.firebase:firebase-auth:19.2.0'
implementation 'com.google.firebase:firebase-database:19.2.0'
implementation 'com.google.firebase:firebase-storage:19.1.0'
implementation 'com.google.firebase:firebase-analytics:17.2.1'
implementation 'com.crashlytics.sdk.android:crashlytics:2.10.1'

// Google Services
implementation 'com.google.android.gms:play-services-auth:17.0.0'
implementation 'com.google.android.gms:play-services-maps:17.0.0'
implementation 'com.google.android.libraries.places:places:2.1.0'
implementation 'com.google.android.gms:play-services-location:17.0.0'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.10.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.10.0'

// Ads
implementation 'com.google.firebase:firebase-ads:18.3.0'
implementation 'com.google.android.gms:play-services-ads:18.3.0'

// UI
implementation 'com.firebaseui:firebase-ui-auth:4.3.1'
implementation 'com.firebaseui:firebase-ui-database:4.0.0'
```

## Future Architecture Improvements

### Phase 2
- MVVM architecture pattern
- Dependency injection (Dagger/Hilt)
- Repository pattern
- Kotlin migration (gradual)
- Coroutines for async operations
- Room database for offline support

### Phase 3
- Modular architecture
- Feature modules
- Dynamic feature delivery
- Jetpack Compose UI (gradual)
- GraphQL API layer
- Microservices backend

## Related Documentation
- [Tech Stack Details](./tech-stack.md)
- [API Integration Guide](./api-integration.md)
- [Feature Specifications](../features/)
- [Roadmap](../roadmap/)
