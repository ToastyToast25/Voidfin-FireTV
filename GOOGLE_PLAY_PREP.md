# Google Play Store Preparation Guide

## Phase 1: Android TV Emulator Setup

### Creating Android TV Emulator

1. **Open Android Studio AVD Manager:**
   ```
   Tools → Device Manager → Create Device
   ```

2. **Select TV Device:**
   - Category: TV
   - Hardware: 1080p Android TV (1920x1080)
   - Click "Next"

3. **Select System Image:**
   - Release: Android 13.0 (API 33) or higher
   - ABI: x86_64 (for emulator performance)
   - Download if needed
   - Click "Next"

4. **Verify Configuration:**
   - AVD Name: VoidStream_TV_Test
   - Startup orientation: Landscape
   - Click "Finish"

5. **Launch Emulator:**
   - Click play button next to the AVD
   - Wait for Android TV home screen to load

### Installing VoidStream for Testing

```bash
# Build Google Play debug APK
cd C:\Users\Administrator\Desktop\Moon\Voidfin-FireTV
gradlew assembleGoogleplayDebug

# Install on emulator
adb install -r app\build\outputs\apk\googleplay\debug\voidstream-androidtv-v2.0.1-googleplay-debug.apk

# Launch app
adb shell am start -n org.voidstream.androidtv/.ui.startup.StartupActivity
```

## Phase 2: Testing Checklist

### D-Pad Navigation Testing

- [ ] Navigate through all main menu items (Home, Library, Search, Settings)
- [ ] Navigate through library grid with D-pad
- [ ] Navigate through settings with D-pad
- [ ] Test back button returns to previous screen
- [ ] Test focus indicator is visible on all focusable items
- [ ] Test focus doesn't get trapped in any screen

### Video Playback Testing

- [ ] Launch a video with D-pad center button
- [ ] Pause/resume with D-pad center button
- [ ] Seek forward/backward with D-pad left/right
- [ ] Open playback controls overlay
- [ ] Navigate playback controls with D-pad
- [ ] Test subtitle selection with D-pad
- [ ] Test audio track selection with D-pad
- [ ] Exit playback with back button

### Search Testing

- [ ] Open search screen
- [ ] Test on-screen keyboard navigation with D-pad
- [ ] Test text input with TV remote
- [ ] Test search results display
- [ ] Navigate search results with D-pad

### Settings Testing

- [ ] Navigate all settings categories
- [ ] Toggle boolean settings with D-pad
- [ ] Open dropdown menus with D-pad
- [ ] Verify "Check for Updates" button works
- [ ] Verify no OTA-specific UI is visible

### Server Connection Testing

- [ ] Add new server with D-pad navigation
- [ ] Enter server URL with on-screen keyboard
- [ ] Login with username/password
- [ ] Verify home screen loads after login
- [ ] Test switching between users (if applicable)

## Phase 3: Screenshot Capture

### Required Screenshots (1920x1080)

You need 3-5 high-quality screenshots showing key features:

1. **Home Screen** - Library overview with content
2. **Video Playback** - Player UI with controls visible
3. **Library Grid** - Movies or TV shows grid view
4. **Settings Screen** - Settings interface
5. **Search Screen** (optional) - Search interface with results

### Capturing Screenshots from Emulator

**Method 1: Emulator Built-in Screenshot**
- Click the camera icon in emulator toolbar
- Saves to default location (usually Desktop)

**Method 2: ADB Screenshot**
```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png

# Pull to computer
adb pull /sdcard/screenshot.png C:\Users\Administrator\Desktop\VoidStream_Screenshots\screenshot.png
```

**Method 3: Android Studio Screenshot**
- View → Tool Windows → Device Capture
- Click camera icon

### Screenshot Guidelines

- **Resolution:** Exactly 1920x1080 (TV landscape)
- **Format:** PNG or JPEG
- **Quality:** High quality, no compression artifacts
- **Content:** Show actual app content (not empty states)
- **UI:** All overlays should be visible and focused
- **Text:** Ensure all text is readable at full resolution

### Screenshot Naming Convention

Save screenshots with descriptive names:
```
voidstream_tv_home_screen.png
voidstream_tv_video_playback.png
voidstream_tv_library_grid.png
voidstream_tv_settings.png
voidstream_tv_search.png
```

## Phase 4: TV Banner Graphic

### Requirements

- **Resolution:** 1280x720 pixels
- **Format:** PNG (with transparency) or JPEG
- **Content:** VoidStream logo + "for Android TV" text
- **Background:** Branded colors (dark theme)

### Creating the Banner

Option 1: Use existing logo and add text overlay
Option 2: Create new branded banner in image editor

**Banner should include:**
- VoidStream logo (centered or left-aligned)
- "for Android TV" tagline
- Clean, professional design
- Matches app branding colors

### Banner Location

Save to: `C:\Users\Administrator\Desktop\VoidStream_Assets\tv_banner.png`

## Phase 5: App Description

### Title (Max 50 characters)
```
VoidStream - Your Media on Android TV
```

### Short Description (Max 80 characters)
```
Stream your Jellyfin media library seamlessly on Android TV and Fire TV devices
```

### Full Description (Max 4000 characters)

```
VoidStream brings your Jellyfin media library to your Android TV, Fire TV, and Google TV devices with a beautiful, TV-optimized interface designed for the big screen.

KEY FEATURES

📺 Optimized for TV
• Fully D-pad navigable interface designed for Android TV
• Clean, modern UI optimized for 10-foot viewing
• Fast navigation through your media library
• Smooth playback with hardware acceleration

🎬 Complete Media Support
• Movies, TV shows, music, and live TV
• Direct play and transcoding support
• Multiple audio tracks and subtitle support
• Resume playback across devices

🎨 Beautiful Interface
• Backdrop images and rich metadata
• Poster and thumbnail views
• Genre and collection browsing
• Search with on-screen keyboard

⚙️ Customizable Experience
• Configurable home screen rows
• Playback quality settings
• Subtitle customization
• Audio preferences

🔐 Secure & Private
• Direct connection to your Jellyfin server
• No data collection or tracking
• Your media stays on your server
• Supports multiple user profiles

REQUIREMENTS
• Jellyfin server (10.8.0 or higher recommended)
• Android TV, Fire TV, or Google TV device
• Network connection to your Jellyfin server

SUPPORTED DEVICES
• Android TV devices (Shield TV, Mi Box, etc.)
• Fire TV devices (Fire Stick 4K, Fire TV Cube)
• Google TV devices (Chromecast with Google TV)
• Smart TVs with Android TV built-in

VoidStream is a fork of the Jellyfin Android TV client, enhanced with additional features and optimizations for the best Android TV experience.

For support and updates, visit our GitHub repository or contact us through the app settings.
```

### Categories

- Primary: Entertainment
- Secondary: Video Players & Editors

### Content Rating

- Declare content rating based on media your server hosts
- Most likely: ESRB Everyone or Teen
- Note: App itself doesn't contain rated content (it's a player)

## Phase 6: Building AAB for Submission

### Build Signed AAB

```bash
# Build Google Play release AAB
cd C:\Users\Administrator\Desktop\Moon\Voidfin-FireTV
gradlew bundleGoogleplayRelease

# Output location:
# app\build\outputs\bundle\googleplayRelease\voidstream-androidtv-v2.0.1-googleplay-release.aab
```

### Verify AAB Contents

```bash
# Check AAB size (should be ~35-40MB)
dir app\build\outputs\bundle\googleplayRelease\

# Verify signing
jarsigner -verify -verbose -certs app\build\outputs\bundle\googleplayRelease\voidstream-androidtv-v2.0.1-googleplay-release.aab
```

### AAB Verification Checklist

- [ ] AAB is signed with release keystore
- [ ] File size is reasonable (~35-40MB)
- [ ] Version code matches gradle.properties
- [ ] Version name matches gradle.properties (2.0.1)
- [ ] No debug symbols included (release build)

## Phase 7: Play Console Setup

### Before Opening Play Console

Ensure you have:
- [ ] 3-5 TV screenshots (1920x1080)
- [ ] TV banner graphic (1280x720)
- [ ] App description text ready
- [ ] Signed release AAB
- [ ] Privacy policy URL (if collecting data)
- [ ] Content rating information

### Creating TV Track

1. **Log in to Google Play Console:**
   - https://play.google.com/console

2. **Create App:**
   - Click "Create app"
   - App name: VoidStream
   - Default language: English (United States)
   - App type: App
   - Free or paid: Free (or Paid if you plan subscriptions)

3. **Set Up App Category:**
   - Category: Entertainment → Video Players & Editors
   - Tags: android tv, fire tv, jellyfin, media player, streaming

4. **Upload TV Assets:**
   - Screenshots: Upload all 3-5 TV screenshots
   - TV banner: Upload TV banner graphic
   - Feature graphic: 1024x500 (create from TV banner if needed)

5. **Set TV Requirements:**
   - Go to "Device catalog"
   - Select "Android TV" as supported
   - Set minimum requirements:
     - Android 8.0 (API 26) or higher
     - 1080p resolution support

6. **Upload AAB:**
   - Go to "Release" → "Production"
   - Click "Create new release"
   - Upload AAB file
   - Fill in release notes
   - Set rollout percentage (start with 10-20% for monitoring)

7. **Complete Store Listing:**
   - Paste app description
   - Add screenshots
   - Upload graphics
   - Set content rating
   - Add privacy policy if applicable

8. **Submit for Review:**
   - Review all sections (green checkmarks)
   - Click "Submit for review"
   - Wait 1-7 days for review completion

## Post-Submission Monitoring

### After Submission

- [ ] Monitor Play Console for review status
- [ ] Check for any review feedback or rejections
- [ ] Monitor crash reports in Play Console
- [ ] Check user reviews and respond promptly
- [ ] Monitor ANR (Application Not Responding) reports
- [ ] Update app based on user feedback

### Common Rejection Reasons

1. **Missing TV banner** - Ensure TV banner is uploaded
2. **Poor TV navigation** - Verify D-pad navigation works everywhere
3. **Missing permissions explanation** - Explain why each permission is needed
4. **Content rating mismatch** - Ensure rating matches actual content
5. **Broken functionality** - Test all features before submission

## Notes

- First review typically takes 3-7 days
- Subsequent updates usually reviewed within 1-3 days
- Google may test on real TV devices during review
- Keep release keystore backed up securely
- Version code must increment with each submission
- Can't downgrade users to lower version code

## Quick Reference Commands

```bash
# Build Google Play debug for testing
gradlew assembleGoogleplayDebug

# Build Google Play release AAB for submission
gradlew bundleGoogleplayRelease

# Install debug APK on emulator
adb install -r app\build\outputs\apk\googleplay\debug\voidstream-androidtv-v2.0.1-googleplay-debug.apk

# Take screenshot from emulator
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png screenshot.png

# Check connected devices
adb devices

# Launch app on emulator
adb shell am start -n org.voidstream.androidtv/.ui.startup.StartupActivity
```
