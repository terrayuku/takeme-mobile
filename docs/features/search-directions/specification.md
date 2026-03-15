# Search Directions Feature Specification

## Overview
Core feature enabling users to search for taxi routes by entering origin and destination locations, then viewing photos of taxi signs that match their route.

## Business Value
- Primary value proposition of the app
- Solves core user pain point (finding correct taxi)
- Drives user engagement and retention
- Generates search data for analytics
- Creates demand for sign contributions

## User Stories

### As a commuter
- I want to search for a route from my location to my destination
- I want to see photos of taxi signs for my route
- I want to know the price of the route
- I want to autocomplete location names so I can search faster
- I want to see if a route exists before going to the taxi rank

### As a tourist
- I want to search using familiar place names
- I want visual confirmation (photos) of which taxi to take
- I want to feel confident about my route choice

## Functional Requirements

### FR-1: Location Search (Origin)
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- User can search for origin location using autocomplete
- Autocomplete suggests places as user types
- Autocomplete biased to South African locations
- Selected location stored for route search
- Clear visual indication of selected location
- Placeholder text: "From..."

#### Implementation Details
```java
// Location: MainActivity.java
- Component: AutocompleteSupportFragment (R.id.from)
- API: Google Places API
- Bias: RectangularBounds for South Africa
  - Southwest: (-34.277857, 18.2359139)
  - Northeast: (-23.9116035, 29.380895)
- Fields: ID, NAME, ADDRESS, LAT_LNG
- Storage: Place from (instance variable)
```

#### UI Components
- AutocompleteSupportFragment
- Search icon
- Clear button
- Dropdown suggestions list
- Loading indicator

### FR-2: Location Search (Destination)
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- User can search for destination location using autocomplete
- Autocomplete suggests places as user types
- Autocomplete biased to South African locations
- Selected location stored for route search
- Clear visual indication of selected location
- Placeholder text: "To..."

#### Implementation Details
```java
// Location: MainActivity.java
- Component: AutocompleteSupportFragment (R.id.destination)
- API: Google Places API
- Bias: Same as origin (South Africa)
- Fields: ID, NAME, ADDRESS, LAT_LNG
- Storage: Place destination (instance variable)
```

### FR-3: Find Directions Button
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- Floating action button visible on main screen
- Button labeled with directions icon
- Button enabled only when both locations selected
- Clicking button initiates sign search
- Validation before search execution
- Error message if locations not selected

#### Implementation Details
```java
// Location: MainActivity.java
- Component: FloatingActionButton (R.id.findDirections)
- Method: findSingButton(View view)
- Validation: Checks from != null && destination != null
- Intent: Launches DisplaySignActivity with route data
- Error: Shows "Please enter valid directions" message
```

#### UI Components
- FloatingActionButton with directions icon
- Snackbar for error messages
- Loading state (optional)

### FR-4: Route Validation
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- System validates both locations selected before search
- Clear error message if validation fails
- Error displayed in red text
- User remains on search screen to correct input
- No navigation if validation fails

#### Implementation Details
```java
// Location: MainActivity.java
- Validation: if (from == null || destination == null)
- Error Message: R.string.noValidDirections
- Display: SpannableStringBuilder with RED color
- Analytics: Tracks "No Valid Directions Entered"
```

### FR-5: Display Search Results
**Priority**: P0 (Critical)
**Status**: Implemented

#### Acceptance Criteria
- User navigated to results screen after valid search
- Route description displayed: "From [Origin] To [Destination]"
- Taxi sign photo displayed if route exists
- Price displayed if available
- Loading indicator while fetching data
- Error message if no sign found
- "Got it" button to return to search

#### Implementation Details
```java
// Location: DisplaySignActivity.java
- Intent Extras:
  - EXTRA_MESSAGE: Full route description
  - FROM: Origin place name
  - DESTINATION: Destination place name
- Database Query: Firebase Realtime Database
  - Path: /signs/{DESTINATION}/{signId}
  - Filter: where from.name matches origin
- Image Loading: Glide library
- Price Display: "R {price}" format
```

#### UI Components
- TextView for route description
- ImageView for sign photo
- TextView for price
- ProgressBar for loading
- "Got it" button (thumbs up icon)
- AdView for monetization

### FR-6: Sign Not Found Handling
**Priority**: P1 (High)
**Status**: Implemented

#### Acceptance Criteria
- Clear message when no sign exists for route
- Suggestion to contribute a sign
- User returned to main screen
- Message displayed on main screen
- Analytics event logged

#### Implementation Details
```java
// Location: DisplaySignActivity.java
- Method: signNotFound()
- Message: R.string.signNotFound
- Intent Extra: SIGN_NOT_FOUND
- Return: MainActivity with message
- Analytics: "Sign Not Found" event
```

### FR-7: Search History (Future)
**Priority**: P2 (Medium)
**Status**: Not Implemented

#### Acceptance Criteria
- Recent searches saved locally
- Quick access to frequent routes
- Clear history option
- Maximum 10 recent searches

## Non-Functional Requirements

### NFR-1: Performance
- Autocomplete response time: < 500ms
- Sign photo load time: < 2 seconds
- Database query time: < 1 second
- Smooth scrolling and transitions
- Efficient image caching

### NFR-2: Usability
- Intuitive search interface
- Clear visual hierarchy
- Accessible touch targets (48dp minimum)
- Keyboard auto-dismiss after selection
- Back button returns to search
- Error messages in plain language

### NFR-3: Reliability
- Graceful handling of network errors
- Offline detection and messaging
- Retry mechanism for failed requests
- Fallback for missing images
- Crash-free search flow

### NFR-4: Scalability
- Efficient database queries (indexed by destination)
- Image optimization and compression
- Pagination for multiple results (future)
- Caching strategy for frequent searches

## Technical Architecture

### Components
```
MainActivity (Search Screen)
├── AutocompleteSupportFragment (From)
├── AutocompleteSupportFragment (To)
├── FloatingActionButton (Search)
├── Validation Logic
└── Navigation to Results

DisplaySignActivity (Results Screen)
├── Firebase Database Query
├── Image Loading (Glide)
├── Price Display
├── Error Handling
└── Analytics Tracking
```

### Data Flow
```
User Input → Autocomplete → Place Selection → Validation → 
Database Query → Image Fetch → Display Results → User Action
```

### Database Structure
```json
{
  "signs": {
    "DESTINATION_NAME": {
      "signId1": {
        "from": {
          "name": "Origin Name",
          "address": "Full Address",
          "latLng": {...}
        },
        "destination": {
          "name": "Destination Name",
          "address": "Full Address",
          "latLng": {...}
        },
        "downloadUrl": "https://...",
        "price": "15",
        "userUID": "user123"
      }
    }
  }
}
```

### API Integration

#### Google Places API
```java
// Initialization
Places.initialize(context, API_KEY);

// Autocomplete Configuration
fragment.setPlaceFields(Arrays.asList(
    Place.Field.ID,
    Place.Field.NAME,
    Place.Field.ADDRESS,
    Place.Field.LAT_LNG
));

// Location Bias (South Africa)
fragment.setLocationBias(RectangularBounds.newInstance(
    new LatLng(-34.277857, 18.2359139),  // Southwest
    new LatLng(-23.9116035, 29.380895)   // Northeast
));
```

#### Firebase Realtime Database
```java
// Query
databaseReference = database.getReference("signs");
databaseReference.addValueEventListener(new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        // Filter by destination and origin
        Sign sign = getSign(from, destination, dataSnapshot);
        displaySign(sign);
    }
});
```

## UI/UX Design

### Main Screen (Search)
```
┌─────────────────────────┐
│  ☰  TakeMe      ⋮       │
├─────────────────────────┤
│                         │
│  ┌───────────────────┐  │
│  │ 📍 From...        │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ 📍 To...          │  │
│  └───────────────────┘  │
│                         │
│  [Status Message]       │
│                         │
│                         │
│                    [➕] │ Add Sign FAB
│                    [🔍] │ Search FAB
│                         │
│  [      Ad       ]      │
└─────────────────────────┘
```

### Results Screen
```
┌─────────────────────────┐
│  ← Display Sign         │
├─────────────────────────┤
│  From Soweto To Sandton │
│                         │
│  ┌───────────────────┐  │
│  │                   │  │
│  │   [Sign Photo]    │  │
│  │                   │  │
│  │                   │  │
│  └───────────────────┘  │
│                         │
│  Price: R 15            │
│                         │
│  [  👍 Got it  ]        │
│                         │
│  [      Ad       ]      │
└─────────────────────────┘
```

## Error Handling

### Error Scenarios
| Error | Message | Action |
|-------|---------|--------|
| No locations selected | "Please enter valid directions" | Stay on search screen |
| Sign not found | "Sorry, no sign found for this route. Please add one!" | Return to main screen |
| Network error | "Connection error. Please check your internet." | Show retry option |
| Places API timeout | "Location search timed out. Please try again." | Allow retry |
| Image load failure | "Could not load sign image" | Show placeholder |
| Database error | "Something went wrong. Please try again." | Return to main screen |

## Analytics Events

### Tracked Events
```java
// Search Events
- "From" - Place selected for origin
  - Parameters: tag, status (Found/Not Found)
- "To" - Place selected for destination
  - Parameters: tag, status (Found/Not Found)
- "Directions" - Search initiated
  - Parameters: from, to, status (Valid/Invalid)

// Results Events
- "DisplaySignActivity Open" - Results screen opened
- "DisplaySignActivity Get Directions" - Sign fetched
  - Parameters: from, to, status (Found/Not Found)
- "Sign Found" - Sign successfully displayed
- "Sign Not Found" - No sign for route
  - Parameters: from, to
- "Got it, happy" - User confirmed sign helpful
```

## Testing Requirements

### Unit Tests
- Location validation logic
- Database query filtering
- Price formatting
- Error message generation

### Integration Tests
- Places API integration
- Firebase database queries
- Image loading with Glide
- Navigation flow

### UI Tests (Espresso)
- SearchASign.java (Implemented)
- Autocomplete interaction
- Search button click
- Results display
- Error message display

## Dependencies
```gradle
// Google Places
implementation 'com.google.android.libraries.places:places:2.1.0'
implementation 'com.google.android.gms:play-services-maps:17.0.0'
implementation 'com.google.android.gms:play-services-location:17.0.0'

// Firebase
implementation 'com.google.firebase:firebase-database:19.2.0'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.10.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.10.0'

// Analytics
implementation 'com.google.firebase:firebase-analytics:17.2.1'
```

## Configuration

### API Keys
```xml
<!-- res/values/strings.xml -->
<string name="maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
```

### Firebase Rules
```json
{
  "rules": {
    "signs": {
      ".read": true,
      "$destination": {
        ".indexOn": ["destination/name", "from/name"]
      }
    }
  }
}
```

## Future Enhancements

### Phase 2
- Multiple sign results for same route
- User ratings for sign quality
- Alternative routes suggestion
- Real-time taxi availability
- Offline search (cached results)

### Phase 3
- Voice search
- AR navigation to taxi rank
- Integration with Google Maps directions
- Predictive search based on time/location
- Route popularity indicators

## Success Metrics
- Search completion rate: > 80%
- Sign found rate: > 70%
- Average search time: < 30 seconds
- User satisfaction (Got it clicks): > 85%
- Search-to-contribution conversion: > 5%

## Known Issues
- Places API timeout errors in poor network conditions
- Case sensitivity in destination matching (mitigated with toUpperCase())
- No pagination for multiple results

## Related Documentation
- [Sign Management Feature](../sign-management/specification.md)
- [Google Places API Integration](../../technical/api-integration.md)
- [Firebase Architecture](../../technical/architecture.md)
