# Miogram Easter Eggs & Hidden Features

> **Miogram / Міограм** · Fun & Hidden Features Guide  
> Author: **[@dkramochka](https://t.me/dkramochka)**

---

## 1. The "Musordrop" Easter Egg (`MiogramMusorDrop`)

### Overview
Miogram features an interactive full-screen audio-visual experience known as **Musordrop**.

### Activation Methods
Musordrop can be triggered in several convenient ways:
1. **Chat Message Command:** Type `/musordrop`, `/musor_drop`, or `tg://musor_drop` into any chat input field and tap **Send**. The client intercepts the input before sending, clears the text field, and plays the video.
2. **Clickable Links:** Tap any `tg://musor_drop`, `tg://musordrop`, `t.me/musor_drop`, or `t.me/musordrop` hyperlink inside any message, bio, or channel.
3. **Settings Secret:** In **Settings -> Налаштування Miogram**, tap any section info footer (e.g. under Exclusive Features, Appearance, or System).
4. **External Deep Links:** Opening `tg://musor_drop` from a web browser or another application routes through `LaunchActivity` directly to the easter egg.

### Hardware-Accelerated Video Architecture
* **TextureView Rendering:** Completely replaces legacy `VideoView` / `SurfaceView` punch-through surfaces. This guarantees zero Z-order occlusion bugs or black screen artifacts when placed inside a black background overlay.
* **Proportional Aspect-Ratio Scaling:** Dynamically measures video dimensions (`getVideoWidth()`, `getVideoHeight()`) against screen dimensions and applies a matrix transformation to center-crop/fit the video cleanly without stretching.
* **Multi-Stage Media Resolution Pipeline:**
  1. **Bundled Asset Direct FD:** Attempts direct zero-copy playback from APK assets (`ctx.getAssets().openFd("musordrop.mp4")`).
  2. **Internal App Storage (`filesDir`):** If uncompressed asset FD is not available, safely streams the asset into private storage with size verification.
  3. **External & Public Directories:** Checks `Downloads`, `Download`, and `Telegram` directories for both `.mp4` and `.mp3` variants.
* **Touch-to-Dismiss:** Tap anywhere on the screen to instantly close the overlay and return to the active screen.
