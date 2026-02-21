# WoundCare Blueprint Summary

## Goals
- Measure wound area (cm²) reliably over time
- Offline-first storage (Room)
- Capture → QC → Calibration → Segmentation/manual edit → Measurement → Analytics → Export

## Core Screens
- Home
- Patient list/detail
- Wound list/detail
- Visit capture (CameraX)
- Analyze/Edit outline
- History timeline
- Analytics charts
- Export (CSV/PDF)

## Data Model
- Patient
- Wound
- Visit
- ImageAsset (stored in app-private storage)
- Measurement (area, method, version)
- DressingEvent (optional)
- Audit/QC flags

## Measurement Rules
- Require calibration marker
- Block/flag poor quality images (blur/glare/angle)
- Store rectified image parameters and scale
- Always store original image + derived measurements
