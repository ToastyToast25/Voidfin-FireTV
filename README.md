<p align="center">
   <img src="LOGOS/voidfin_logo.png" alt="VoidStream" width="300" />
</p>

<h1 align="center">Voidfin for Android TV</h1>
<h3 align="center">Enhanced Jellyfin client for Android TV, Nvidia Shield, and Amazon Fire TV devices</h3>

---

[![Release](https://img.shields.io/github/release/ToastyToast25/Voidfin-FireTV.svg)](https://github.com/ToastyToast25/Voidfin-FireTV/releases)

> **[← Back to main Voidfin project](https://github.com/ToastyToast25)**

Voidfin for Android TV is an enhanced fork of the official Jellyfin Android TV client, optimized for the viewing experience on Android TV, Nvidia Shield, and Amazon Fire TV devices.

## Features & Enhancements

Voidfin for Android TV builds on the solid foundation of Jellyfin with targeted improvements for TV viewing:

### Cross-Server Content Playback
- **Unified Library Support** - Seamless playback from multiple Jellyfin servers
- Seamless switching between servers for content playback
- Improved server selection logic

### SyncPlay (Beta)
- **Synchronized Group Playback** - Watch together with friends and family in perfect sync
- Dynamic playback speed adjustments based on drift calculations
- Buffering and ready state reporting for better synchronization
- User notifications for group join/leave events

### Playlist System
- **Full Playlist Support** - Create, manage, and share playlists
- Add to Playlist button on detail screens with modal selection
- Create new playlists or add to existing ones
- Public playlist support for sharing with other users
- Remove from Playlist on long press
- Replaced the previous local-only Watchlist feature

### Jellyseerr Integration

Voidfin is the first Android TV client with native Jellyseerr support.

- Browse trending, popular, and recommended movies/shows and filter content by Series/Movie Genres, Studio, Network, and keywords
- Request content in HD or 4K directly from your Roku
- **NSFW Content Filtering** (optional) using Jellyseerr/TMDB metadata
- Smart season selection when requesting TV shows
- View all your pending, approved, and available requests
- Authenticate using your Jellyfin login (permanent local API key saved)
- Global search includes Jellyseerr results
- Rich backdrop images for a more cinematic discovery experience

### MDBList Ratings Integration
- **Multiple Rating Sources** - Display ratings from various platforms:
  - AlloCine, AniList, Douban, IMDB, Kinopoisk
  - Letterboxd, Metacritic, MyAnimeList, Roger Ebert
  - TMDB, Trakt
- TMDB episode ratings support with configurable settings
- Episode ratings displayed in library views

### Customizable Toolbar
- **Toggle buttons** - Show/hide Shuffle, Genres, and Favorites buttons
- **Library row toggle** - Show/hide the entire library button row for a cleaner home screen
- **Shuffle filter** - Choose Movies only, TV Shows only, or Both
- **Pill-shaped design** - Subtle rounded background with better contrast
- Dynamic library buttons that scroll horizontally for 5+ libraries

### Featured Media Bar
- Rotating showcase of 15 random movies and TV shows right on your home screen
- **Profile-aware refresh** - Automatically refreshes content when switching profiles to prevent inappropriate content from appearing on child profiles
- See ratings, genres, runtime, and a quick overview without extra clicks
- Smooth crossfade transitions as items change, with matching backdrop images
- Height and positioning tuned for viewing from the couch

### Enhanced Navigation
- **Left Sidebar Navigation** - New sidebar with expandable icons/text and configurable navbar position
- **Folder View** - Browse media in folder structure for organized access
- Quick access home button (house icon) and search (magnifying glass)
- Shuffle button for instant random movie/TV show discovery with genre-specific shuffle on long press
- Genres redesigned as sortable tiles with random backdrop images
- Dynamic library buttons automatically populate based on your Jellyfin libraries
- One-click navigation to any library or collection directly from the toolbar
- Cleaner icon-based design for frequently used actions

### Playback & Media Control
- **ASS/SSA Subtitle Support** - Direct-play and rendering support for ASS/SSA subtitle formats
- **Subtitle Delay & Positioning** - Fine-tune subtitle sync and adjust position/size for wide aspect ratio videos
- **Max Video Resolution** - New preference to limit video resolution
- **Unpause Rewind** - Automatically rewinds a configurable amount when unpausing playback
- **Theme Music Playback** - Background theme music support for TV shows and movies with volume control
- **Pre-Playback Track Selection** - Choose your preferred audio track and subtitle before playback starts (configurable in settings)
- **Next Episode Countdown** - Skip button shows countdown timer when next episode is available
- **Automatic Screensaver Dimming** - Reduces brightness after 90 seconds of playback inactivity to prevent screen burn-in with dynamic logo/clock movement
- **Exit Confirmation Dialog** - Optional confirmation prompt when exiting the app (configurable in settings)
- **OTA Update System** - Automatic check for new Voidfin versions with in-app update notifications

### Improved Details Screen
- Metadata organized into clear sections: genres, directors, writers, studios, and runtime
- Taglines displayed above the description where available
- Cast photos appear as circles for a cleaner look
- Fits more useful information on screen without feeling cramped

### UI Polish
- **Adjustable Backdrop Blur** - Customizable background blur amount with slider control for personal preference
- **Media Bar Opacity Control** - Slider-based opacity adjustment for the featured media bar overlay
- Item details show up right in the row, no need to open every title to see what it is
- Buttons look better when not focused (transparent instead of distracting)
- Better contrast makes text easier to read
- Transitions and animations feel responsive
- Consistent icons and visual elements throughout

## Screenshots
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/6af5aa5a-b1eb-4db7-9fca-ea736bf7a686" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/afe9818e-360c-4ce0-a50c-ff0ed0cbc81a" />


<img width="1920" height="1080" alt="Screenshot_20251121_212922" src="https://github.com/user-attachments/assets/9cb2fd75-c336-4721-9842-d614d106b38a" />
<img width="1920" height="1080" alt="Screenshot_20251121_212955" src="https://github.com/user-attachments/assets/d7c7d13f-501c-4ca1-9441-8e9294124302" />

## Videos
https://github.com/user-attachments/assets/5d89961b-8067-4af4-9757-b4de51474fcf

https://github.com/user-attachments/assets/0414ffca-60f4-470a-94b9-6b3405b3570c


---

**Disclaimer:** Screenshots shown in this documentation feature media content, artwork, and actor likenesses for demonstration purposes only. None of the media, studios, actors, or other content depicted are affiliated with, sponsored by, or endorsing the Voidfin client or the Jellyfin project. All rights to the portrayed content belong to their respective copyright holders. These screenshots are used solely to demonstrate the functionality and interface of the application.

---

## Installation on Amazon Fire TV / Fire Stick

Follow these steps to install Voidfin on your Fire TV Stick or Fire TV device:

### Step 1: Enable Developer Options
1. Go to **Settings** on your Fire TV
2. Select **My Fire TV** (or **Device & Software**)
3. Select **About**
4. Click on **your Fire TV name** 7 times rapidly until you see "You are now a developer"

### Step 2: Enable Apps from Unknown Sources
1. Go back to **Settings → My Fire TV**
2. Select **Developer Options**
3. Turn on **Apps from Unknown Sources** (or **Install unknown apps**)
4. If prompted, confirm with **Turn On**

### Step 3: Install the Downloader App
1. From the Fire TV home screen, go to **Find → Search**
2. Search for **"Downloader"** (by AFTVnews, orange icon)
3. Install and open the Downloader app
4. When prompted, allow Downloader to access files

### Step 4: Download and Install Voidfin
1. Open the **Downloader** app
2. In the URL field, enter the download URL for the latest release:
   ```
   https://github.com/ToastyToast25/Voidfin-FireTV/releases/latest
   ```
3. Navigate to the **Assets** section and select the `.apk` file
4. The APK will download — when finished, select **Install**
5. Once installed, select **Done** (or **Open** to launch immediately)
6. Go back to Downloader and select **Delete** to remove the APK file and free up space

### Step 5: Launch Voidfin
1. Go to **Settings → Applications → Manage Installed Applications** to find Voidfin
2. Or look for **Voidfin** in your Apps & Channels row on the home screen
3. To move it to the front: long-press the app icon → select **Move to front**

### Alternative: Sideload via ADB
If you prefer using a computer:

1. Enable **ADB Debugging** in Developer Options on your Fire TV
2. Find your Fire TV's IP address: **Settings → My Fire TV → About → Network**
3. On your computer, connect via ADB:
   ```bash
   adb connect <FIRE_TV_IP>:5555
   ```
4. Download the APK from the [Releases page](https://github.com/ToastyToast25/Voidfin-FireTV/releases)
5. Install via ADB:
   ```bash
   adb install voidfin-androidtv-v*.apk
   ```

### Updating Voidfin
Voidfin includes a built-in **OTA Update System** that automatically checks for new versions. When an update is available, you'll receive an in-app notification with the option to download and install directly — no need to repeat the sideloading steps.

## Other Supported Devices

- **Android TV** (Android 6.0+) — Install the APK directly or via ADB
- **Nvidia Shield TV** — Same steps as above, or use the Downloader app
- **Google TV (Chromecast)** — Enable Developer Options, then sideload via Downloader or ADB

## Jellyseerr Setup (Optional)
To enable media discovery and requesting:

1. Install and configure Jellyseerr on your network ([jellyseerr.dev](https://jellyseerr.dev))
2. In Voidfin, go to **Settings → Jellyseerr**
3. Enter your Jellyseerr server URL (e.g., `http://192.168.1.100:5055`)
4. Click **Connect with Jellyfin** and enter your Jellyfin password
5. Test the connection, then start discovering!

Your session is saved securely and will reconnect automatically.

## Building from Source

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 11 or newer
- Android SDK with API 23+ installed

### Steps

1. **Clone the repository:**
```bash
git clone https://github.com/ToastyToast25/Voidfin-FireTV.git
cd Voidfin-FireTV
```

2. **Build debug version:**
```bash
./gradlew assembleDebug
```

3. **Install to connected device:**
```bash
./gradlew installDebug
```

4. **Build release version:**

First, create a `keystore.properties` file in the root directory (use `keystore.properties.template` as a guide):
```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Then build:
```bash
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/release/`

## Development

### Developer Notes
- Uses Gradle wrapper (no need to install Gradle separately)
- Android Studio is recommended for development
- Keep Android SDK and build tools updated
- Code style follows upstream Jellyfin conventions
- UI changes should be tested on actual TV devices when possible

## Contributing

We welcome contributions to Voidfin for Android TV!

### Guidelines
1. **Check existing issues** - See if your idea/bug is already reported
2. **Discuss major changes** - Open an issue first for significant features
3. **Follow code style** - Match the existing codebase conventions
4. **Test on TV devices** - Verify changes work on actual Android TV hardware
5. **Consider upstream** - Features that benefit all users should go to Jellyfin first!

### Pull Request Process
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes with clear commit messages
4. Test thoroughly on Android TV devices
5. Submit a pull request with a detailed description

## Translating

Translations are maintained through the Jellyfin Weblate instance:
- [Jellyfin Android TV on Weblate](https://translate.jellyfin.org/projects/jellyfin-android/jellyfin-androidtv)

Translations contributed to Voidfin that are universally applicable will be submitted upstream to benefit the entire community.

## Support & Community

- **Issues** - [GitHub Issues](https://github.com/ToastyToast25/Voidfin-FireTV/issues) for bugs and feature requests
- **Discussions** - [GitHub Discussions](https://github.com/ToastyToast25/Voidfin-FireTV/discussions) for questions and ideas
- **Upstream Jellyfin** - [jellyfin.org](https://jellyfin.org) for server-related questions

## Credits

Voidfin for Android TV is built upon the excellent work of:

- **[Jellyfin Project](https://jellyfin.org)** - The foundation and upstream codebase
- **[MakD](https://github.com/MakD)** - Original Jellyfin-Media-Bar concept that inspired our featured media bar
- **Jellyfin Android TV Contributors** - All the developers who built the original client
- **Voidfin Contributors** - Everyone who has contributed to this fork

## License

This project is proprietary software. All original additions, modifications, branding, and enhancements are copyright ToastyToast25. Some components are derived from the Jellyfin project and remain subject to the GPL v2. See the [LICENSE](LICENSE) file for full details.

---

<p align="center">
   <img src="LOGOS/voidfin_logo.png" alt="VoidStream" width="150" />
   <br>
   <strong>Voidfin for Android TV</strong> is an independent fork and is not affiliated with the Jellyfin project.<br>
   <a href="https://github.com/ToastyToast25">← Back to main Voidfin project</a>
</p>
