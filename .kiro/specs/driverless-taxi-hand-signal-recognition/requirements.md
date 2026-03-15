# Requirements Document

## Introduction

This feature evolves the TakeMe mobile app into the foundation for a driverless public taxi system, initially targeting South Africa's minibus taxi industry and designed for global expansion. The core capability is real-time hand signal recognition: an autonomous vehicle's onboard camera continuously scans the roadside for passengers using the traditional hitchhiking/hailing gesture (extended arm with thumb up or open hand wave) and coordinates a safe pickup. The system integrates with the existing TakeMe location services, Firebase backend, and camera infrastructure.

## Glossary

- **Autonomous_Vehicle**: A driverless taxi unit equipped with cameras, GPS, and onboard compute running the TakeMe system.
- **Hand_Signal_Detector**: The ML inference component that processes camera frames and classifies hand signals.
- **Passenger**: A person standing at the roadside who uses a hand signal to request a taxi pickup.
- **Pickup_Request**: A confirmed detection event that triggers the vehicle to evaluate and execute a stop.
- **Route_Manager**: The component responsible for the vehicle's current route, stops, and detour decisions.
- **Dispatch_Service**: The Firebase-backed backend service that coordinates fleet routing, passenger matching, and trip records.
- **Camera_Feed**: The continuous video stream from the Autonomous_Vehicle's forward-facing or side-facing camera.
- **Confidence_Score**: A numeric value between 0.0 and 1.0 representing the Hand_Signal_Detector's certainty that a detected gesture is a valid hail signal.
- **Safe_Stop_Zone**: A road segment classified as safe for the vehicle to decelerate and stop based on road geometry, speed, and traffic data.
- **Trip**: A record of a passenger journey from pickup location to destination, stored in the Dispatch_Service.
- **Operator**: A human fleet supervisor who monitors the autonomous fleet via a management dashboard.

---

## Requirements

---

### Requirement 1.1: Camera Feed Frame Rate

**User Story:** As an Autonomous_Vehicle, I want to process camera frames at a consistent rate, so that I do not miss passengers hailing in real time.

#### Acceptance Criteria

1. WHILE the Autonomous_Vehicle is in service, THE Hand_Signal_Detector SHALL process Camera_Feed frames at a minimum rate of 10 frames per second.
2. WHILE the Autonomous_Vehicle is in service, THE Hand_Signal_Detector SHALL log a warning event when the measured frame rate drops below 10 frames per second for more than 1 second.

---

### Requirement 1.2: Camera Feed Unavailability Handling

**User Story:** As an Operator, I want to be notified immediately when the camera feed is lost, so that I can dispatch a replacement vehicle or intervene.

#### Acceptance Criteria

1. WHEN the Camera_Feed becomes unavailable, THE Hand_Signal_Detector SHALL log the failure with a timestamp within 500ms.
2. WHEN the Camera_Feed becomes unavailable, THE Hand_Signal_Detector SHALL notify the Dispatch_Service within 2 seconds of the failure.
3. IF the Camera_Feed is unavailable for more than 10 seconds, THEN THE Dispatch_Service SHALL alert the Operator dashboard with the affected Autonomous_Vehicle identifier.

---

### Requirement 1.3: Camera Feed Frame Quality Filtering

**User Story:** As an Autonomous_Vehicle, I want to discard low-quality frames before inference, so that poor inputs do not produce unreliable detections.

#### Acceptance Criteria

1. WHEN a Camera_Feed frame resolution is below 480p, THE Hand_Signal_Detector SHALL discard the frame without running inference.
2. WHEN a Camera_Feed frame is discarded for quality reasons, THE Hand_Signal_Detector SHALL log a quality warning containing the frame timestamp and measured resolution.
3. THE Hand_Signal_Detector SHALL operate on frames captured from a minimum field of view of 90 degrees horizontal.

---

### Requirement 2.1: Gesture Inference Latency

**User Story:** As an Autonomous_Vehicle, I want gesture inference to complete quickly, so that the vehicle has enough time to react to a hailing passenger.

#### Acceptance Criteria

1. WHEN a Camera_Feed frame contains a person performing the hailing gesture, THE Hand_Signal_Detector SHALL produce a Confidence_Score for that detection within 200ms of frame capture.

---

### Requirement 2.2: Pickup Request Emission on High Confidence

**User Story:** As an Autonomous_Vehicle, I want to emit a pickup request only when the detector is sufficiently confident, so that the vehicle does not stop for false positives.

#### Acceptance Criteria

1. WHEN the Confidence_Score for a detected gesture is 0.75 or above, THE Hand_Signal_Detector SHALL emit a Pickup_Request event.
2. WHEN a Pickup_Request event is emitted, THE Hand_Signal_Detector SHALL include the GPS coordinates, timestamp, and Confidence_Score in the event payload.
3. WHEN the Confidence_Score for a detected gesture is below 0.75, THE Hand_Signal_Detector SHALL discard the detection without emitting a Pickup_Request.

---

### Requirement 2.3: False Positive Rate Under Daylight Conditions

**User Story:** As an Autonomous_Vehicle, I want the detector to reliably distinguish hailing gestures from incidental arm movements, so that the vehicle does not make unnecessary stops.

#### Acceptance Criteria

1. THE Hand_Signal_Detector SHALL distinguish between a hailing gesture and incidental arm movements with a false-positive rate below 5% under standard daylight conditions.

---

### Requirement 2.4: Low-Light Detection

**User Story:** As a Passenger, I want to hail a taxi at night or in low-light conditions, so that the service is available around the clock.

#### Acceptance Criteria

1. WHERE night-mode camera hardware is available, THE Hand_Signal_Detector SHALL maintain detection capability in ambient light conditions down to 10 lux.
2. WHERE night-mode camera hardware is available, THE Hand_Signal_Detector SHALL maintain a false-positive rate below 5% in ambient light conditions down to 10 lux.

---

### Requirement 2.5: Detection Range

**User Story:** As a Passenger, I want the vehicle to detect my signal from a reasonable distance, so that the vehicle has time to slow down and stop safely.

#### Acceptance Criteria

1. THE Hand_Signal_Detector SHALL support detection of the hailing gesture from a lateral distance of up to 30 metres from the vehicle.

---

### Requirement 3.1: Safe Stop Zone Evaluation

**User Story:** As an Autonomous_Vehicle, I want to evaluate whether the current road segment is safe to stop on, so that I do not endanger passengers or other road users.

#### Acceptance Criteria

1. WHEN a Pickup_Request is received, THE Route_Manager SHALL evaluate whether the current road segment is a Safe_Stop_Zone within 500ms.
2. WHEN the current road segment is a Safe_Stop_Zone and the vehicle speed is below 60 km/h, THE Route_Manager SHALL initiate a deceleration sequence to stop for the Passenger.

---

### Requirement 3.2: Nearest Safe Stop Zone Fallback

**User Story:** As a Passenger, I want the vehicle to find the nearest safe stop when the current location is unsafe, so that I am still picked up even if the vehicle cannot stop immediately.

#### Acceptance Criteria

1. IF the current road segment is not a Safe_Stop_Zone, THEN THE Route_Manager SHALL continue to the next Safe_Stop_Zone within 200 metres ahead and stop there.
2. IF no Safe_Stop_Zone exists within 200 metres ahead, THEN THE Route_Manager SHALL cancel the Pickup_Request.
3. WHEN a Pickup_Request is cancelled due to no Safe_Stop_Zone, THE Route_Manager SHALL notify the Dispatch_Service with the cancellation reason and the GPS coordinates of the cancellation point.

---

### Requirement 3.3: Pickup Visual Notification

**User Story:** As a Passenger, I want the vehicle to signal that it is stopping for me, so that I know it is safe to approach.

#### Acceptance Criteria

1. WHILE the Autonomous_Vehicle is decelerating for a pickup, THE Autonomous_Vehicle SHALL activate hazard lights.
2. WHILE the Autonomous_Vehicle is decelerating for a pickup, THE Autonomous_Vehicle SHALL display a passenger notification on the exterior display.

---

### Requirement 4.1: Boarding Confirmation Prompt

**User Story:** As a Passenger, I want to receive a boarding prompt on my phone when the taxi stops for me, so that I can confirm I am getting in.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle stops for a Pickup_Request, THE Dispatch_Service SHALL send a boarding confirmation prompt to the Passenger's registered mobile device within 5 seconds.

---

### Requirement 4.2: Trip Record Creation on Boarding

**User Story:** As a Passenger, I want my trip to be recorded when I confirm boarding, so that my journey is tracked and I am billed correctly.

#### Acceptance Criteria

1. WHEN the Passenger confirms boarding via the TakeMe app, THE Dispatch_Service SHALL create a Trip record.
2. WHEN a Trip record is created, THE Dispatch_Service SHALL include the pickup GPS coordinates, timestamp, passenger identifier, and Autonomous_Vehicle identifier in the Trip record.

---

### Requirement 4.3: Boarding Timeout Handling

**User Story:** As an Autonomous_Vehicle, I want to resume my route if a passenger does not board within a reasonable time, so that the vehicle is not blocked indefinitely.

#### Acceptance Criteria

1. IF the Passenger does not confirm boarding within 60 seconds of the vehicle stopping, THEN THE Dispatch_Service SHALL mark the Pickup_Request as unconfirmed.
2. WHEN a Pickup_Request is marked as unconfirmed due to timeout, THE Autonomous_Vehicle SHALL resume its route.

---

### Requirement 5.1: Destination Selection After Boarding

**User Story:** As a Passenger, I want to select my destination after boarding, so that the vehicle knows where to take me.

#### Acceptance Criteria

1. WHEN a Trip is created, THE Route_Manager SHALL prompt the Passenger to select a destination via the TakeMe app.
2. WHEN the Passenger selects a destination, THE Route_Manager SHALL calculate the optimal route from the current pickup location to the destination using the existing location services.

---

### Requirement 5.2: Destination Arrival Notification

**User Story:** As a Passenger, I want to be notified when I am approaching my destination, so that I can prepare to exit the vehicle.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle arrives within 50 metres of the Passenger's destination, THE Dispatch_Service SHALL notify the Passenger via the TakeMe app.

---

### Requirement 5.3: Out-of-Zone Destination Handling

**User Story:** As a Passenger, I want to be informed if my destination is outside the service area, so that I can plan an alternative.

#### Acceptance Criteria

1. IF the Passenger's destination is outside the Autonomous_Vehicle's current operating zone, THEN THE Dispatch_Service SHALL inform the Passenger that the destination is outside the operating zone.
2. WHEN the Passenger's destination is outside the operating zone, THE Dispatch_Service SHALL suggest the nearest available drop-off point within the zone.

---

### Requirement 6.1: Fare Calculation

**User Story:** As a Passenger, I want my fare to be calculated based on the distance I travelled, so that I pay a fair and transparent amount.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle reaches the Passenger's destination stop, THE Dispatch_Service SHALL calculate the fare based on the distance travelled in kilometres.
2. WHEN the fare is calculated, THE Dispatch_Service SHALL present the fare to the Passenger in the TakeMe app before processing payment.

---

### Requirement 6.2: Payment Processing

**User Story:** As a Passenger, I want to confirm and pay my fare in the app, so that the transaction is completed without cash.

#### Acceptance Criteria

1. WHEN the Passenger confirms payment, THE Dispatch_Service SHALL record the payment against the Trip.
2. WHEN the Passenger confirms payment, THE Dispatch_Service SHALL mark the Trip as completed.

---

### Requirement 6.3: Payment Failure Handling

**User Story:** As an Operator, I want failed payments to be retried and escalated, so that revenue is not lost due to transient errors.

#### Acceptance Criteria

1. IF payment fails, THEN THE Dispatch_Service SHALL retry payment processing up to 3 times before flagging the Trip for Operator review.
2. WHEN a Trip is flagged for Operator review due to payment failure, THE Dispatch_Service SHALL surface the Trip in the Operator dashboard with the failure reason.

---

### Requirement 6.4: Trip Record Persistence

**User Story:** As an Operator, I want completed trip records to be stored with full journey details, so that I can audit and report on fleet activity.

#### Acceptance Criteria

1. THE Dispatch_Service SHALL store completed Trip records including pickup coordinates, drop-off coordinates, distance in kilometres, fare amount, and payment status.

---

### Requirement 7.1: Real-Time Vehicle Position Publishing

**User Story:** As an Operator, I want to see the live position and status of every active vehicle, so that I can monitor fleet operations at a glance.

#### Acceptance Criteria

1. WHILE an Autonomous_Vehicle is in service, THE Dispatch_Service SHALL publish the vehicle's GPS position, speed, and current Trip status to the Operator dashboard at intervals of no more than 5 seconds.

---

### Requirement 7.2: Cancelled Pickup Request Visibility

**User Story:** As an Operator, I want to see when pickup requests are cancelled, so that I can identify problem areas on the route network.

#### Acceptance Criteria

1. WHEN a Pickup_Request is cancelled due to no Safe_Stop_Zone, THE Dispatch_Service SHALL surface the cancellation event in the Operator dashboard within 10 seconds.
2. WHEN a cancellation event is surfaced, THE Dispatch_Service SHALL include the GPS coordinates, timestamp, and cancellation reason in the dashboard entry.

---

### Requirement 7.3: Camera Failure Operator Alert

**User Story:** As an Operator, I want to be alerted when a vehicle's camera fails, so that I can take the vehicle out of service before it causes a safety issue.

#### Acceptance Criteria

1. WHEN the Hand_Signal_Detector reports a Camera_Feed failure, THE Dispatch_Service SHALL alert the Operator dashboard with the affected Autonomous_Vehicle identifier.
2. WHEN a Camera_Feed failure alert is raised, THE Dispatch_Service SHALL flag the Autonomous_Vehicle as requiring inspection.

---

### Requirement 7.4: Fleet Event Log Retention

**User Story:** As an Operator, I want fleet event logs to be retained for at least 30 days, so that I can investigate incidents after the fact.

#### Acceptance Criteria

1. THE Dispatch_Service SHALL retain fleet event logs for a minimum of 30 days.

---

### Requirement 8.1: On-Device Frame Processing

**User Story:** As a Passenger, I want my image data to stay on the vehicle and never be transmitted, so that my likeness is not stored or shared.

#### Acceptance Criteria

1. THE Hand_Signal_Detector SHALL process Camera_Feed frames on-device.
2. THE Hand_Signal_Detector SHALL transmit only the Pickup_Request event data (GPS coordinates, timestamp, Confidence_Score) to the Dispatch_Service.
3. THE Hand_Signal_Detector SHALL NOT transmit raw image frames to the Dispatch_Service or any external service.

---

### Requirement 8.2: No Image Frame Storage

**User Story:** As a Passenger, I want the backend to store only trip metadata and not my image, so that my privacy is protected at rest.

#### Acceptance Criteria

1. WHEN a Pickup_Request event is processed, THE Dispatch_Service SHALL store only the event metadata.
2. THE Dispatch_Service SHALL NOT store the source image frame associated with any Pickup_Request.

---

### Requirement 8.3: POPIA Compliance for South Africa

**User Story:** As a Passenger in South Africa, I want my personal data to be handled in accordance with POPIA, so that my legal rights are protected.

#### Acceptance Criteria

1. THE Dispatch_Service SHALL comply with the Protection of Personal Information Act (POPIA) for all Passenger data stored within South Africa.

---

### Requirement 8.4: Jurisdiction-Specific Data Protection

**User Story:** As a Passenger outside South Africa, I want my personal data to be handled according to local regulations, so that my legal rights are protected regardless of where I use the service.

#### Acceptance Criteria

1. WHERE the system operates outside South Africa, THE Dispatch_Service SHALL apply the data protection regulations applicable to the operating jurisdiction.

---

### Requirement 9.1: Detector Unavailability Fallback

**User Story:** As an Operator, I want the vehicle to continue operating safely when the detector fails, so that passengers already on board are not stranded.

#### Acceptance Criteria

1. IF the Hand_Signal_Detector becomes unavailable, THEN THE Autonomous_Vehicle SHALL continue its route without accepting new Pickup_Requests.
2. WHEN the Hand_Signal_Detector becomes unavailable, THE Autonomous_Vehicle SHALL notify the Dispatch_Service with the failure details.

---

### Requirement 9.2: Dispatch Service Disconnection Handling

**User Story:** As a Passenger, I want my trip to complete even if the backend connection is lost, so that I am not stranded mid-journey.

#### Acceptance Criteria

1. IF the Dispatch_Service connection is lost, THEN THE Autonomous_Vehicle SHALL complete any active Trip using the last known route.
2. WHILE the Dispatch_Service connection is lost, THE Autonomous_Vehicle SHALL attempt to reconnect every 30 seconds.

---

### Requirement 9.3: Obstacle Detection During Pickup Manoeuvre

**User Story:** As an Autonomous_Vehicle, I want to abort a pickup stop if an obstacle is detected in my path, so that I do not cause a collision.

#### Acceptance Criteria

1. WHEN the Autonomous_Vehicle detects an obstacle in the deceleration path during a pickup manoeuvre, THE Route_Manager SHALL abort the stop.
2. WHEN a pickup stop is aborted due to an obstacle, THE Route_Manager SHALL notify the Dispatch_Service with the GPS coordinates and timestamp of the abort event.

---

### Requirement 9.4: Safe Following Distance

**User Story:** As an Autonomous_Vehicle, I want to maintain a safe following distance at all times, so that I can stop safely even during a pickup deceleration.

#### Acceptance Criteria

1. THE Autonomous_Vehicle SHALL maintain a minimum safe following distance of 3 seconds from the vehicle ahead at all times, including during pickup deceleration.

---

### Requirement 10.1: Detection Event Logging for Model Improvement

**User Story:** As an Operator, I want every detection event to be logged with outcome and context, so that I have the data needed to retrain and improve the model.

#### Acceptance Criteria

1. THE Hand_Signal_Detector SHALL log each detection event with its Confidence_Score.
2. THE Hand_Signal_Detector SHALL log the outcome of each detection event (confirmed pickup, unconfirmed, or false positive).
3. THE Hand_Signal_Detector SHALL log anonymised environmental metadata for each detection event, including time of day and lighting estimate.

---

### Requirement 10.2: False Positive Rate Monitoring and Alert

**User Story:** As an Operator, I want to be alerted when the false positive rate spikes, so that I can trigger a model retraining review before service quality degrades.

#### Acceptance Criteria

1. WHEN the false-positive rate over a rolling 7-day window exceeds 10%, THE Dispatch_Service SHALL alert the Operator.
2. WHEN the false-positive rate alert is raised, THE Dispatch_Service SHALL flag the Hand_Signal_Detector model for retraining review.

---

### Requirement 10.3: Over-the-Air Model Updates

**User Story:** As an Operator, I want to push model updates to vehicles without a full app reinstall, so that improvements are deployed quickly across the fleet.

#### Acceptance Criteria

1. THE Hand_Signal_Detector SHALL support over-the-air model updates without requiring a full application reinstall.
2. WHEN an over-the-air model update is applied, THE Hand_Signal_Detector SHALL log the previous model version and the new model version with a timestamp.

---

### Requirement 10.4: Pre-Deployment Model Accuracy Gate

**User Story:** As an Operator, I want every new model version to meet a minimum accuracy threshold before it is deployed, so that a bad model cannot be pushed to production vehicles.

#### Acceptance Criteria

1. FOR ALL model versions deployed, THE Hand_Signal_Detector SHALL maintain a detection accuracy of 85% or above on the TakeMe hand signal validation dataset before deployment.
2. IF a candidate model version does not meet the 85% accuracy threshold on the validation dataset, THEN THE Dispatch_Service SHALL reject the deployment and notify the Operator with the measured accuracy score.
