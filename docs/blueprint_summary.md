# WoundCare Blueprint Summary (must-follow)

## Core pipeline
Capture (CameraX) -> QC gates (blur/glare/lighting/angle/marker) -> save ORIGINAL image (app-private)
-> rectify plane via marker homography -> segment wound mask -> manual refine -> compute area cm²
-> store in Room -> timeline/analytics -> export CSV/PDF

## Measurement requirements
- MUST use main rear camera (avoid ultrawide/macro for measurement)
- MUST require calibration marker: either 2 rulers/markers on opposite sides OR ArUco/AprilTag
- MUST compute pixel->mm scale and use homography rectification
- QC must warn/reject for: missing marker, glare, blur, low light, high tilt angle, low segmentation confidence
- Store original image and derived measurements with versioning

## Data model (Room)
Clinician, Patient, Wound, Visit, ImageAsset, Measurement, DressingEvent, Consent, AuditLog
Images stored in app-private storage; metadata in Room; export only via explicit user action

## Screens
Home; Patient list/detail; Wound list/detail; Visit capture (guidance overlay); Review (accept/retake/refine);
Manual refine editor; Timeline; Compare two dates; Analytics; Export

## Analytics
Days vs wound area; % change from baseline; healing rate; area vs dressing method; optional tissue proportions

## Export
CSV: visits + measurements + dressing metadata (no images default)
PDF: summary with selected cropped images + area graph + % change + notes
