# Wound Segmentation Model

Place the TensorFlow Lite model at:

- `models/wound_segmentation.tflite`

Expected model contract:

- **Input size:** `1 x 256 x 256 x 3` (RGB image tensor)
- **Output shape:**
  - **Binary segmentation:** `1 x 256 x 256 x 1`
  - **Multiclass segmentation:** `1 x 256 x 256 x C` (where `C` is number of classes)
