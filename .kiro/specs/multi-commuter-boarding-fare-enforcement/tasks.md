# Implementation Plan: Multi-Commuter Boarding & Fare Enforcement (Android)

## Overview

This plan covers the Android (`takeme-mobile`) side of the multi-commuter boarding and fare enforcement feature. Tasks are ordered by dependency: data models first, then the controller interface/implementation, then activities and UI, then wiring. The RB5 Python components are out of scope — the Android app communicates with them exclusively through Firebase RTDB.

## Tasks

- [x] 1. Add ZXing dependency and extend FleetEvent enum
  - [x] 1.1 Add ZXing QR scanning dependency to build.gradle
    - Add `implementation 'com.journeyapps:zxing-android-embedded:4.3.0'` to `app/build.gradle` dependencies block
    - _Requirements: 1.2.1_
  - [x] 1.2 Add new FleetEventType enum values
    - Add `FARE_EVASION_ALERT`, `FARE_EVASION_RESOLVED`, `SENSOR_FAILURE` to `FleetEvent.FleetEventType` in `model/FleetEvent.java`
    - _Requirements: 3.3.1, 3.3.4, 7.2.2_

- [x] 2. Create data models
  - [x] 2.1 Create CommuterTrip model class
    - Create `model/CommuterTrip.java` with fields: `commuterTripId`, `vehicleId`, `commuterUid`, `boardingLat`, `boardingLng`, `boardingTimestampMs`, `alightingLat`, `alightingLng`, `alightingTimestampMs`, `distanceKm`, `fareAmount`, `currency`, `status`, `paymentStatus`, `paymentAttempts`, `autoCompleted`, `checkinMethod`, `stopEventId`
    - Include no-arg constructor for Firebase deserialization, full constructor, and getters/setters
    - Follow existing `Trip.java` pattern with flat GPS fields
    - _Requirements: 1.1.2, 4.1.2, 5.1.1, 6.3.1_
  - [ ]* 2.2 Write property test for CommuterTrip data completeness
    - **Property 17: Completed Commuter_Trip Data Completeness**
    - Use jqwik `@Property` to generate arbitrary CommuterTrip instances with status COMPLETED and assert all required fields are non-null: `commuterUid`, `boardingLat`, `boardingLng`, `alightingLat`, `alightingLng`, `distanceKm` (≥ 0), `fareAmount` (≥ 0), `paymentStatus`, `autoCompleted`
    - **Validates: Requirements 6.3.1**
  - [x] 2.3 Create StopEvent model class
    - Create `model/StopEvent.java` with fields: `stopEventId`, `vehicleId`, `stopLat`, `stopLng`, `timestampMs`, `checkinIds` (List<String>), `checkoutIds` (List<String>), `seatCount`, `cameraHeadcount`, `checkinCount`, `reconciliationStatus`, `enforcementAction`
    - Include no-arg constructor for Firebase, full constructor, and getters/setters
    - _Requirements: 1.3.4, 6.2.1_
  - [ ]* 2.4 Write property test for StopEvent data completeness
    - **Property 5: StopEvent Data Completeness on Window Close**
    - Use jqwik `@Property` to generate StopEvent instances and assert: non-null GPS coordinates, positive timestamp, `checkinIds` list length equals `checkinCount`, non-null `seatCount` and `reconciliationStatus`
    - **Validates: Requirements 1.3.4, 6.2.1**
  - [x] 2.5 Create VehicleOccupancyState model class
    - Create `model/VehicleOccupancyState.java` with fields: `vehicleId`, `seatSensorCount`, `cameraHeadcount`, `checkedInCount`, `activeCommuterTrips`, `maxCapacity`, `boardingWindowOpen`, `alightingWindowOpen`, `gracePeriodStopsRemaining`, `timestampMs`
    - Include no-arg constructor for Firebase, full constructor, and getters/setters
    - _Requirements: 6.1.2_
  - [ ]* 2.6 Write property test for VehicleOccupancyState data completeness
    - **Property 16: Vehicle Occupancy State Data Completeness**
    - Use jqwik `@Property` to generate VehicleOccupancyState instances and assert: non-negative integers for `seatSensorCount`, `cameraHeadcount`, `checkedInCount`, `activeCommuterTrips`; positive `maxCapacity`; positive `timestampMs`
    - **Validates: Requirements 6.1.2**

- [x] 3. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement CommuterTripController interface and implementation
  - [x] 4.1 Create CommuterTripController interface
    - Create `impl/CommuterTripController.java` with methods: `checkIn(String vehicleId, double stopLat, double stopLng)`, `checkOut(String commuterTripId, double stopLat, double stopLng)`, `calculateCommuterFare(double distanceKm, String currencyCode)`, `processCommuterPayment(CommuterTrip trip, boolean paymentSuccess)`
    - Follow existing `TripController.java` interface pattern
    - _Requirements: 1.1.1, 1.1.2, 4.1.1, 4.1.2, 5.1.1, 5.2.1_
  - [x] 4.2 Create CommuterTripControllerImpl
    - Create `impl/CommuterTripControllerImpl.java` implementing `CommuterTripController`
    - Constructor takes `FirebaseDatabase` and `FirebaseRemoteConfig` (same pattern as `TripControllerImpl`)
    - `checkIn`: validates GPS proximity (≤30m via haversine), rejects duplicate check-ins for same vehicle by querying `/commuter_trips` for existing active trip with same UID and vehicleId, creates `CommuterTrip` with status BOARDING and writes to `/commuter_trips/{id}`
    - `checkOut`: updates `/commuter_trips/{id}` with alighting coords and timestamp, calculates distance via haversine, triggers fare calculation
    - `calculateCommuterFare`: reuses `rate_per_km` from Remote Config, returns `Fare` with `amountRand = distanceKm * ratePerKm`
    - `processCommuterPayment`: on success sets `paymentStatus=PAID`, `status=COMPLETED`; on failure retries up to 3 times, then sets `paymentStatus=FLAGGED`, `status=PAYMENT_FAILED`, publishes `PAYMENT_FAILURE` fleet event
    - _Requirements: 1.1.1, 1.1.2, 1.1.3, 1.1.4, 4.1.1, 4.1.2, 4.1.3, 4.1.4, 5.1.1, 5.1.2, 5.1.4, 5.2.1, 5.2.2, 5.2.3, 5.2.4_
  - [ ]* 4.3 Write property test for per-commuter fare calculation
    - **Property 14: Per-Commuter Fare Calculation**
    - Use jqwik `@Property` with `@ForAll @DoubleRange(min=0.0, max=500.0) double distanceKm` and `@ForAll @DoubleRange(min=0.01, max=100.0) double ratePerKm` to assert `calculateCommuterFare` returns `distanceKm * ratePerKm` within floating-point epsilon
    - **Validates: Requirements 5.1.1, 5.1.2, 5.1.4**
  - [ ]* 4.4 Write property test for duplicate check-in rejection
    - **Property 2: Duplicate Check-In Rejection**
    - Use jqwik `@Property` to generate commuter UIDs and vehicle IDs; mock Firebase to simulate an existing active trip; assert that a second `checkIn` call is rejected and no new trip is created
    - **Validates: Requirements 1.1.4**
  - [ ]* 4.5 Write property test for commuter payment lifecycle
    - **Property 15: Commuter Payment Lifecycle**
    - Use jqwik `@Property` to generate CommuterTrip instances; assert that on payment success `paymentStatus == PAID` and `status == COMPLETED`; on all 3 failures `paymentStatus == FLAGGED` and `status == PAYMENT_FAILED` and a `PAYMENT_FAILURE` fleet event is published; total payment API calls never exceed 3
    - **Validates: Requirements 5.2.2, 5.2.3, 5.2.4**
  - [ ]* 4.6 Write unit tests for CommuterTripControllerImpl
    - Test check-in creates CommuterTrip with correct fields and writes to Firebase
    - Test check-in rejects when GPS distance > 30m
    - Test check-out updates alighting coords and triggers fare calc
    - Test calculateCommuterFare with zero distance returns zero
    - Test processCommuterPayment success path
    - Test processCommuterPayment failure path flags after 3 retries
    - Use Mockito for FirebaseDatabase and FirebaseRemoteConfig (same pattern as `TripControllerTest.java`)
    - _Requirements: 1.1.1, 1.1.2, 1.1.4, 4.1.1, 4.1.2, 5.1.1, 5.2.2, 5.2.3_

- [x] 5. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement CommuterCheckInActivity
  - [x] 6.1 Create activity_commuter_check_in.xml layout
    - Create layout with: "Board" button (`btn_board`), status TextView (`tv_status`), countdown TextView (`tv_boarding_countdown`), ProgressBar, and a "Scan QR" fallback button (`btn_scan_qr`)
    - Follow existing `activity_boarding_confirmation.xml` layout pattern
    - _Requirements: 1.1.1, 1.2.1_
  - [x] 6.2 Create CommuterCheckInActivity Java class
    - Create `CommuterCheckInActivity.java` in root package
    - Accept `vehicleId` via Intent extra
    - Listen on `/vehicle_occupancy/{vehicleId}` for `boardingWindowOpen` state
    - On "Board" tap: get current GPS via `FusedLocationProviderClient`, validate ≤30m from vehicle using haversine, call `CommuterTripController.checkIn()`
    - Handle duplicate rejection: show Toast "Already checked in on this vehicle"
    - Handle capacity full: show Toast "Vehicle is full" (when `checkedInCount == maxCapacity`)
    - On "Scan QR" tap: launch `QrCheckInActivity`
    - Show boarding window countdown timer
    - _Requirements: 1.1.1, 1.1.4, 1.3.2, 8.1.2_
  - [x] 6.3 Register CommuterCheckInActivity in AndroidManifest.xml
    - Add `<activity>` entry for `CommuterCheckInActivity`
    - _Requirements: 1.1.1_

- [x] 7. Implement QrCheckInActivity
  - [x] 7.1 Create activity_qr_check_in.xml layout
    - Create layout with ZXing `DecoratedBarcodeView` scanner area, status TextView, and cancel button
    - _Requirements: 1.2.1_
  - [x] 7.2 Create QrCheckInActivity Java class
    - Create `QrCheckInActivity.java` in root package
    - Use ZXing `DecoratedBarcodeView` to scan QR codes
    - Decode `vehicleId` from QR payload
    - Validate decoded vehicleId matches nearby vehicle (GPS proximity ≤30m)
    - On valid scan: call `CommuterTripController.checkIn()` with `checkinMethod = "QR_CODE"`
    - On invalid QR: show error "Invalid QR code"
    - On vehicle mismatch: show error "QR code does not match nearby vehicle"
    - _Requirements: 1.2.1, 1.2.2, 1.2.3_
  - [ ]* 7.3 Write property test for QR check-in equivalence
    - **Property 3: QR Code Check-In Equivalence**
    - Use jqwik `@Property` to generate commuter UIDs and vehicle IDs; assert that a QR check-in produces a CommuterTrip with identical fields to an app check-in except `checkinMethod` is "QR_CODE" vs "APP"
    - **Validates: Requirements 1.2.2, 1.2.3**
  - [x] 7.4 Register QrCheckInActivity in AndroidManifest.xml
    - Add `<activity>` entry for `QrCheckInActivity`
    - Add camera permission `<uses-permission android:name="android.permission.CAMERA" />` if not already present
    - _Requirements: 1.2.1_

- [x] 8. Implement CommuterCheckOutActivity
  - [x] 8.1 Create activity_commuter_check_out.xml layout
    - Create layout with: "Alight" button (`btn_alight`), fare display TextView (`tv_fare_amount`), fare label (`tv_fare_label`), status message (`tv_status`), "Pay" button (`btn_pay`), ProgressBar
    - Follow existing `activity_payment.xml` layout pattern
    - _Requirements: 4.1.1, 5.1.3_
  - [x] 8.2 Create CommuterCheckOutActivity Java class
    - Create `CommuterCheckOutActivity.java` in root package
    - Accept `commuterTripId` and `vehicleId` via Intent extras
    - Listen on `/vehicle_occupancy/{vehicleId}` for `alightingWindowOpen` state
    - On "Alight" tap: call `CommuterTripController.checkOut()` with current GPS coords
    - After check-out: display calculated fare using `CommuterTripController.calculateCommuterFare()`
    - On "Pay" tap: trigger payment via `CommuterTripController.processCommuterPayment()`
    - Handle auto-completed trip notification: show "Your trip was auto-completed" message if `autoCompleted == true`
    - Handle payment success: show confirmation, finish activity
    - Handle payment failure: show retry UI (same pattern as `PaymentActivity`)
    - _Requirements: 4.1.1, 4.1.2, 4.1.3, 4.1.4, 5.1.1, 5.1.3, 5.2.1, 5.2.2, 5.3.2_
  - [x] 8.3 Register CommuterCheckOutActivity in AndroidManifest.xml
    - Add `<activity>` entry for `CommuterCheckOutActivity`
    - _Requirements: 4.1.1_

- [x] 9. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Wire activities into existing navigation and add Firebase listeners
  - [x] 10.1 Add navigation from BoardingConfirmationActivity to CommuterCheckInActivity
    - After existing boarding confirmation flow completes, launch `CommuterCheckInActivity` with the `vehicleId`
    - This connects the existing single-passenger boarding flow to the new multi-commuter check-in
    - _Requirements: 1.1.1_
  - [x] 10.2 Add Firebase RTDB listener for auto-completed trip notifications
    - In `CommuterCheckOutActivity` or a shared service, listen on `/commuter_trips/{id}` for `autoCompleted` flag changes
    - When `autoCompleted` becomes true, show notification to commuter with fare details
    - _Requirements: 5.3.2_
  - [x] 10.3 Add commuter trip history to existing trip listing
    - Query `/commuter_trips` filtered by `commuterUid == auth.uid` for trip history display
    - Reuse existing trip list UI patterns
    - _Requirements: 6.3.2_

- [x] 11. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik 1.8.1 (already in build.gradle)
- This plan covers only the Android (`takeme-mobile`) side; the RB5 Python components (BoardingManager, AlightingManager, OccupancyReconciler, SeatPressureSensorDriver, CabinHeadcountDetector, DoorLockController) are a separate implementation plan
- Properties 1, 4, 6, 7, 8, 9, 10, 11, 12, 13, 18, 19, 20 are RB5-side properties and are not included in this Android task list
- The Android app communicates with RB5 components exclusively through Firebase RTDB
