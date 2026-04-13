# Mock Media Resources

Place the following files in this directory for full mock mode display:

| File | Purpose | Format |
|------|---------|--------|
| `keyframe-leaf-disease.jpg` | AI keyframe - leaf disease | JPEG, 640×480+ |
| `keyframe-pest-detect.jpg` | AI keyframe - pest detection | JPEG, 640×480+ |
| `live-greenhouse.mp4` | Live monitoring placeholder | MP4 H.264, 1920×1080 |
| `playback-morning.mp4` | Playback clip - morning | MP4 H.264, 1920×1080 |
| `playback-afternoon.mp4` | Playback clip - afternoon | MP4 H.264, 1280×720 |

These files are served at `/api/mock-media/{filename}` when the backend runs in mock mode.
