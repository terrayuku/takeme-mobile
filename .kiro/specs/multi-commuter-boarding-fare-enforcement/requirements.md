# Requirements Document

## Introduction

This feature extends the TakeMe driverless minibus taxi system to support multi-commuter boarding, alighting, per-commuter fare enforcement, and fare evasion detection. The existing system (driverless-taxi-hand-signal-recognition) handles hand signal detection, single-passenger trips, and payment. However, South African minibus taxis pick up and drop off multiple commuters at the same stop, and the current system has no mechanism for tracking multiple concurrent passengers, reconciling physical occupancy against digital check-ins, or calculating individual fares based on each commuter's actual distance travelled.

This spec introduces app-based check-in/check-out, seat pressure sensor integration, anonymous camera headcount reconciliation, fare evasion enforcement policies, and per-commuter trip lifecycle management. It builds on top of the existing Firebase RTDB schema, RB5 Python service, and Android app.

## Glossary

- **Commuter**: A person who boards the Autonomous_Vehicle for a trip. Multiple Commuters may be on board simultaneously.
- **Boarding_Manager**: The component on the RB5 that manages the boarding window, tracks check-ins, reconciles occupancy, and enforces departure policy.
- **Alighting_Manager**: The component on the RB5 that manages the alighting window, tracks check-outs, and triggers per-commuter fare calculation.
- **Occupancy_Reconciler**: The sub-component that compares seat pressure sensor count, camera headcount, and app check-in count to detect discrepancies.
- **Seat_Pressure_Sensor**: A physical sensor embedded in each seat that reports binary occupied/unoccupied state to the RB5.
- **Camera_Headcount**: An anonymous person count derived from the existing RB5 camera feed using object detection. Produces a count only — no identity, no biometrics.
- **StopEvent**: A data record grouping all boarding and alighting actions that occur at a single stop. Contains the stop GPS coordinates, timestamp, and lists of commuter check-ins and check-outs.
- **Commuter_Trip**: A per-commuter trip record tracking an individual Commuter's boarding stop, alighting stop, distance travelled, and fare. Distinct from the vehicle-level Trip.
- **Check_In**: The act of a Commuter registering their presence on the vehicle via the TakeMe app (tap "Board") or QR code scan.
- **Check_Out**: The act of a Commuter registering their departure from the vehicle via the TakeMe app (tap "Alight").
- **QR_Code**: A vehicle-specific QR code displayed inside the Autonomous_Vehicle that Commuters can scan as a fallback Check_In method.
- **Boarding_Window**: A configurable time period (default 90 seconds) after the vehicle stops during which Commuters may board and check in.
- **Alighting_Window**: A configurable time period (default 60 seconds) after the vehicle stops during which Commuters may alight and check out.
- **Fare_Evasion_Alert**: A fleet event of type FARE_EVASION_ALERT published to the Operator dashboard when occupancy exceeds check-ins and enforcement fails to resolve the discrepancy.
- **Grace_Period**: A configurable number of stops (default 2) during which an unresolved occupancy discrepancy is tolerated before escalation.
- **Safe_Stop**: A designated stop where the vehicle routes to if a fare evasion discrepancy remains unresolved after the Grace_Period.
- **Door_Lock_Controller**: The RB5 component that controls the vehicle's door lock actuator to prevent departure during enforcement holds.
- **Vehicle_Occupancy_State**: A real-time record of the vehicle's current occupancy: seat sensor count, camera headcount, checked-in commuter count, and active Commuter_Trip list.

---

## Requirements

---

### Requirement 1.1: App-Based Check-In on Boarding

**User Story:** As a Commuter, I want to check in via the TakeMe app when I board the minibus, so that the system knows I am on board and can track my trip.

#### Acceptance Criteria

1. WHEN a Commuter taps "Board" in the TakeMe app while within 30 metres of a stopped Autonomous_Vehicle, THE Boarding_Manager SHALL register the Commuter's Check_In and associate the Commuter's Firebase Auth UID with the current StopEvent.
2. WHEN a Check_In is registered, THE Boarding_Manager SHALL create a Commuter_Trip record containing the Commuter's UID, the boarding stop GPS coordinates, and the boarding timestamp.
3. WHEN a Check_In is registered, THE Boarding_Manager SHALL add the Commuter_Trip to the Vehicle_Occupancy_State active trip list.
4. IF a Commuter attempts to Check_In while already checked in on the same Autonomous_Vehicle, THEN THE Boarding_Manager SHALL reject the duplicate Check_In and notify the Commuter via the TakeMe app.

---

### Requirement 1.2: QR Code Fallback Check-In

**User Story:** As a Commuter whose app GPS is unreliable, I want to scan a QR code inside the vehicle to check in, so that I have an alternative boarding method.

#### Acceptance Criteria

1. THE Autonomous_Vehicle SHALL display a vehicle-specific QR_Code inside the passenger cabin.
2. WHEN a Commuter scans the QR_Code with the TakeMe app, THE Boarding_Manager SHALL register the Commuter's Check_In identically to an app-based Check_In.
3. WHEN a QR_Code Check_In is registered, THE Boarding_Manager SHALL create a Commuter_Trip record with the same fields as an app-based Check_In.

---

### Requirement 1.3: Boarding Window Management

**User Story:** As an Autonomous_Vehicle, I want to enforce a time-limited boarding window after stopping, so that the vehicle does not wait indefinitely for passengers to check in.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle stops at a pickup point, THE Boarding_Manager SHALL open a Boarding_Window of 90 seconds.
2. WHILE the Boarding_Window is open, THE Boarding_Manager SHALL accept Check_In requests from Commuters.
3. WHEN the Boarding_Window expires, THE Boarding_Manager SHALL close the Boarding_Window and trigger occupancy reconciliation.
4. WHEN the Boarding_Window closes, THE Boarding_Manager SHALL create a StopEvent record containing the stop GPS coordinates, timestamp, and the list of Check_Ins registered during the window.

---

### Requirement 2.1: Seat Pressure Sensor Occupancy Counting

**User Story:** As an Autonomous_Vehicle, I want to count how many seats are physically occupied, so that I have a ground-truth occupancy signal independent of app check-ins.

#### Acceptance Criteria

1. WHILE the Autonomous_Vehicle is in service, THE Boarding_Manager SHALL read the Seat_Pressure_Sensor state for each seat at intervals of no more than 1 second.
2. WHEN a Seat_Pressure_Sensor transitions from unoccupied to occupied, THE Boarding_Manager SHALL increment the Vehicle_Occupancy_State seat count.
3. WHEN a Seat_Pressure_Sensor transitions from occupied to unoccupied, THE Boarding_Manager SHALL decrement the Vehicle_Occupancy_State seat count.
4. THE Boarding_Manager SHALL maintain the Vehicle_Occupancy_State seat count as a non-negative integer at all times.

---

### Requirement 2.2: Camera-Based Anonymous Headcount

**User Story:** As an Autonomous_Vehicle, I want an anonymous headcount from the cabin camera, so that I have a second occupancy signal that does not rely on seat sensors.

#### Acceptance Criteria

1. WHEN the Boarding_Window closes, THE Occupancy_Reconciler SHALL request a Camera_Headcount from the RB5 cabin camera.
2. THE Camera_Headcount SHALL produce an integer person count using object detection on the cabin camera feed.
3. THE Camera_Headcount SHALL NOT extract, store, or transmit any biometric data, facial features, or personally identifiable information from the camera feed.
4. WHEN the Camera_Headcount is produced, THE Occupancy_Reconciler SHALL record the count in the Vehicle_Occupancy_State.

---

### Requirement 2.3: Occupancy Reconciliation

**User Story:** As an Autonomous_Vehicle, I want to compare physical occupancy against app check-ins after each boarding window, so that I can detect when someone boarded without checking in.

#### Acceptance Criteria

1. WHEN the Boarding_Window closes, THE Occupancy_Reconciler SHALL compare the Seat_Pressure_Sensor count against the number of registered Check_Ins.
2. WHEN the Seat_Pressure_Sensor count equals the Check_In count, THE Occupancy_Reconciler SHALL mark the StopEvent as RECONCILED and permit departure.
3. WHEN the Seat_Pressure_Sensor count exceeds the Check_In count, THE Occupancy_Reconciler SHALL mark the StopEvent as DISCREPANCY_DETECTED and trigger the primary enforcement policy.
4. WHEN the Camera_Headcount is available, THE Occupancy_Reconciler SHALL use the Camera_Headcount as a secondary confirmation of the Seat_Pressure_Sensor count.

---

### Requirement 3.1: Primary Enforcement — Refuse to Move

**User Story:** As an Autonomous_Vehicle, I want to refuse to depart when more people are seated than checked in, so that fare evasion is prevented at the point of boarding.

#### Acceptance Criteria

1. WHEN the Occupancy_Reconciler detects a DISCREPANCY_DETECTED state, THE Boarding_Manager SHALL prevent the Autonomous_Vehicle from departing.
2. WHEN the Autonomous_Vehicle is held for a discrepancy, THE Boarding_Manager SHALL broadcast an in-vehicle audio and visual prompt instructing unchecked-in Commuters to check in via the TakeMe app or QR_Code.
3. WHEN the Autonomous_Vehicle is held for a discrepancy, THE Boarding_Manager SHALL re-open the Boarding_Window for an additional 60 seconds to allow late Check_Ins.
4. WHEN the re-opened Boarding_Window closes and the Seat_Pressure_Sensor count equals the Check_In count, THE Boarding_Manager SHALL permit departure.

---

### Requirement 3.2: Fallback Enforcement — Grace Period and Escalation

**User Story:** As an Autonomous_Vehicle, I want a fallback policy when the primary enforcement does not resolve the discrepancy, so that the vehicle is not blocked indefinitely.

#### Acceptance Criteria

1. WHEN the re-opened Boarding_Window closes and the Seat_Pressure_Sensor count still exceeds the Check_In count, THE Boarding_Manager SHALL depart and enter a Grace_Period of 2 stops.
2. WHILE the Grace_Period is active, THE Boarding_Manager SHALL broadcast a prompt at each subsequent stop instructing unchecked-in Commuters to check in.
3. WHEN a late Check_In resolves the discrepancy during the Grace_Period, THE Boarding_Manager SHALL cancel the Grace_Period and resume normal operation.
4. IF the Grace_Period expires without resolution, THEN THE Boarding_Manager SHALL route the Autonomous_Vehicle to the nearest Safe_Stop.
5. WHEN the Autonomous_Vehicle arrives at the Safe_Stop after Grace_Period expiry, THE Boarding_Manager SHALL hold the vehicle and broadcast a final check-in prompt.

---

### Requirement 3.3: Fare Evasion Alert

**User Story:** As an Operator, I want to be notified when fare evasion is detected and unresolved, so that I can take action.

#### Acceptance Criteria

1. WHEN the Occupancy_Reconciler detects a DISCREPANCY_DETECTED state, THE Boarding_Manager SHALL publish a Fare_Evasion_Alert fleet event to the Dispatch_Service.
2. WHEN a Fare_Evasion_Alert is published, THE Dispatch_Service SHALL surface the alert in the Operator dashboard within 10 seconds.
3. WHEN a Fare_Evasion_Alert is published, THE Boarding_Manager SHALL include the vehicle identifier, stop GPS coordinates, timestamp, Seat_Pressure_Sensor count, Check_In count, and Camera_Headcount in the alert payload.
4. WHEN a discrepancy is resolved by a late Check_In, THE Boarding_Manager SHALL publish a FARE_EVASION_RESOLVED fleet event to the Dispatch_Service.

---

### Requirement 3.4: Door Lock Enforcement

**User Story:** As an Autonomous_Vehicle, I want to control the door lock during enforcement holds, so that the vehicle cannot be exited or entered unsafely during a discrepancy resolution.

#### Acceptance Criteria

1. WHEN the Boarding_Manager prevents departure due to a DISCREPANCY_DETECTED state, THE Door_Lock_Controller SHALL keep the vehicle doors unlocked to allow additional Commuters to board and check in.
2. WHEN the Autonomous_Vehicle departs after a resolved or unresolved discrepancy, THE Door_Lock_Controller SHALL lock the vehicle doors.
3. WHEN the Autonomous_Vehicle arrives at a stop, THE Door_Lock_Controller SHALL unlock the vehicle doors.

---

### Requirement 4.1: App-Based Check-Out on Alighting

**User Story:** As a Commuter, I want to check out via the TakeMe app when I exit the minibus, so that my trip distance and fare are calculated correctly.

#### Acceptance Criteria

1. WHEN a Commuter taps "Alight" in the TakeMe app while the Autonomous_Vehicle is stopped, THE Alighting_Manager SHALL register the Commuter's Check_Out.
2. WHEN a Check_Out is registered, THE Alighting_Manager SHALL record the alighting stop GPS coordinates and timestamp on the Commuter's Commuter_Trip record.
3. WHEN a Check_Out is registered, THE Alighting_Manager SHALL remove the Commuter_Trip from the Vehicle_Occupancy_State active trip list.
4. WHEN a Check_Out is registered, THE Alighting_Manager SHALL trigger fare calculation for the Commuter_Trip.

---

### Requirement 4.2: Alighting Window Management

**User Story:** As an Autonomous_Vehicle, I want to enforce a time-limited alighting window at each stop, so that the vehicle does not wait indefinitely for passengers to exit.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle stops at a drop-off point, THE Alighting_Manager SHALL open an Alighting_Window of 60 seconds.
2. WHILE the Alighting_Window is open, THE Alighting_Manager SHALL accept Check_Out requests from Commuters.
3. WHEN the Alighting_Window expires, THE Alighting_Manager SHALL close the Alighting_Window and trigger occupancy reconciliation for alighting.
4. WHEN the Alighting_Window closes, THE Alighting_Manager SHALL append the list of Check_Outs to the StopEvent record.

---

### Requirement 4.3: Alighting Occupancy Reconciliation

**User Story:** As an Autonomous_Vehicle, I want to verify that the number of people who exited matches the check-outs, so that I can detect commuters who left without checking out.

#### Acceptance Criteria

1. WHEN the Alighting_Window closes, THE Occupancy_Reconciler SHALL compare the change in Seat_Pressure_Sensor count against the number of Check_Outs registered during the Alighting_Window.
2. WHEN the seat count decrease equals the Check_Out count, THE Occupancy_Reconciler SHALL mark the alighting as RECONCILED.
3. WHEN the seat count decrease exceeds the Check_Out count (a Commuter left without checking out), THE Occupancy_Reconciler SHALL identify the missing Check_Out by comparing the active Commuter_Trip list against the remaining occupied seats.
4. IF a Commuter exits without checking out, THEN THE Alighting_Manager SHALL auto-complete the Commuter_Trip using the current stop as the alighting point and trigger fare calculation.

---

### Requirement 5.1: Per-Commuter Fare Calculation

**User Story:** As a Commuter, I want to pay only for the distance I actually travelled, so that my fare is fair and transparent.

#### Acceptance Criteria

1. WHEN a Commuter_Trip is completed (via Check_Out or auto-completion), THE Alighting_Manager SHALL calculate the fare based on the distance in kilometres between the boarding stop and the alighting stop.
2. WHEN the fare is calculated, THE Alighting_Manager SHALL use the rate_per_km value from Firebase Remote Config, consistent with the existing fare calculation logic.
3. WHEN the fare is calculated, THE Alighting_Manager SHALL present the fare to the Commuter in the TakeMe app before processing payment.
4. THE Alighting_Manager SHALL calculate the fare as fare_amount = distance_km * rate_per_km, where distance_km is the route distance between boarding and alighting stops.

---

### Requirement 5.2: Per-Commuter Payment Processing

**User Story:** As a Commuter, I want to pay my individual fare through the app, so that each person pays for their own trip.

#### Acceptance Criteria

1. WHEN the Commuter confirms payment in the TakeMe app, THE Dispatch_Service SHALL process the payment via Peach Payments for the Commuter_Trip fare amount.
2. WHEN payment succeeds, THE Dispatch_Service SHALL mark the Commuter_Trip payment status as PAID and the trip status as COMPLETED.
3. IF payment fails, THEN THE Dispatch_Service SHALL retry payment processing up to 3 times with exponential backoff before flagging the Commuter_Trip for Operator review.
4. WHEN a Commuter_Trip is flagged for Operator review due to payment failure, THE Dispatch_Service SHALL publish a PAYMENT_FAILURE fleet event containing the Commuter_Trip identifier and failure reason.

---

### Requirement 5.3: Auto-Completed Trip Fare Handling

**User Story:** As an Operator, I want commuters who exit without checking out to still be billed, so that revenue is not lost.

#### Acceptance Criteria

1. WHEN a Commuter_Trip is auto-completed due to a missing Check_Out, THE Alighting_Manager SHALL calculate the fare using the current stop as the alighting point.
2. WHEN a Commuter_Trip is auto-completed, THE Alighting_Manager SHALL notify the Commuter via the TakeMe app that the trip was auto-completed and present the fare.
3. WHEN a Commuter_Trip is auto-completed, THE Alighting_Manager SHALL mark the Commuter_Trip with an AUTO_COMPLETED flag for Operator audit.

---

### Requirement 6.1: Vehicle Occupancy State Publishing

**User Story:** As an Operator, I want to see the real-time occupancy state of each vehicle, so that I can monitor capacity and detect anomalies.

#### Acceptance Criteria

1. WHILE the Autonomous_Vehicle is in service, THE Boarding_Manager SHALL publish the Vehicle_Occupancy_State to the Dispatch_Service at intervals of no more than 5 seconds.
2. WHEN the Vehicle_Occupancy_State is published, THE Boarding_Manager SHALL include the seat sensor count, camera headcount, checked-in commuter count, and the number of active Commuter_Trips.
3. THE Dispatch_Service SHALL surface the Vehicle_Occupancy_State in the Operator dashboard alongside the existing fleet status data.

---

### Requirement 6.2: StopEvent Logging

**User Story:** As an Operator, I want a record of every boarding and alighting event at each stop, so that I can audit commuter flow and investigate disputes.

#### Acceptance Criteria

1. WHEN a StopEvent is created, THE Boarding_Manager SHALL write the StopEvent to the Dispatch_Service with the stop GPS coordinates, timestamp, list of Check_Ins, list of Check_Outs, reconciliation status, and any enforcement actions taken.
2. THE Dispatch_Service SHALL retain StopEvent records for a minimum of 30 days, consistent with the existing fleet event retention policy.

---

### Requirement 6.3: Commuter Trip Record Persistence

**User Story:** As an Operator, I want completed commuter trip records stored with full journey details, so that I can audit per-commuter billing.

#### Acceptance Criteria

1. THE Dispatch_Service SHALL store completed Commuter_Trip records including commuter UID, boarding stop coordinates, alighting stop coordinates, distance in kilometres, fare amount, payment status, and auto-completion flag.
2. THE Dispatch_Service SHALL make Commuter_Trip records accessible to the Commuter via the TakeMe app for trip history viewing.
3. THE Dispatch_Service SHALL make Commuter_Trip records accessible to the Operator via the dashboard for audit purposes.

---

### Requirement 7.1: POPIA Compliance for Occupancy Monitoring

**User Story:** As a Commuter in South Africa, I want the occupancy monitoring system to respect my privacy rights under POPIA, so that my personal data is protected.

#### Acceptance Criteria

1. THE Camera_Headcount SHALL NOT use facial recognition, biometric identification, or any technique that extracts special personal information as defined by POPIA.
2. THE Camera_Headcount SHALL process frames on-device and transmit only the integer person count to the Occupancy_Reconciler.
3. THE Camera_Headcount SHALL NOT store or transmit raw camera frames from the cabin camera.
4. THE Boarding_Manager SHALL associate Commuter identity only through voluntary app-based Check_In, not through any passive identification mechanism.

---

### Requirement 7.2: Seat Pressure Sensor Failure Handling

**User Story:** As an Operator, I want to be notified when seat sensors malfunction, so that I can schedule maintenance and avoid false fare evasion alerts.

#### Acceptance Criteria

1. IF a Seat_Pressure_Sensor reports an invalid or stuck reading for more than 60 seconds, THEN THE Boarding_Manager SHALL flag the sensor as faulty and exclude the seat from the occupancy count.
2. WHEN a Seat_Pressure_Sensor is flagged as faulty, THE Boarding_Manager SHALL publish a SENSOR_FAILURE fleet event to the Dispatch_Service with the vehicle identifier and seat identifier.
3. WHILE one or more Seat_Pressure_Sensors are faulty, THE Occupancy_Reconciler SHALL rely on the Camera_Headcount as the primary occupancy signal for reconciliation.

---

### Requirement 7.3: Concurrent Boarding and Alighting at the Same Stop

**User Story:** As an Autonomous_Vehicle, I want to handle commuters boarding and alighting at the same stop, so that the system correctly tracks occupancy changes in both directions.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle stops at a point where both boarding and alighting occur, THE Boarding_Manager SHALL process Check_Outs before Check_Ins to establish the correct baseline occupancy.
2. WHEN both Check_Outs and Check_Ins occur at the same stop, THE Occupancy_Reconciler SHALL reconcile the net occupancy change (new seat count = previous count - check_outs + check_ins) against the Seat_Pressure_Sensor count.
3. WHEN both boarding and alighting occur at the same stop, THE Boarding_Manager SHALL create a single StopEvent containing both the Check_Out list and the Check_In list.

---

### Requirement 8.1: Vehicle Capacity Enforcement

**User Story:** As an Autonomous_Vehicle, I want to enforce a maximum passenger capacity, so that the vehicle does not become overloaded.

#### Acceptance Criteria

1. THE Boarding_Manager SHALL enforce a configurable maximum passenger capacity for the Autonomous_Vehicle.
2. WHEN the Vehicle_Occupancy_State checked-in count equals the maximum capacity, THE Boarding_Manager SHALL reject additional Check_In requests and notify the Commuter via the TakeMe app that the vehicle is full.
3. WHEN the vehicle is at maximum capacity, THE Boarding_Manager SHALL close the Boarding_Window early and permit departure.
