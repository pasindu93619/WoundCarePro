# WoundCarePro Project Plan

This plan is based on the blueprint pipeline for wound measurement and tracking and is intentionally sequenced as **one milestone at a time**.

**Required flow:**
Patient → Visit/Assessment → Capture image → Quality checks → Save image + metadata → Manual outline → Measurement → History → Export.

---

## Milestone 1 — Patient
**Goal:** Establish patient management as the entry point for all wound-tracking data.

### Scope
- Define patient entity and repository contracts.
- Build patient list and patient detail/create/edit flows in Jetpack Compose.
- Add input validation for required demographics/identifiers used by downstream workflows.

### Acceptance Criteria
- User can create, view, update, and archive a patient record.
- Patient list supports basic search/filter by name or identifier.
- Selecting a patient opens detail with clear CTA to start a new Visit/Assessment.
- Data persists across app restarts using Room.
- Package structure remains under `com.pasindu.woundcarepro`.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 2.

---

## Milestone 2 — Visit/Assessment
**Goal:** Introduce visit context so every capture and measurement is linked to a specific assessment.

### Scope
- Define Visit/Assessment entity linked to Patient.
- Implement create/view visit workflow (date/time, wound location, notes, clinician input fields).
- Add visit state model suitable for incomplete vs completed assessments.

### Acceptance Criteria
- A visit cannot exist without a valid patient reference.
- User can start a visit from patient detail and resume it later.
- Visit detail clearly shows status and required next action: Capture image.
- Visit metadata is persisted and queryable per patient timeline.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 3.

---

## Milestone 3 — Capture Image
**Goal:** Capture clinically usable wound photos with CameraX using device-safe defaults.

### Scope
- Integrate CameraX capture screen for active visit.
- Enforce rear main camera selection strategy (avoid macro/ultrawide paths for measurement reliability).
- Add on-screen guidance overlay for wound framing and calibration marker placement.

### Acceptance Criteria
- Capture screen is accessible only from an active visit.
- User can capture at least one image and preview it before acceptance.
- Capture flow stores temporary session state so accidental navigation does not lose progress.
- Camera permission denial path is handled gracefully with retry instructions.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 4.

---

## Milestone 4 — Quality Checks
**Goal:** Prevent poor-quality inputs from entering measurement pipeline.

### Scope
- Implement QC checks and decision states: pass, warn, reject.
- QC dimensions: marker presence, blur, glare, low light, camera tilt/angle.
- Build UX for QC results with retake path.

### Acceptance Criteria
- Missing calibration marker produces reject state.
- Blur/glare/lighting/tilt checks produce actionable warnings or rejection.
- User can retake image directly from QC screen without losing visit context.
- QC outcomes are logged for audit/traceability.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 5.

---

## Milestone 5 — Save Image + Metadata
**Goal:** Persist original image and all required metadata safely and versionably.

### Scope
- Save original (unmodified) image to app-private storage.
- Persist metadata in Room: patientId, visitId, timestamps, capture device/camera info, QC results, marker metadata.
- Introduce image asset versioning model for future derived artifacts.

### Acceptance Criteria
- Original image is saved in app-private storage and retrievable by visit.
- Room metadata record is created atomically with image save.
- Failed save does not leave orphaned/inconsistent records.
- Metadata schema supports downstream outline and measurement revisions.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 6.

---

## Milestone 6 — Manual Outline
**Goal:** Provide clinician control to refine/define wound boundary before measurement.

### Scope
- Build manual outline editor over captured image.
- Support create/edit/reset boundary operations.
- Track outline revision history per image/visit.

### Acceptance Criteria
- User can draw and edit a closed wound contour.
- Undo/reset actions are available and stable.
- Outline can be saved as a versioned artifact tied to image metadata.
- UI clearly indicates when outline is required before measurement.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 7.

---

## Milestone 7 — Measurement
**Goal:** Compute reproducible wound measurements from calibrated image and outline.

### Scope
- Implement pixel-to-mm scaling from calibration marker.
- Apply rectification assumptions/transform needed for planar measurement.
- Compute area (cm²), and optionally perimeter, with confidence/status fields.

### Acceptance Criteria
- Measurement cannot run without valid marker scale and saved outline.
- Area output is stored with formula/version metadata for traceability.
- Re-running after outline edits creates a new measurement version.
- Measurement result is linked to visit and visible in visit summary.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 8.

---

## Milestone 8 — History
**Goal:** Make longitudinal tracking clinically useful across visits.

### Scope
- Build patient/wound history timeline.
- Show area trend over time and baseline percentage change.
- Provide per-visit drill-down to image, outline, measurement, and notes.

### Acceptance Criteria
- Timeline displays visits in chronological order with key metrics.
- Trend view shows area change and percent change from baseline.
- User can compare at least two visits side-by-side (date, image, area).
- History reads only committed/approved measurements.

### Exit Gate
- Milestone demo complete and approved before starting Milestone 9.

---

## Milestone 9 — Export
**Goal:** Enable controlled data sharing for reporting and follow-up.

### Scope
- CSV export for structured visit + measurement + dressing metadata.
- PDF summary export for selected date range/visits.
- Export UX with explicit user action and privacy messaging.

### Acceptance Criteria
- CSV export includes required non-image clinical fields and opens/shareable output.
- PDF export includes summary metrics and selected visuals as defined in requirements.
- Export is user-initiated only; no background auto-export.
- Export actions are auditable (who/when/what range).

### Exit Gate
- Milestone demo complete and approved before implementation hardening/release prep.

---

## Definition of Done (applies to every milestone)
- Feature scope is limited to the current milestone only (no unrelated refactors).
- Build verification passes with `./gradlew assembleDebug`.
- New/changed code follows project stack constraints: Kotlin + Jetpack Compose + Room + CameraX + Hilt.
- Data relationships remain consistent with package namespace `com.pasindu.woundcarepro`.
- Milestone is reviewed and explicitly approved before moving forward.
