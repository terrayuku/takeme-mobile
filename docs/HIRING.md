# TakeMe — Hiring

We're building the infrastructure for driverless public transport, starting with South Africa's
minibus taxi industry. A passenger raises their hand. The vehicle sees them, stops safely, and
takes them where they need to go. No driver. No cash. No app required to hail.

We're a small team moving fast. Every engineer here owns a meaningful slice of the system.
If you want to work on a problem that matters — and ship code that runs on a vehicle on a real
road — read on.

---

## Open Roles

1. Senior Android Engineer — `takeme-mobile`
2. Python / Embedded Systems Engineer — `takeme-rb5`
3. ML Engineer — Hand Signal Recognition
4. Frontend Engineer — Operator Dashboard (React)

---

---

# Role 1: Senior Android Engineer

## The Work

You'll own the passenger-facing Android app. That means the boarding confirmation flow, destination
selection, fare presentation, and payment — all driven by real-time Firebase events from an
autonomous vehicle that just stopped because it saw someone raise their hand.

The app is Java-based (Android), integrated with Firebase Realtime Database, Google Maps SDK,
Google Places, and Peach Payments. You'll extend it to handle the full autonomous trip lifecycle:
boarding prompts, destination routing, arrival notifications, and digital payment.

## What You'll Build

- Boarding confirmation UI triggered by Firebase RTDB push from the vehicle
- Destination selection using Google Places autocomplete and Directions API
- Fare calculation and Peach Payments checkout (card, EFT, SnapScan, Zapper)
- Arrival proximity notifications (within 50m of destination)
- Out-of-zone destination handling with nearest valid drop-off suggestion
- Payment retry logic with operator escalation on failure
- Espresso integration tests for the full boarding → destination → payment flow

## Stack

- Java, Android SDK
- Firebase Realtime Database, Firebase Auth, Firebase Storage, Remote Config
- Google Maps SDK, Google Places API, Directions API, Distance Matrix API
- Peach Payments Android SDK
- JUnit 4, jqwik (property-based testing), Mockito, Espresso

## You're a Good Fit If You

- Have shipped production Android apps in Java or Kotlin (Java preferred here)
- Are comfortable with Firebase RTDB listeners and real-time UI state management
- Have integrated a payment SDK before — you know what PCI-DSS means in practice
- Write tests as a matter of course, not as an afterthought
- Can read a Firebase Security Rules file and spot a misconfiguration
- Care about the user experience for someone who has never used a smartphone app before

## Nice to Have

- Experience with property-based testing (jqwik, QuickCheck, Hypothesis — any flavour)
- Familiarity with Google Maps SDK route rendering
- Any experience with autonomous vehicle or IoT consumer-facing apps

## Interview Process

1. 30-minute intro call — we explain the system, you ask hard questions
2. Take-home: extend a small Firebase + Android stub (2–3 hours, we respect your time)
3. Technical review — walk us through your take-home, discuss trade-offs
4. Founder conversation — culture, mission, compensation

---

---

# Role 2: Python / Embedded Systems Engineer

## The Work

You'll own `takeme-rb5` — the Python service that runs on a Qualcomm RB5 compute unit mounted
inside each minibus. This is the vehicle's brain. It ingests the camera feed, runs ML inference
on the Hexagon DSP via SNPE, evaluates whether the road ahead is safe to stop on, manages the
boarding window, and publishes fleet telemetry to Firebase — all in a continuous loop, on a
vehicle moving through real traffic.

This is not a web service. Latency is a hard constraint. Failures have physical consequences.
The code you write runs on Ubuntu 20.04 on a QRB5165 SoC in a minibus.

## What You'll Build

- `CameraFeedManager` — OpenCV + V4L2 feed ingestion at ≥10fps, ≥480p
- `FrameQualityFilter` — discard sub-480p frames before inference
- `HandSignalDetector` — SNPE inference on Hexagon DSP, 200ms latency budget
- `RouteManager` — safe stop zone evaluation via Google Roads API, 500ms budget
- `BoardingController` — 60-second boarding window, Firebase push
- `DispatchService` — firebase-admin facade, fleet telemetry at ≤5s intervals
- `ModelUpdateManager` — OTA `.dlc` model swap from Firebase Storage
- `DetectionLogger` — anonymised detection log, false positive rate tracking
- Hypothesis property-based tests for all 23 correctness properties

## Stack

- Python 3.11, Ubuntu 20.04
- Qualcomm SNPE 2.x (Hexagon DSP runtime, `.dlc` model format)
- OpenCV 4.x with V4L2 backend
- firebase-admin SDK 6.x
- pytest 7.x, Hypothesis 6.x, pytest-mock

## You're a Good Fit If You

- Have written Python services that run on Linux embedded hardware (Raspberry Pi, Jetson, RB5,
  or similar)
- Understand V4L2 and have worked with OpenCV `VideoCapture` on Linux
- Are comfortable reading hardware datasheets and SDK documentation
- Know what a latency budget means and how to measure it
- Have worked with a Firebase SDK (any language) in a production context
- Write defensive code — you assume the camera will fail, the network will drop, the model
  file will be corrupt

## Nice to Have

- Direct experience with Qualcomm SNPE or another on-device inference SDK (TFLite, ONNX Runtime)
- Experience with Hypothesis or another property-based testing library
- Familiarity with the Qualcomm RB5 / QRB5165 platform
- Any background in autonomous systems, robotics, or real-time control loops

## Interview Process

1. 30-minute intro call — we walk through the RB5 architecture, you ask hard questions
2. Take-home: implement a small Python component with a Hypothesis property test (2–3 hours)
3. Technical review — latency, failure modes, trade-offs
4. Founder conversation — culture, mission, compensation

---

---

# Role 3: ML Engineer — Hand Signal Recognition

## The Work

You'll own the model that makes the whole system work. The hand signal detector runs on a
Qualcomm RB5 at ≥10fps, must produce a confidence score within 200ms of frame capture, and
must maintain a false positive rate below 5% in daylight and in low-light conditions down to
10 lux. It needs to detect a hailing gesture from up to 30 metres lateral distance.

You'll own the full ML lifecycle: training data strategy, model architecture, training pipeline,
conversion to SNPE `.dlc` format, the CI accuracy gate (≥85% on the TakeMe validation dataset),
and OTA deployment via Firebase Storage.

## What You'll Build

- Training data collection and annotation pipeline for hand signal gestures
- Model architecture selection and training (likely MobileNet or EfficientDet family for
  on-device latency constraints)
- TFLite → SNPE `.dlc` conversion pipeline
- CI accuracy gate: automated evaluation against the TakeMe validation dataset before any
  Firebase Storage upload
- False positive rate monitoring integration with `DetectionLogger`
- Low-light augmentation strategy for night-mode detection

## Stack

- PyTorch or TensorFlow (your preference)
- Qualcomm SNPE model converter (TFLite/ONNX → `.dlc`)
- Python 3.11
- Firebase Storage (model binary hosting)
- Firebase Remote Config (version manifest)
- CI pipeline (GitHub Actions or equivalent)

## You're a Good Fit If You

- Have trained and deployed a real-time object detection or gesture recognition model
- Understand the constraints of on-device inference — model size, quantisation, latency
- Have converted models to a mobile/edge format (TFLite, ONNX, CoreML, SNPE `.dlc`)
- Know what precision/recall trade-offs mean in a safety-relevant context
- Can build a reproducible training pipeline, not just a notebook

## Nice to Have

- Experience with Qualcomm SNPE or the AI Model Efficiency Toolkit (AIMET)
- Background in human pose estimation or gesture recognition specifically
- Experience with low-light or night-mode image augmentation
- Familiarity with property-based testing for ML pipelines

## Interview Process

1. 30-minute intro call — we discuss the detection problem and constraints
2. Take-home: train a small gesture classifier and document your accuracy/latency trade-offs
3. Technical review — model choices, failure modes, deployment strategy
4. Founder conversation — culture, mission, compensation

---

---

# Role 4: Frontend Engineer — Operator Dashboard

## The Work

You'll build the operator dashboard — the web app that lets fleet supervisors monitor every
vehicle in real time. Live positions on a map. Camera failure alerts. False positive rate trends.
Payment failure escalations. Trip review. All driven by Firebase RTDB listeners with no polling.

This is a React 18 + TypeScript app. It needs to be fast, clear, and usable by someone who is
not a developer — a fleet supervisor watching 50 vehicles on a map at 6am.

## What You'll Build

- Live fleet map using Google Maps JavaScript API — vehicle positions updated from
  `/fleet_status` RTDB listener
- Alert panel: camera failures, FP rate spikes, vehicles flagged for inspection
- Trip review table: payment failures, flagged trips, operator actions
- False positive rate trend charts (Recharts)
- Real-time cancellation event feed with GPS coordinates and reason
- Firebase Auth integration for operator login

## Stack

- React 18, TypeScript
- Firebase JS SDK 10.x (`onValue()` RTDB listeners)
- Google Maps JavaScript API
- Recharts
- Firebase Auth

## You're a Good Fit If You

- Have built real-time dashboards with WebSocket or Firebase listeners
- Are comfortable with React state management for high-frequency data updates
- Care about information density — you know when a table is better than a chart
- Have worked with Google Maps JavaScript API or a similar mapping library
- Write clean, typed TypeScript

## Nice to Have

- Experience with fleet management or logistics dashboards
- Familiarity with Firebase Security Rules (you'll need to read them)
- Any background in transport, logistics, or operations tooling

## Interview Process

1. 30-minute intro call
2. Take-home: build a small real-time dashboard component with a Firebase RTDB mock (2–3 hours)
3. Technical review — component design, performance, UX decisions
4. Founder conversation — culture, mission, compensation

---

---

## Working at TakeMe

**Remote-first, South Africa timezone.** We work async by default and meet when it matters.

**Small team, high ownership.** There is no PM between you and the problem. You read the
requirements, you design the solution, you ship it, you monitor it.

**The work is real.** The code you write runs on a vehicle on a public road. That's motivating
and it's also a responsibility we take seriously.

**Compensation.** Competitive for the South African market, with equity. We'll discuss specifics
in the founder conversation — we don't play games with lowball offers.

**We don't care about your degree.** We care about what you've shipped and how you think.

---

## How to Apply

Send a short message to [careers@takeme.co.za] with:

- Which role you're applying for
- Two or three things you've built that are relevant (links, repos, or a brief description)
- One sentence on why this problem interests you

No cover letter required. No recruiters.
