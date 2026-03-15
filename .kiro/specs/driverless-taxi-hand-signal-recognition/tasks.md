# Implementation Plan: Driverless Taxi Hand Signal Recognition

## Overview

Two projects share a single Firebase backend. Tasks are ordered by dependency: foundational
infrastructure first, then core detection pipeline, then passenger app, then operator dashboard,
then CI/model accuracy gate. Property-based tests are placed immediately after the component they
validate so errors are caught early.

---

## Tasks

<!-- ================================================================ -->
<!-- PROJECT 1: takeme-rb5 (Python · Ubuntu 20.04 · Qualcomm RB5)     -->
<!-- ================================================================ -->

## Part 1 — `takeme-rb5` Python Service

- [ ] 1. Bootstrap `takeme-rb5` project structure and dependencies
  - Create `takeme-rb5/` directory with the layout defined in the design doc
  - Write `requirements.txt` with `snpe-python`, `numpy>=1.24`, `opencv-python>=4.8`,
    `firebase-admin>=6.3`, `pytest>=7.4`, `hypothesis>=6.100`, `pytest-mock>=3.12`
  - Write `main.py` entry point with a service loop stub that wires all modules together
  - Write `config/` directory with a `.gitignore` that excludes `service_account.json`
  - _Requirements: 1.1, 2.1, 7.1_

- [ ] 2. Implement `FrameQualityFilter`
  - [ ] 2.1 Implement `FrameQualityFilter` class in `camera/camera_feed_manager.py`
    - `accept(frame, width, height) -> bool`: return `False` for height < 480, log quality warning
      with frame timestamp and measured resolution
    - `record_frame_timestamp(timestamp_ms)`: maintain a sliding 1-second window for fps tracking
    - `get_measured_fps() -> float`: return fps computed from the sliding window
    - _Requirements: 1.1.1, 1.1.2, 1.3.1, 1.3.2_

  - [ ]* 2.2 Write property test for `FrameQualityFilter` (Property 1)
    - **Property 1: Frame Quality Gate**
    - **Validates: Requirements 1.3.1, 1.3.2**
    - Use `@given(height=st.integers(min_value=1, max_value=479))` to assert `accept()` returns
      `False` and a quality warning log entry is produced for any sub-480p frame

  - [ ]* 2.3 Write unit tests for `FrameQualityFilter`
    - Exact 480p boundary accepted, 479p rejected, 481p accepted
    - fps warning logged when rate drops below 10 for > 1 second
    - _Requirements: 1.1.1, 1.1.2, 1.3.1, 1.3.2_


- [ ] 3. Implement `CameraFeedManager`
  - [ ] 3.1 Implement `CameraFeedManager` class in `camera/camera_feed_manager.py`
    - `start_feed(device, quality_filter, detector)`: open `/dev/videoN` via
      `cv2.VideoCapture(device, cv2.CAP_V4L2)`, set `CAP_PROP_FPS >= 10`,
      `CAP_PROP_FRAME_WIDTH >= 640`, `CAP_PROP_FRAME_HEIGHT >= 480`; run frame loop
    - `stop_feed()`, `is_feed_healthy() -> bool`
    - `set_failure_listener(listener)`: call listener within 500ms of `read()` returning `False`
    - _Requirements: 1.1.1, 1.2.1, 1.2.2, 1.3.3_

  - [ ]* 3.2 Write property test for camera failure notification timing (Property 23)
    - **Property 23: Camera Feed Failure Notification Timing**
    - **Validates: Requirements 1.2.1, 1.2.2**
    - Use `@given(loss_time=st.integers(min_value=0, max_value=10000))` with a mocked
      `VideoCapture` that returns `False`; assert `DetectionLogger.log()` called within 500ms
      and `DispatchService.report_camera_failure()` called within 2000ms

  - [ ]* 3.3 Write unit tests for `CameraFeedManager`
    - Feed loss triggers failure listener within 500ms
    - `is_feed_healthy()` returns `False` after feed loss
    - _Requirements: 1.2.1, 1.2.2_

- [ ] 4. Implement `HandSignalDetector`
  - [ ] 4.1 Implement `HandSignalDetector` class in `detector/hand_signal_detector.py`
    - `detect(frame) -> DetectionResult`: run SNPE inference on Hexagon DSP; must return within
      200ms; emit `PickupRequestEvent` via listener when `confidence_score >= 0.75`
    - `load_model(model_path, new_version)`: hot-swap `.dlc` without service restart; log
      previous and new version with timestamp
    - `get_current_model_version() -> str`
    - `set_pickup_request_listener(listener)`
    - Enter `UNAVAILABLE` state on SNPE runtime exception; log `INFERENCE_ERROR`
    - _Requirements: 2.1.1, 2.2.1, 2.2.2, 2.2.3, 9.1.1, 9.1.2, 10.3.1, 10.3.2_

  - [ ]* 4.2 Write property test for confidence threshold emission (Property 2)
    - **Property 2: Confidence Threshold Emission**
    - **Validates: Requirements 2.2.1, 2.2.2, 2.2.3**
    - `@given(score=st.floats(min_value=0.0, max_value=1.0, allow_nan=False))`: assert event
      emitted iff `score >= 0.75`; when emitted, payload contains non-null GPS, positive
      timestamp, and same `confidence_score`

  - [ ]* 4.3 Write property test for inference latency bound (Property 3)
    - **Property 3: Inference Latency Bound**
    - **Validates: Requirements 2.1.1**
    - `@given(frame=st.just(np.zeros((480, 640, 3), dtype=np.uint8)))`: assert `detect()` returns
      within 200ms wall-clock time using a mocked SNPE runtime

  - [ ]* 4.4 Write unit tests for `HandSignalDetector`
    - Model load/swap succeeds; `get_current_model_version()` returns new version after swap
    - SNPE runtime exception → `UNAVAILABLE` state, `INFERENCE_ERROR` logged
    - Corrupt model file → `InvalidModelError` raised; previous `.dlc` retained
    - _Requirements: 2.1.1, 2.2.1, 9.1.1, 10.3.1, 10.3.2_


- [ ] 5. Implement `DetectionLogger`
  - [ ] 5.1 Implement `DetectionLogger` class in `detector/detection_logger.py`
    - `log(entry: dict)`: write `DetectionLogEntry` with `detectionId`, `vehicleId`,
      `confidenceScore`, `outcome`, `hourOfDay`, `lightingEstimate`, `timestampMs`; no image
      bytes, no PII
    - `get_false_positive_rate(rolling_days: int) -> float`: compute FP rate from local log
    - Sync log entries to `/detection_logs/{detectionId}` via `DispatchService`
    - _Requirements: 8.1.1, 8.1.2, 8.1.3, 10.1.1, 10.1.2, 10.1.3_

  - [ ]* 5.2 Write property test for detection log completeness and anonymisation (Property 20)
    - **Property 20: Detection Log Completeness and Anonymisation**
    - **Validates: Requirements 10.1.1, 10.1.2, 10.1.3**
    - `@given(score=st.floats(0.0, 1.0), outcome=st.sampled_from(["CONFIRMED_PICKUP",
      "UNCONFIRMED", "FALSE_POSITIVE"]))`: assert every `log()` call produces an entry with
      non-null `detectionId`, valid `confidenceScore`, non-null `outcome`, `hourOfDay` in [0,23],
      non-null `lightingEstimate`, and no image-data fields

  - [ ]* 5.3 Write unit tests for `DetectionLogger`
    - Log entry contains no image bytes or passenger PII
    - `get_false_positive_rate()` returns correct rate at 0%, 10%, 11%
    - _Requirements: 8.1, 10.1, 10.2_

- [ ] 6. Implement `RouteManager`
  - [ ] 6.1 Implement `RouteManager` class in `route/route_manager.py`
    - `evaluate_pickup_request(event, current_speed_kmh) -> str`: call Roads API; return
      `STOP_HERE` / `STOP_AHEAD` / `CANCEL` within 500ms; on Roads API timeout return `CANCEL`
      with reason `"ROADS_API_TIMEOUT"`
    - `find_next_safe_stop_zone(lat, lng, lookahead_metres) -> tuple | None`
    - `initiate_deceleration_sequence(stop_lat, stop_lng)`: enforce 3-second following distance
      invariant before decelerating
    - `abort_stop(reason, abort_lat, abort_lng, timestamp_ms)`: call
      `DispatchService.retain_event_log()` with `OBSTACLE_ABORT` event
    - `calculate_route(from_lat, from_lng, to_lat, to_lng) -> list`
    - _Requirements: 3.1.1, 3.1.2, 3.2.1, 3.2.2, 3.2.3, 9.3.1, 9.3.2, 9.4.1_

  - [ ]* 6.2 Write property test for safe stop decision correctness (Property 4)
    - **Property 4: Safe Stop Decision Correctness**
    - **Validates: Requirements 3.1.1, 3.1.2, 3.2.1, 3.2.2, 3.2.3**
    - `@given(speed=st.floats(0, 120), is_safe_zone=st.booleans(),
      next_safe_dist=st.floats(0, 300))`: assert `STOP_HERE` when safe zone and speed < 60,
      `STOP_AHEAD` when unsafe but next safe zone ≤ 200m, `CANCEL` when > 200m; assert
      `DispatchService.report_pickup_request_cancelled()` called on `CANCEL`

  - [ ]* 6.3 Write property test for obstacle abort notification (Property 18)
    - **Property 18: Obstacle Abort Notification**
    - **Validates: Requirements 9.3.1, 9.3.2**
    - `@given(lat=st.floats(-90, 90), lng=st.floats(-180, 180),
      ts=st.integers(min_value=1))`: assert `abort_stop()` triggers `retain_event_log()` with
      `OBSTACLE_ABORT` event containing non-null coordinates and positive `timestampMs`

  - [ ]* 6.4 Write property test for safe following distance invariant (Property 19)
    - **Property 19: Safe Following Distance Invariant**
    - **Validates: Requirements 9.4.1**
    - `@given(speed_kmh=st.floats(0, 120))`: assert minimum following distance ≥
      `speed_kmh / 3.6 * 3` metres before any deceleration sequence is initiated

  - [ ]* 6.5 Write unit tests for `RouteManager`
    - Roads API timeout → `CANCEL` with reason `"ROADS_API_TIMEOUT"`
    - Exactly 200m boundary: safe stop at 200m → `STOP_AHEAD`; at 201m → `CANCEL`
    - _Requirements: 3.1.1, 3.2.1, 3.2.2_


- [ ] 7. Implement `BoardingController`
  - [ ] 7.1 Implement `BoardingController` class in `boarding/boarding_controller.py`
    - `await_boarding_confirmation(event, on_confirmed, on_timeout)`: push boarding prompt to
      Firebase within 5s of vehicle stop; fire `on_confirmed` or `on_timeout` within 60s
    - `cancel_boarding_timeout(pickup_request_id)`: cancel pending timeout
    - On timeout: set `PickupRequestStatus` to `UNCONFIRMED` via `DispatchService`
    - _Requirements: 4.1.1, 4.3.1, 4.3.2_

  - [ ]* 7.2 Write property test for boarding confirmation creates complete trip (Property 5)
    - **Property 5: Boarding Confirmation Creates Complete Trip**
    - **Validates: Requirements 4.2.1, 4.2.2**
    - `@given(event=pickup_request_strategy)`: assert `DispatchService.create_trip()` called with
      non-null `pickupCoordinates`, positive `pickupTimestampMs`, non-null `passengerId`, non-null
      `vehicleId`

  - [ ]* 7.3 Write unit tests for `BoardingController`
    - Confirmation at 59s fires `on_confirmed`
    - Timeout at exactly 60s fires `on_timeout`; `PickupRequestStatus` set to `UNCONFIRMED`
    - Boarding prompt sent to Firebase within 5s of stop
    - _Requirements: 4.1.1, 4.3.1, 4.3.2_

- [ ] 8. Implement `DispatchService`
  - [ ] 8.1 Implement `DispatchService` class in `dispatch/dispatch_service.py`
    - `publish_fleet_status(status)`: write to `/fleet_status/{vehicleId}` at ≤ 5s intervals
    - `report_camera_failure(vehicle_id, timestamp_ms)`: write `FleetEvent(CAMERA_FAILURE)`;
      set `requiresInspection = True` in `/fleet_status/{vehicleId}`
    - `report_pickup_request_cancelled(request_id, reason, lat, lng, timestamp_ms)`: write to
      `/fleet_events` within 10s
    - `create_trip(trip)`, `update_trip(trip)`
    - `retain_event_log(event)`: write to `/fleet_events`; TTL rule enforces 30-day retention
    - `flag_vehicle_for_inspection(vehicle_id)`
    - `schedule_reconnect()`: retry every 30s on disconnection; buffer events locally; flush on
      reconnect
    - _Requirements: 7.1.1, 7.2.1, 7.2.2, 7.3.1, 7.3.2, 7.4.1, 9.2.1, 9.2.2_

  - [ ]* 8.2 Write property test for fleet telemetry publish interval (Property 12)
    - **Property 12: Fleet Telemetry Publish Interval**
    - **Validates: Requirements 7.1.1**
    - `@given(n=st.integers(min_value=2, max_value=20))`: simulate `n` consecutive
      `publish_fleet_status()` calls; assert all consecutive `timestampMs` deltas ≤ 5000ms

  - [ ]* 8.3 Write property test for cancellation event surfacing (Property 13)
    - **Property 13: Cancellation Event Surfacing**
    - **Validates: Requirements 7.2.1, 7.2.2**
    - `@given(event=pickup_request_strategy)`: assert `/fleet_events` entry appears within 10s
      with non-null `gpsLat`, `gpsLng`, `timestampMs`, and non-empty `detail`

  - [ ]* 8.4 Write property test for camera failure alert and vehicle flag (Property 14)
    - **Property 14: Camera Failure Alert and Vehicle Flag**
    - **Validates: Requirements 7.3.1, 7.3.2**
    - `@given(vehicle_id=st.text(min_size=1), ts=st.integers(min_value=1))`: assert
      `FleetEvent(CAMERA_FAILURE)` written and `requiresInspection = True` set in
      `/fleet_status/{vehicleId}`

  - [ ]* 8.5 Write property test for reconnect interval (Property 17)
    - **Property 17: Reconnect Interval**
    - **Validates: Requirements 9.2.2**
    - `@given(n=st.integers(min_value=2, max_value=5))`: simulate `n` reconnect attempts;
      assert consecutive attempt timestamps are ≥ 30000ms apart

  - [ ]* 8.6 Write unit tests for `DispatchService`
    - Firebase write retry with exponential backoff (3 attempts)
    - Reconnect scheduling: `schedule_reconnect()` fires every 30s
    - Buffered events flushed on reconnect
    - _Requirements: 7.1.1, 9.2.1, 9.2.2_


- [ ] 9. Implement `ModelUpdateManager`
  - [ ] 9.1 Implement `ModelUpdateManager` class in `model/model_update_manager.py`
    - `check_for_update()`: fetch Remote Config manifest; compare `model_version` to on-device
      version; download new `.dlc` from Firebase Storage if version differs
    - `apply_update(model_path, new_version)`: atomically swap model file; call
      `HandSignalDetector.load_model()`; write `/model_versions/{vehicleId}` with previous
      version, new version, and `updatedAtMs`
    - `get_current_model_version() -> str`
    - On Remote Config fetch failure: retain current `.dlc`; retry on next service restart
    - On Storage download failure: retain current `.dlc`; log `MODEL_DOWNLOAD_ERROR`
    - _Requirements: 10.3.1, 10.3.2_

  - [ ]* 9.2 Write property test for OTA model update and version logging (Property 22)
    - **Property 22: OTA Model Update and Version Logging**
    - **Validates: Requirements 10.3.1, 10.3.2**
    - `@given(old_ver=st.text(min_size=1), new_ver=st.text(min_size=1))`: assert after
      `apply_update()`, `get_current_model_version()` returns `new_ver` and
      `/model_versions/{vehicleId}` contains previous version, new version, and positive
      `updatedAtMs`

  - [ ]* 9.3 Write unit tests for `ModelUpdateManager`
    - Same version → no download triggered
    - Corrupt downloaded file → retain previous `.dlc`; `MODEL_DOWNLOAD_ERROR` logged
    - Remote Config fetch failure → retain current `.dlc`
    - _Requirements: 10.3.1, 10.3.2_

- [ ] 10. Implement false-positive rate monitoring in `DetectionLogger` + `DispatchService`
  - [ ] 10.1 Add FP rate alert logic to `DetectionLogger` and `DispatchService`
    - `DetectionLogger.get_false_positive_rate(rolling_days=7)`: compute rate from
      `/detection_logs` entries in the rolling window
    - When rate > 0.10: call `DispatchService.retain_event_log()` with `FP_RATE_ALERT` event;
      set `modelFlaggedForRetraining = True` in `/model_versions/{vehicleId}`
    - _Requirements: 10.2.1, 10.2.2_

  - [ ]* 10.2 Write property test for false-positive rate monitoring and alert (Property 21)
    - **Property 21: False-Positive Rate Monitoring and Alert**
    - **Validates: Requirements 10.2.1, 10.2.2**
    - `@given(fp_rate=st.floats(min_value=0.0, max_value=1.0))`: assert `FP_RATE_ALERT` event
      written and `modelFlaggedForRetraining = True` iff `fp_rate > 0.10`

- [ ] 11. Implement privacy enforcement in `PickupRequestEvent` and `DispatchService`
  - [ ] 11.1 Add runtime field validation to `DispatchService` write methods
    - Before any write to `/pickup_requests`, assert the payload contains only `requestId`,
      `vehicleId`, `gpsCoordinates`, `timestampMs`, `confidenceScore`, `status`
    - Raise `PrivacyViolationError` if any image-data or biometric field is present
    - _Requirements: 8.1.2, 8.1.3, 8.2.1, 8.2.2_

  - [ ]* 11.2 Write property test for privacy — no image data in transmission or storage (Property 15)
    - **Property 15: Privacy — No Image Data in Transmission or Storage**
    - **Validates: Requirements 8.1.2, 8.1.3, 8.2.1, 8.2.2**
    - `@given(event=pickup_request_strategy)`: assert no field in the written payload contains
      raw image bytes, file paths to image files, or passenger biometric data

- [ ] 12. Implement detector unavailability fallback in `RouteManager`
  - [ ] 12.1 Add `DETECTOR_UNAVAILABLE` guard to `RouteManager.evaluate_pickup_request()`
    - When `HandSignalDetector` is in `UNAVAILABLE` state, return `CANCEL` with reason
      `"DETECTOR_UNAVAILABLE"` and call `DispatchService.report_pickup_request_cancelled()`
    - _Requirements: 9.1.1, 9.1.2_

  - [ ]* 12.2 Write property test for detector unavailability fallback (Property 16)
    - **Property 16: Detector Unavailability Fallback**
    - **Validates: Requirements 9.1.1, 9.1.2**
    - `@given(event=pickup_request_strategy)`: with detector in `UNAVAILABLE` state, assert
      `evaluate_pickup_request()` returns `CANCEL` and
      `report_pickup_request_cancelled()` called with reason `"DETECTOR_UNAVAILABLE"`

- [ ] 13. Checkpoint — `takeme-rb5` core pipeline complete
  - Ensure all `takeme-rb5` tests pass: `pytest tests/ --tb=short`
  - Ask the user if questions arise before proceeding to the Android project.


<!-- ================================================================ -->
<!-- PROJECT 2: takeme-mobile (Java · Android · minSdkVersion 21)     -->
<!-- ================================================================ -->

## Part 2 — `takeme-mobile` Android App

- [x] 14. Add new dependencies to `app/build.gradle`
  - Add `testImplementation 'net.jqwik:jqwik:1.8.1'`
  - Add `testImplementation 'org.mockito:mockito-core:4.11.0'`
  - Add `testImplementation 'org.assertj:assertj-core:3.24.2'`
  - Add Peach Payments Checkout SDK dependency
  - Add `implementation 'com.google.firebase:firebase-config:19.2.0'` for Remote Config
  - Ensure `testOptions.unitTests.returnDefaultValues = true` is set (already present)
  - _Requirements: 6.2, 6.3, 10.3_

- [x] 15. Define shared data model classes
  - [x] 15.1 Create `model/PickupRequestEvent.java` with fields and `PickupRequestStatus` enum
    - Fields: `requestId`, `vehicleId`, `gpsCoordinates (LatLng)`, `timestampMs`,
      `confidenceScore`, `status`
    - _Requirements: 2.2.1, 2.2.2, 4.1, 4.2_

  - [x] 15.2 Create `model/Trip.java` with fields and `TripStatus` / `PaymentStatus` enums
    - Fields: `tripId`, `vehicleId`, `passengerId`, `pickupCoordinates`, `dropoffCoordinates`,
      `pickupTimestampMs`, `dropoffTimestampMs`, `distanceKm`, `fare`, `status`,
      `paymentStatus`, `paymentAttempts`
    - _Requirements: 4.2.2, 6.1, 6.2, 6.3, 6.4_

  - [x] 15.3 Create `model/Fare.java`, `model/VehicleStatus.java`, `model/DetectionLogEntry.java`,
    `model/FleetEvent.java` matching the design doc data models
    - _Requirements: 6.1.1, 7.1.1, 10.1_

  - [ ]* 15.4 Write jqwik property test for `Fare` completeness
    - `@Property`: for any non-negative `distanceKm` and positive `ratePerKm`, assert
      `fare.amountRand == distanceKm * ratePerKm` within floating-point epsilon and
      `fare.currency` is non-null
    - **Property 8: Fare Calculation Proportionality**
    - **Validates: Requirements 6.1.1**

- [x] 16. Implement `TripController`
  - [x] 16.1 Create `impl/TripController.java` interface and `impl/TripControllerImpl.java`
    - `startTrip(Trip)`: write trip to `/trips/{tripId}` via Firebase RTDB
    - `onDestinationSelected(Trip, LatLng)`: call `RouteManager` (Directions API) to calculate
      optimal route; update trip record
    - `calculateFare(distanceKm, currencyCode) -> Fare`: `amountRand = distanceKm * ratePerKm`
      from Remote Config; currency from `currencyCode` param (jurisdiction-aware, req 8.4)
    - `completeTrip(Trip, PaymentConfirmation)`: set `paymentStatus = PAID`,
      `status = COMPLETED`; write to RTDB
    - `checkArrivalProximity(Trip, LatLng)`: if distance to `dropoffCoordinates` ≤ 50m, call
      `DispatchService` to notify passenger
    - Payment retry: up to 3 Peach Payments API calls with exponential backoff (1s, 2s, 4s);
      after 3 failures set `paymentStatus = FLAGGED`; write `FleetEvent(PAYMENT_FAILURE)`
    - _Requirements: 5.1.2, 5.2.1, 5.3.1, 5.3.2, 6.1.1, 6.2.1, 6.2.2, 6.3.1, 6.3.2, 6.4.1,
      8.4_

  - [ ]* 16.2 Write jqwik property test for fare calculation proportionality (Property 8)
    - **Property 8: Fare Calculation Proportionality**
    - **Validates: Requirements 6.1.1**
    - `@Property @ForAll @DoubleRange(min=0.0, max=500.0) double distanceKm,
      @ForAll @DoubleRange(min=0.01, max=10.0) double ratePerKm`: assert
      `fare.amountRand ≈ distanceKm * ratePerKm` and `fare.currency != null`

  - [ ]* 16.3 Write jqwik property test for payment confirmation completes trip (Property 9)
    - **Property 9: Payment Confirmation Completes Trip**
    - **Validates: Requirements 6.2.1, 6.2.2**
    - `@Property`: for any trip and successful `PaymentConfirmation`, assert
      `trip.paymentStatus == PAID` and `trip.status == COMPLETED` after `completeTrip()`

  - [ ]* 16.4 Write jqwik property test for payment retry and escalation (Property 10)
    - **Property 10: Payment Retry and Escalation**
    - **Validates: Requirements 6.3.1, 6.3.2**
    - `@Property`: simulate all payment attempts failing; assert Peach API called exactly 3
      times and `paymentStatus == FLAGGED` after exhaustion

  - [ ]* 16.5 Write jqwik property test for completed trip data completeness (Property 11)
    - **Property 11: Completed Trip Data Completeness**
    - **Validates: Requirements 6.4.1**
    - `@Property`: for any completed trip written to `/trips/{tripId}`, assert non-null values
      for `pickupLat`, `pickupLng`, `dropoffLat`, `dropoffLng`, `distanceKm`, `fareAmount`,
      `status`, `paymentStatus`

  - [ ]* 16.6 Write jqwik property test for arrival proximity notification (Property 6)
    - **Property 6: Arrival Proximity Notification**
    - **Validates: Requirements 5.2.1**
    - `@Property`: for any active trip, when vehicle position is within 50m of
      `dropoffCoordinates`, assert `DispatchService` notification dispatched

  - [ ]* 16.7 Write jqwik property test for out-of-zone destination handling (Property 7)
    - **Property 7: Out-of-Zone Destination Handling**
    - **Validates: Requirements 5.3.1, 5.3.2**
    - `@Property`: for any destination outside the operating zone polygon, assert
      `DispatchService` sends out-of-zone notification and suggests nearest valid drop-off point

  - [ ]* 16.8 Write unit tests for `TripController`
    - `calculateFare()` returns correct ZAR amount
    - Payment retry fires exactly 3 times before `FLAGGED`
    - `checkArrivalProximity()` at exactly 50m triggers notification; at 51m does not
    - _Requirements: 5.2.1, 6.1.1, 6.3.1_


- [x] 17. Implement boarding confirmation UI
  - [x] 17.1 Create `BoardingConfirmationActivity.java`
    - Listen on `/pickup_requests/{requestId}` via Firebase RTDB `onValue()` listener
    - Display boarding prompt with "Confirm Boarding" button
    - On confirm: call `TripController.startTrip()` and write `status = CONFIRMED` to RTDB
    - On 60s timeout: write `status = UNCONFIRMED`; dismiss activity
    - _Requirements: 4.1.1, 4.2.1, 4.2.2, 4.3.1, 4.3.2_

  - [ ]* 17.2 Write Espresso integration test for boarding confirmation flow
    - `BoardingConfirmationFlowTest`: RTDB boarding prompt → passenger confirms → Trip created
      with correct fields in RTDB
    - _Requirements: 4.1.1, 4.2.1, 4.2.2_

- [x] 18. Implement destination selection UI
  - [ ] 18.1 Create `DestinationSelectionActivity.java`
    - Reuse `Location.java` `setPlace()` with `AutocompleteSupportFragment` for destination
      input
    - On place selected: call `TripController.onDestinationSelected()` with `LatLng`
    - Display calculated route on Google Maps fragment
    - If destination outside zone: show out-of-zone message and nearest valid drop-off
      suggestion
    - _Requirements: 5.1.1, 5.1.2, 5.3.1, 5.3.2_

  - [ ]* 18.2 Write Espresso integration test for destination selection
    - `DestinationSelectionTest`: Places autocomplete → route calculation triggered; out-of-zone
      destination shows correct error message and suggestion
    - _Requirements: 5.1.1, 5.1.2, 5.3.1, 5.3.2_

- [x] 19. Implement fare presentation and payment screen
  - [x] 19.1 Create `PaymentActivity.java`
    - Display `Fare.amountRand` in ZAR (or jurisdiction currency) before payment
    - Integrate Peach Payments Checkout SDK for payment sheet UI
    - On payment success: call `TripController.completeTrip()`
    - On payment failure: show retry UI; after 3 failures show "payment flagged" message
    - _Requirements: 6.1.2, 6.2.1, 6.2.2, 6.3.1, 6.3.2_

  - [ ]* 19.2 Write Espresso integration test for payment flow
    - `PaymentFlowTest`: Peach Payments sandbox → `Trip.status` updates to `COMPLETED` in RTDB
    - _Requirements: 6.2.1, 6.2.2_

- [x] 20. Checkpoint — `takeme-mobile` core flows complete
  - All activities compile with zero diagnostics
  - All three new activities registered in AndroidManifest.xml
  - Unit tests in TripControllerTest pass


<!-- ================================================================ -->
<!-- SHARED: Firebase Backend Setup                                    -->
<!-- ================================================================ -->

## Part 3 — Shared Firebase Backend

- [ ] 21. Configure Firebase Realtime Database schema and security rules
  - [ ] 21.1 Write `firebase/database.rules.json` with the security rules from the design doc
    - `fleet_status/{vehicleId}`: read = `auth != null`; write = `auth.uid === $vehicleId`
    - `trips/{tripId}`: read/write scoped to `passengerId` or operator token
    - `detection_logs`: read = `auth.token.operator === true`; write = `auth != null`
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 21.2 Write `firebase/storage.rules` restricting `.dlc` uploads to CI service account
    - Only the CI service account may write to `model-binaries/` path
    - _Requirements: 10.3, 10.4_

  - [ ] 21.3 Configure `/fleet_events` TTL rule (30-day retention)
    - Add Firebase TTL rule or Cloud Function cleanup job that deletes entries older than 30 days
    - _Requirements: 7.4.1_

- [ ] 22. Implement Firebase Cloud Functions
  - [ ] 22.1 Create `functions/peachPaymentsProxy.js`
    - HTTP callable function that accepts `{ tripId, amountRand, currency }` and calls Peach
      Payments REST API using server-side secret key; returns `{ success, transactionId }`
    - _Requirements: 6.2.1, 6.3.1_

  - [ ] 22.2 Create `functions/fpRateAggregator.js`
    - Scheduled function (runs daily) that reads `/detection_logs`, computes rolling 7-day FP
      rate per vehicle, writes result to `/model_versions/{vehicleId}/fpRate7d`; triggers
      `FP_RATE_ALERT` if rate > 0.10
    - _Requirements: 10.2.1, 10.2.2_

  - [ ]* 22.3 Write unit tests for Cloud Functions
    - `peachPaymentsProxy`: mock Peach API; assert correct request shape and error handling
    - `fpRateAggregator`: assert alert triggered at 10.1%, not triggered at 9.9%
    - _Requirements: 6.3.1, 10.2.1_

<!-- ================================================================ -->
<!-- OPERATOR DASHBOARD: React 18 + Firebase JS SDK                   -->
<!-- ================================================================ -->

## Part 4 — Operator Dashboard (React 18 + TypeScript)

- [ ] 23. Bootstrap operator dashboard project
  - Create `takeme-dashboard/` with Vite + React 18 + TypeScript scaffold
  - Add dependencies: `firebase@10.x`, `@react-google-maps/api`, `recharts`
  - Configure Firebase JS SDK with the shared Firebase project credentials
  - _Requirements: 7.1, 7.2, 7.3, 10.2_

- [ ] 24. Implement live fleet map component
  - [ ] 24.1 Create `components/FleetMap.tsx`
    - Use `onValue('/fleet_status')` listener to receive real-time vehicle positions
    - Render each vehicle as a marker on Google Maps JavaScript API map
    - Update marker position on each RTDB push (≤ 5s interval)
    - _Requirements: 7.1.1_

  - [ ]* 24.2 Write unit tests for `FleetMap`
    - Mock `onValue` with synthetic RTDB snapshots; assert markers rendered for each vehicle
    - _Requirements: 7.1.1_

- [ ] 25. Implement alert panel component
  - [ ] 25.1 Create `components/AlertPanel.tsx`
    - Listen on `/fleet_events` filtered by `eventType` in
      `[CAMERA_FAILURE, FP_RATE_ALERT, PAYMENT_FAILURE]`
    - Display each alert with `vehicleId`, `eventType`, `timestampMs`, and `detail`
    - Camera failure alerts highlight the affected vehicle on the fleet map
    - _Requirements: 1.2.3, 7.3.1, 10.2.1_

  - [ ]* 25.2 Write unit tests for `AlertPanel`
    - Mock RTDB snapshot with `CAMERA_FAILURE` event; assert alert rendered with correct fields
    - _Requirements: 7.3.1_

- [ ] 26. Implement trip review panel
  - [ ] 26.1 Create `components/TripReviewPanel.tsx`
    - Listen on `/trips` filtered by `paymentStatus == FLAGGED`
    - Display flagged trips with `tripId`, `vehicleId`, `fareAmount`, and failure reason from
      `detail`
    - _Requirements: 6.3.2_

  - [ ]* 26.2 Write unit tests for `TripReviewPanel`
    - Mock RTDB snapshot with a `FLAGGED` trip; assert trip rendered with correct fields
    - _Requirements: 6.3.2_

- [ ] 27. Implement false-positive rate trend chart
  - [ ] 27.1 Create `components/FpRateChart.tsx`
    - Read `/model_versions/{vehicleId}/fpRate7d` for each vehicle
    - Render a Recharts `LineChart` showing FP rate trend over time per vehicle
    - Highlight vehicles where `fpRate7d > 0.10` in red
    - _Requirements: 10.2.1, 10.2.2_

  - [ ]* 27.2 Write unit tests for `FpRateChart`
    - Assert red highlight applied when `fpRate7d > 0.10`
    - _Requirements: 10.2.1_

- [ ] 28. Checkpoint — Operator dashboard complete
  - Ensure all dashboard tests pass
  - Ask the user if questions arise before proceeding to CI pipeline.


<!-- ================================================================ -->
<!-- CI / MODEL ACCURACY GATE                                         -->
<!-- ================================================================ -->

## Part 5 — CI Pipeline and Model Accuracy Gate

- [ ] 29. Implement model accuracy evaluator script
  - [ ] 29.1 Create `takeme-rb5/ci/model_accuracy_evaluator.py`
    - Accept `--model-path` (`.dlc` file) and `--dataset-path` (validation dataset dir)
    - Run inference on each sample in the dataset using `HandSignalDetector`
    - Compute accuracy as `correct_detections / total_samples`
    - Print accuracy score; exit with code 1 if accuracy < 0.85
    - _Requirements: 10.4.1, 10.4.2_

  - [ ] 29.2 Create `takeme-rb5/ci/convert_model.sh`
    - Shell script that converts a source `.tflite` or `.onnx` to `.dlc` using the SNPE model
      converter (`snpe-tensorflow-to-dlc` or `snpe-onnx-to-dlc`)
    - Outputs converted `.dlc` to `models/candidate.dlc`
    - _Requirements: 10.4.1_

- [ ] 30. Write CI pipeline configuration
  - [ ] 30.1 Create `.github/workflows/rb5-ci.yml` (or equivalent CI config)
    - Steps: install SNPE QDK, install Python deps, run `pytest tests/`, run
      `model_accuracy_evaluator.py` against validation dataset
    - Fail pipeline if accuracy < 85%
    - On pass: upload `models/candidate.dlc` to Firebase Storage; update Remote Config manifest
      with new `model_version` and `model_url`
    - Notify Operator (via `DispatchService.retain_event_log()`) with measured accuracy score
      on both pass and fail
    - _Requirements: 10.4.1, 10.4.2_

  - [ ] 30.2 Create `.github/workflows/mobile-ci.yml`
    - Steps: set up JDK 11, run `./gradlew test` (unit tests including jqwik), run
      `./gradlew connectedAndroidTest` (Espresso) on emulator
    - _Requirements: 6.2, 6.3, 10.3_

  - [ ] 30.3 Create `.github/workflows/dashboard-ci.yml`
    - Steps: `npm ci`, `npm test -- --run`, `npm run build`
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] 31. Final checkpoint — all projects integrated
  - Ensure all tests pass across `takeme-rb5`, `takeme-mobile`, and `takeme-dashboard`
  - Verify Firebase security rules deployed and TTL rule active
  - Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Properties 1–23 from the design doc are each covered by a dedicated property-based test sub-task
- `takeme-rb5` uses Hypothesis `@given` tests; `takeme-mobile` uses jqwik `@Property` tests
- The CI accuracy gate (task 29–30) must pass before any model reaches production vehicles
- Firebase Cloud Functions (task 22) keep Peach Payments secret keys out of the APK
