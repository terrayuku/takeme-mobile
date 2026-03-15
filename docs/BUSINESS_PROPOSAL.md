# TakeMe — Driverless Taxi Hand Signal Recognition
## Business Proposal

---

## Executive Summary

TakeMe is an autonomous minibus taxi platform that uses AI-powered hand signal recognition to detect and respond to roadside passengers — no app required to hail a ride. A passenger simply raises their hand, and the vehicle stops. The system is built for South Africa's minibus taxi industry first, with a clear path to global expansion.

The platform combines an onboard Qualcomm RB5 compute unit (running real-time ML inference), the existing TakeMe Android app (passenger boarding, payment), and a Firebase backend that ties everything together. No raw camera footage ever leaves the vehicle, making the system POPIA-compliant by design.

---

## The Problem

South Africa's minibus taxi industry moves approximately 15 million passengers daily, yet it remains largely informal, cash-dependent, and dangerous. Drivers are fatigued, routes are unpredictable, and passengers have no reliable way to know when or where a taxi will stop. Globally, the last-mile transport problem is unsolved for the majority of the world's population who cannot afford ride-hailing premiums.

Key pain points:
- No standardised hailing mechanism — passengers rely on informal gestures with no guarantee of being seen
- Driver fatigue and human error are leading causes of minibus taxi accidents
- Cash-only transactions create revenue leakage and safety risks for drivers
- Fleet operators have zero real-time visibility into vehicle positions or incidents

---

## The Solution

TakeMe deploys a Qualcomm RB5 compute unit in each minibus. A forward-facing camera feeds frames into an on-device ML model (running on the Hexagon DSP at up to 15 TOPS) that detects the traditional hitchhiking/hailing gesture in real time. When a passenger is detected with sufficient confidence (≥ 0.75), the system evaluates the road ahead for a safe stop zone and coordinates the pickup — all without a human driver making that decision.

The passenger receives a boarding prompt on their TakeMe app, confirms boarding, selects a destination, and pays digitally via Peach Payments. The fleet operator monitors everything live on a React-based dashboard.

**What makes this different:**
- No app required to hail — the gesture is the interface
- Inference runs entirely on-device; no images leave the vehicle (privacy by architecture)
- Built on existing infrastructure (Firebase, Google Maps, TakeMe Android app) — no greenfield backend
- OTA model updates mean the fleet improves continuously without a Play Store release

---

## Market Opportunity

| Market | Size |
|---|---|
| South Africa minibus taxi industry | ~R90 billion annually |
| Sub-Saharan Africa shared mobility | Projected $12B by 2030 |
| Global autonomous last-mile transport | $500B+ long-term addressable market |

South Africa is the ideal launch market: high taxi density, established informal hailing culture, and a regulatory environment that is actively exploring autonomous vehicle frameworks. The gesture-based hailing model is culturally native — passengers already use this exact signal.

---

## Technology Overview

The system is split across two codebases sharing a single Firebase backend.

### `takeme-rb5` — The Vehicle Brain
Runs on Ubuntu 20.04 on the Qualcomm RB5 (QRB5165 SoC) mounted in each minibus.

- Camera feed ingestion via OpenCV + V4L2 at ≥10fps, ≥480p, ≥90° horizontal FOV
- ML inference via Qualcomm SNPE on the Hexagon DSP — 200ms latency per frame
- Safe stop zone evaluation using Google Roads API (decision within 500ms)
- Fleet telemetry published to Firebase every ≤5 seconds
- OTA model updates via Firebase Storage — no vehicle downtime required

### `takeme-mobile` — The Passenger Interface
The existing Android app, extended for autonomous trip flows.

- Boarding confirmation prompt delivered within 5 seconds of vehicle stopping
- Destination selection via Google Places autocomplete
- Fare calculation (distance × rate per km) presented before payment
- Payment via Peach Payments (card, EFT, SnapScan, Zapper) — ZAR native
- Payment retry logic with up to 3 attempts before operator escalation

### Firebase Backend
- Realtime Database for fleet telemetry, pickup requests, trips, and event logs
- Firebase Storage for ML model binaries (.dlc format)
- Remote Config for model version manifest and fare rate configuration
- Cloud Functions as a secure proxy for Peach Payments API calls
- Fleet event logs retained for 30 days minimum

### Operator Dashboard
A React 18 web application with live fleet map, alert panel, and trip review — all driven by Firebase RTDB listeners with no polling.

---

## Privacy and Compliance

Privacy is not a feature — it is a constraint baked into the architecture.

- All camera frame processing happens on-device on the RB5
- Only the pickup event metadata (GPS coordinates, timestamp, confidence score) is transmitted — never raw images
- No passenger biometric data is stored anywhere in the system
- Detection logs are fully anonymised (time-of-day bucket, lighting estimate — no PII)
- POPIA compliant for South Africa; jurisdiction-aware data handling for international expansion
- Firebase Security Rules enforce that passengers can only read their own trip data; operators have scoped read access to detection logs

---

## Safety Architecture

The system is designed to fail safely at every layer.

| Failure Scenario | System Response |
|---|---|
| Camera feed lost | Detector enters UNAVAILABLE state; operator alerted within 2s; vehicle flagged for inspection |
| No safe stop zone within 200m | Pickup request cancelled; operator notified with GPS coordinates and reason |
| Obstacle detected during deceleration | Stop aborted immediately; event logged with coordinates and timestamp |
| Detector unavailable | Vehicle completes active trip, accepts no new pickups |
| Firebase connection lost | Active trip continues on last known route; reconnect attempted every 30s |
| Bad ML model pushed | CI accuracy gate rejects any model below 85% accuracy before it reaches vehicles |

The vehicle maintains a minimum 3-second following distance at all times, including during pickup deceleration manoeuvres.

---

## ML Model Quality and Continuous Improvement

- Every detection event is logged with confidence score, outcome, and anonymised environmental context
- False positive rate is monitored on a rolling 7-day window; operator alerted if it exceeds 10%
- A CI accuracy gate (≥85% on the TakeMe validation dataset) must pass before any model update is published to the fleet
- OTA updates deploy new models to all vehicles without a Play Store release or vehicle downtime
- Property-based testing (Hypothesis on the RB5 Python service, jqwik on Android) validates 23 correctness properties across thousands of generated inputs

---

## Revenue Model

| Stream | Description |
|---|---|
| Per-trip commission | Percentage of each fare processed through Peach Payments |
| Fleet licensing | Monthly SaaS fee per vehicle for the RB5 software stack and operator dashboard |
| Data insights | Anonymised route demand and hailing hotspot analytics sold to municipalities and transport planners |
| White-label | Platform licensed to international operators entering new markets |

The digital payment rail eliminates cash handling entirely, reducing driver risk and creating a complete, auditable revenue record for operators.

---

## Go-to-Market Strategy

**Phase 1 — Pilot (Months 1–6)**
Deploy 10–20 RB5-equipped minibuses on a single high-density corridor in a South African metro (Cape Town or Johannesburg). Partner with an established taxi association. Measure detection accuracy, false positive rate, boarding conversion, and payment completion in real operating conditions.

**Phase 2 — City Scale (Months 7–18)**
Expand to 200+ vehicles across multiple routes. Onboard additional taxi associations. Launch the operator dashboard as a commercial product. Integrate with municipal transport data feeds.

**Phase 3 — Regional Expansion (Months 19–36)**
Enter additional Sub-Saharan African markets (Kenya, Nigeria, Ghana). Adapt fare and payment stack for local currencies and payment methods. Apply jurisdiction-specific data protection compliance.

**Phase 4 — Global Platform**
License the platform to autonomous vehicle operators in Asia, Latin America, and Europe. The gesture-based hailing model is culturally transferable — the open-hand wave is a near-universal hailing signal.

---

## Competitive Advantage

| Factor | TakeMe | Ride-hailing apps | Traditional taxis |
|---|---|---|---|
| App required to hail | No | Yes | No |
| Autonomous operation | Yes | No | No |
| Real-time fleet visibility | Yes | Partial | No |
| Digital payments | Yes | Yes | Rarely |
| Privacy-preserving by design | Yes | No | N/A |
| Operates in low-connectivity areas | Yes (offline trip completion) | No | Yes |

No competitor combines gesture-based hailing, on-device privacy-preserving inference, and a digital payment rail in a single platform targeting the informal shared mobility market.

---

## Investment Ask

Funding will be deployed across three areas:

1. **Hardware procurement and fleet deployment** — Qualcomm RB5 units, camera hardware, vehicle installation for the Phase 1 pilot fleet
2. **ML model development and validation** — Training data collection, model iteration, validation dataset curation, and CI pipeline build-out
3. **Engineering and operations** — RB5 Python service hardening, Android app extensions, operator dashboard, and 24/7 fleet monitoring during pilot

A detailed financial model and unit economics breakdown are available on request.

---

## Team and Execution

The TakeMe platform is built on a production Android app with existing Firebase Auth, Google Maps, and location service integrations. The architecture decisions — SNPE for on-device inference, Firebase RTDB for real-time telemetry, Peach Payments for ZAR-native transactions — are deliberate choices made by engineers who understand the South African operating environment.

The system is designed to be operated by a small team: the operator dashboard surfaces everything an operator needs to monitor a fleet, and the automated alerting (camera failures, false positive spikes, payment failures) means issues are caught before they become incidents.

---

## Summary

TakeMe turns the most universal human gesture — a raised hand — into a complete autonomous taxi experience. The technology is production-ready in architecture, privacy-compliant by design, and built for the market where it will launch first. The opportunity is large, the moat is real, and the timing is right.

*For further information, technical documentation, or a live demonstration, please get in touch.*
