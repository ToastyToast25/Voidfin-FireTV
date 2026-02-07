# VoidStream-FireTV

## GitHub Repository

- **Origin:** https://github.com/ToastyToast25/VoidStream-FireTV
- **Upstream:** https://github.com/jellyfin/jellyfin-androidtv (original Jellyfin repo)

## Git Remotes

```bash
# Verify remotes
git remote -v

# Expected output:
# origin    https://github.com/ToastyToast25/VoidStream-FireTV.git (fetch)
# origin    https://github.com/ToastyToast25/VoidStream-FireTV.git (push)
# upstream  https://github.com/jellyfin/jellyfin-androidtv.git (fetch)
# upstream  https://github.com/jellyfin/jellyfin-androidtv.git (push)
```

## Pushing Changes

```bash
# Stage specific files
git add <file1> <file2>

# Stage all changes
git add -A

# Commit
git commit -m "Your commit message"

# Push to your repo
git push origin master
```

## Pulling Upstream Updates

```bash
# Fetch latest from original Jellyfin repo
git fetch upstream

# Merge upstream changes into your branch
git merge upstream/master
```

## MCP Servers

The following MCP servers are configured and available for use:

### Context7 (Library Documentation)
- **Purpose:** Fetch up-to-date library/API documentation, code examples, and setup guides
- **Tools:** `resolve-library-id`, `query-docs`
- **Usage:** Always use Context7 when needing library/API documentation, code generation, setup or configuration steps — without the user having to explicitly ask

### GitHub (GitHub API)
- **Purpose:** Full GitHub integration — issues, PRs, branches, commits, code search, reviews, releases
- **Tools:** `create_pull_request`, `list_issues`, `search_code`, `create_branch`, `add_issue_comment`, and many more
- **Account:** ToastyToast25
- **Repo:** ToastyToast25/VoidStream-FireTV

### Playwright (Browser Automation)
- **Purpose:** Browser automation for testing, screenshots, form filling, navigation
- **Tools:** `browser_navigate`, `browser_click`, `browser_snapshot`, `browser_take_screenshot`, etc.

### MarkItDown (File Conversion)
- **Purpose:** Convert files (PDF, Word, Excel, images, etc.) to markdown
- **Tools:** `convert_to_markdown`

### Desktop Commander (System Operations)
- **Purpose:** Persistent terminal sessions, file operations, process management, desktop screenshots
- **Tools:** `start_process`, `read_process_output`, `list_processes`, `kill_process`, `get_file_info`, etc.
- **Key advantage:** Long-running processes that persist beyond normal timeout limits

## Signing / Keystore

Keystore is generated and ready for release signing.

- **Keystore file:** `release.keystore` (JKS, RSA 2048-bit, valid ~27 years)
- **Key alias:** `voidstream`
- **Store password:** `Frostbite2531!`
- **Key password:** `Frostbite2531!hrm`
- **Config file:** `keystore.properties` (loaded by `app/build.gradle.kts`)
- **Template:** `keystore.properties.template` (safe to commit, has placeholder values)
- Both `release.keystore` and `keystore.properties` are git-ignored (secrets)

```bash
# Build signed release APK
./gradlew assembleRelease

# Output location
# app/build/outputs/apk/release/voidstream-androidtv-v*.apk

# Change keystore password (recommended)
keytool -storepasswd -keystore release.keystore
keytool -keypasswd -keystore release.keystore -alias voidstream

# Verify keystore
keytool -list -v -keystore release.keystore -alias voidstream
```

**IMPORTANT:** Back up `release.keystore` securely. If lost, you cannot update any APK signed with it.

## Build Flavors (Distribution)

VoidStream uses Gradle **product flavors** to produce separate builds for different distribution channels. A single `distribution` flavor dimension controls which features are enabled.

### Available Flavors

| Flavor | Purpose | OTA Updates | Donate Button | Install Permission | Build Command |
|--------|---------|-------------|---------------|--------------------|---------------|
| **`github`** | Sideloaded builds (GitHub Releases) | Yes | Yes | Yes | `./gradlew assembleGithubRelease` |
| **`amazon`** | Amazon Appstore submission | No | No | No | `./gradlew assembleAmazonRelease` |
| **`googleplay`** | Google Play Store (Shield, Chromecast, etc.) | No | No | No | `./gradlew assembleGoogleplayRelease` |

### APK / AAB Output Locations

```
app/build/outputs/apk/github/release/voidstream-androidtv-v{VERSION}-github-release.apk
app/build/outputs/apk/amazon/release/voidstream-androidtv-v{VERSION}-amazon-release.apk
app/build/outputs/apk/googleplay/release/voidstream-androidtv-v{VERSION}-googleplay-release.apk
app/build/outputs/bundle/googleplayRelease/voidstream-androidtv-v{VERSION}-googleplay-release.aab
```

Google Play requires AAB format for store submission. Use `./gradlew bundleGoogleplayRelease` to build the AAB. APK builds are still available for local testing.

### How the Gating System Works

Features are gated using **two mechanisms**:

**1. BuildConfig flags (runtime gating)**

Two separate compile-time boolean constants are set per flavor in `app/build.gradle.kts`:

| Flavor | `IS_AMAZON_BUILD` | `IS_GOOGLE_PLAY_BUILD` |
|--------|-------------------|------------------------|
| `github` | `false` | `false` |
| `amazon` | `true` | `false` |
| `googleplay` | `false` | `true` |

Amazon and Google Play have **separate flags** because they have different payment systems and store policies that may diverge in the future (Amazon IAP vs Google Play Billing).

Code uses `if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) { ... }` to gate features that are only available on sideloaded (github) builds. This gates:
- OTA update check on startup (`StartupActivity.kt`)
- "What's New" dialog after OTA update (`StartupActivity.kt`)
- Forced update screen (`StartupActivity.kt`)
- Background update worker scheduling (`JellyfinApplication.kt`)
- UpdateCheckerService DI registration (`AppModule.kt`)
- "Check for Updates" button in Settings (`SettingsMainScreen.kt`)
- "Update Notifications" toggle in Settings (`SettingsMainScreen.kt`)
- "Beta Updates" toggle in Settings (`SettingsMainScreen.kt`)
- "Support VoidStream" donate button in Settings (`SettingsMainScreen.kt`)
- Update overlay in Settings (`SettingsMainScreen.kt`)

Use `BuildConfig.IS_AMAZON_BUILD` or `BuildConfig.IS_GOOGLE_PLAY_BUILD` individually for store-specific gating (e.g., Amazon IAP vs Google Play Billing).

**2. Manifest split (compile-time gating)**

Flavor-specific `AndroidManifest.xml` files are merged with `main/AndroidManifest.xml` at build time:
- `app/src/github/AndroidManifest.xml` — Adds `REQUEST_INSTALL_PACKAGES` permission and `FileProvider` (needed for APK installation)
- `app/src/amazon/AndroidManifest.xml` — Empty (no install permission, no FileProvider)
- `app/src/googleplay/AndroidManifest.xml` — Empty (no install permission, no FileProvider)

The Android manifest merger automatically combines the appropriate flavor manifest with the main manifest.

### Files Involved in Gating

| File | What's Gated |
|------|-------------|
| `app/build.gradle.kts` | Flavor definitions, `IS_AMAZON_BUILD` + `IS_GOOGLE_PLAY_BUILD` fields |
| `app/src/main/AndroidManifest.xml` | Shared permissions (no install permission here) |
| `app/src/github/AndroidManifest.xml` | `REQUEST_INSTALL_PACKAGES` + FileProvider |
| `app/src/amazon/AndroidManifest.xml` | Empty (no OTA-related entries) |
| `app/src/googleplay/AndroidManifest.xml` | Empty (no OTA-related entries) |
| `AppModule.kt` | `UpdateCheckerService` Koin registration |
| `JellyfinApplication.kt` | `UpdateCheckWorker` scheduling |
| `StartupActivity.kt` | OTA update check + What's New dialog |
| `SettingsMainScreen.kt` | 4 UI items (updates + donate) + update overlay |

### Adding a New Gated Feature

To gate a new feature for specific flavors:

1. **Runtime gate (sideloaded only):** Wrap with `if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) { ... }`
2. **Runtime gate (store-specific):** Use `BuildConfig.IS_AMAZON_BUILD` or `BuildConfig.IS_GOOGLE_PLAY_BUILD` individually
3. **Permission gate:** Add the permission to the flavor-specific manifest instead of `main/`
4. **DI gate:** Wrap the Koin `single { }` registration with the BuildConfig check
5. **IMPORTANT:** If a service is only registered for certain flavors, make sure NO code path on excluded flavors can access it (lazy delegates like `by inject()` crash when resolved if the bean isn't registered)

## OTA Update System (GitHub Flavor Only)

The OTA update system is **only active in the `github` flavor**. On the `amazon` flavor, all update code paths are skipped — updates are handled by the Amazon Appstore.

### How It Works

1. **Update checking:** `UpdateCheckerService` queries the GitHub Releases API (`ToastyToast25/VoidStream-FireTV`)
2. **Startup check:** On app launch, `StartupActivity` checks for forced updates (blocks app if `[FORCE]` tag is in release notes)
3. **Background check:** `UpdateCheckWorker` runs daily via WorkManager to check for new releases
4. **Optional vs Forced:**
   - `[FORCE]` in release body → App blocks on forced update screen until installed
   - No `[FORCE]` → Update is logged but app continues normally
5. **Beta updates:** Pre-release GitHub Releases are only shown to users with "Beta updates" enabled in Settings
6. **Download & verify:** APK is downloaded to cache, SHA-256 checksum verified against GitHub's digest, then user is prompted to install via `ACTION_INSTALL_PACKAGE` intent
7. **What's New:** Release notes are saved to SharedPreferences before install. On next launch, a full-screen "What's New" fragment is shown, then cleared

### Publishing an OTA Update (GitHub Flavor)

#### 1. Bump Version

Edit `gradle.properties`:

```properties
voidstream.version=X.Y.Z
```

#### 2. Build Release APK

```bash
./gradlew assembleGithubRelease
# Output: app/build/outputs/apk/github/release/voidstream-androidtv-vX.Y.Z-github-release.apk
```

#### 3. Commit and Push

```bash
git add -A
git commit -m "Description of changes — vX.Y.Z"
git push origin master
```

#### 4. Create GitHub Release

Use `gh api` (not `gh release create` — it has auth scope issues):

```bash
# Create the release
# Use -F (not -f) for boolean fields (draft, prerelease)
# Include [FORCE] in the body to make it a forced update (blocks app until installed)
# Omit [FORCE] for optional updates (users can skip)
gh api repos/ToastyToast25/VoidStream-FireTV/releases -X POST \
  -f tag_name="vX.Y.Z" \
  -f target_commitish="master" \
  -f name="VoidStream vX.Y.Z" \
  -f body="Release notes here. Include [FORCE] to force update." \
  -F draft=false \
  -F prerelease=false \
  --jq '.id,.upload_url,.html_url'

# Upload the APK (replace RELEASE_ID with the id from above)
gh api "https://uploads.github.com/repos/ToastyToast25/VoidStream-FireTV/releases/RELEASE_ID/assets?name=voidstream-androidtv-vX.Y.Z-github-release.apk" \
  -X POST \
  --input "app/build/outputs/apk/github/release/voidstream-androidtv-vX.Y.Z-github-release.apk" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --jq '.name,.size,.browser_download_url'
```

#### Pre-release / Beta Builds

```bash
-F prerelease=true
```

Only users with "Beta updates" enabled in Settings will see pre-release builds.

## CI/CD Workflows

GitHub Actions workflows are in `.github/workflows/`.

### App Build (`app-build.yaml`)
- **Triggers:** Push to `master` or `release-*`, and all pull requests
- **What it does:** Checks out code, sets up Java + Gradle, runs `./gradlew assembleDebug`, uploads APK artifacts
- **Retention:** 14 days for build artifacts

### Update README Badges (`update-badges.yaml`)
- **Triggers:** Push to `master` when `gradle/libs.versions.toml` or `gradle/wrapper/gradle-wrapper.properties` change
- **What it does:** Parses version numbers from the TOML catalog and Gradle wrapper, then updates the shields.io badge URLs in `README.md`
- **Versions tracked:** Android SDK (min–target), Kotlin, Java, Gradle, Jellyfin SDK, Media3, Jetpack Compose
- **Commit message:** `Update README version badges [skip ci]` (uses `[skip ci]` to prevent infinite loop)
- **How badge matching works:** Each badge URL contains a unique color code suffix (e.g., `Kotlin-{VERSION}-7F52FF`). The workflow uses `sed` to match and replace the version portion between the badge name and color code.

### Store Compliance (`store-compliance.yaml`)
- **Triggers:** Push to `master` and all pull requests
- **What it does:** Builds Amazon and Google Play debug builds, then runs automated compliance checks
- **Jobs:**
  - `amazon-compliance` — Builds `amazonDebug` APK and verifies Amazon Appstore policy compliance
  - `google-play-compliance` — Builds `googleplayDebug` APK and verifies Google Play policy compliance
- **Compliance checks:**
  - No `REQUEST_INSTALL_PACKAGES` permission (prohibited by both stores)
  - No `FileProvider` in manifest (not needed for store builds)
  - BuildConfig flags are present (`IS_AMAZON_BUILD` / `IS_GOOGLE_PLAY_BUILD`)
- **Badge status:** Green (passing) means the build is compliant and ready for store submission
- **Note:** Debug builds are used to avoid requiring signing keys in CI. The gating is flavor-based, not build-type-based, so debug and release builds have identical compliance characteristics.

### When Bumping Versions
- The badge workflow runs **automatically** — no manual README edits needed when updating `libs.versions.toml` or the Gradle wrapper
- The Release badge (`img.shields.io/github/release/...`) is dynamic and always shows the latest GitHub release tag
- If you add a new badge, add a corresponding `sed` replacement in `update-badges.yaml`

## Amazon Appstore Restrictions

The following features are **prohibited** by Amazon Appstore policies and are removed/gated in the `amazon` flavor:

### 1. Self-Updating / OTA Updates
- **Policy:** Apps may not "deliver additional executable code" outside the Amazon Appstore update mechanism
- **Our solution:** All OTA update code is gated behind `BuildConfig.IS_AMAZON_BUILD`
- **What's removed:** Update checker, forced update screen, What's New dialog, background update worker, `REQUEST_INSTALL_PACKAGES` permission, FileProvider

### 2. External Monetization
- **Policy:** All digital purchases must go through Amazon IAP (In-App Purchasing)
- **Our solution:** The "Support VoidStream" donate button (QR code to external payment) is hidden on Amazon builds
- **Future:** Implement Amazon IAP for subscriptions when ready

### 3. Permissions
- **`REQUEST_INSTALL_PACKAGES`:** Removed from Amazon manifest (only needed for OTA APK installation)
- **`RECORD_AUDIO`:** Kept but declared optional (`android.hardware.microphone` with `required="false"`). Handled gracefully if denied — voice search button is disabled, text search always available as fallback

### 4. GPL v2 Compliance (Deferred)
- VoidStream is a fork of Jellyfin (GPL v2). Closed-source distribution may conflict with GPL v2
- **Status:** Deferred — needs legal review before Amazon submission

## Amazon Appstore Compliance Checklist

**IMPORTANT:** Run through this checklist before every Amazon Appstore submission.

### Pre-Submission Checks

- [ ] Build the Amazon flavor: `./gradlew assembleAmazonRelease`
- [ ] Install on emulator and verify:
  - [ ] App launches without crash
  - [ ] No "Check for Updates" in Settings
  - [ ] No "Beta Updates" toggle in Settings
  - [ ] No "Update Notifications" toggle in Settings
  - [ ] No "Support VoidStream" donate button in Settings
  - [ ] No forced update screen on startup
  - [ ] No `REQUEST_INSTALL_PACKAGES` in `aapt dump permissions` output
  - [ ] No FileProvider in merged manifest
- [ ] Test D-pad navigation through all screens
- [ ] Test RECORD_AUDIO denial (voice search degrades to text-only)
- [ ] Verify app icon is 512x512 PNG with transparency (see `LOGOS/`)
- [ ] Update `AMAZON_PRIVACY_DISCLOSURE.md` if data collection has changed
- [ ] Prepare 3-10 Fire TV screenshots (1920x1080) in `SCREENSHOTS/`
- [ ] Verify content rating is accurate

### Verifying the Amazon APK

```bash
# Check permissions (REQUEST_INSTALL_PACKAGES should NOT appear)
aapt dump permissions app/build/outputs/apk/amazon/release/voidstream-androidtv-v*.apk

# Check for FileProvider (should NOT appear)
aapt dump xmltree app/build/outputs/apk/amazon/release/voidstream-androidtv-v*.apk AndroidManifest.xml | grep FileProvider

# Install and test
adb install -r app/build/outputs/apk/amazon/release/voidstream-androidtv-v*.apk
```

### When Adding New Features

If adding a feature that involves any of the following, it must be gated for store builds:

- **Downloading or installing APKs** — Gate behind both `IS_AMAZON_BUILD` and `IS_GOOGLE_PLAY_BUILD`
- **External payment links** — Gate behind both `IS_AMAZON_BUILD` and `IS_GOOGLE_PLAY_BUILD`
- **Self-update mechanisms** — Gate behind both `IS_AMAZON_BUILD` and `IS_GOOGLE_PLAY_BUILD`
- **Amazon-specific features (Amazon IAP)** — Gate behind `IS_AMAZON_BUILD` only
- **Google Play-specific features (Play Billing)** — Gate behind `IS_GOOGLE_PLAY_BUILD` only
- **New dangerous permissions** — Add to flavor-specific manifest, not `main/`
- **Third-party analytics/tracking** — Ensure disclosed in `AMAZON_PRIVACY_DISCLOSURE.md`

## Google Play Store Restrictions

The following features are **prohibited** by Google Play policies and are removed/gated in the `googleplay` flavor:

### 1. Self-Updating / OTA Updates

- **Policy:** Apps may not modify, replace, or update themselves outside of Google Play's update mechanism
- **Our solution:** All OTA update code is gated behind `BuildConfig.IS_GOOGLE_PLAY_BUILD`
- **What's removed:** Update checker, forced update screen, What's New dialog, background update worker, `REQUEST_INSTALL_PACKAGES` permission, FileProvider

### 2. External Monetization

- **Policy:** All digital purchases must go through Google Play Billing
- **Our solution:** The "Support VoidStream" donate button (QR code to external payment) is hidden on Google Play builds
- **Future:** Implement Google Play Billing for subscriptions when ready

### 3. Permissions

- **`REQUEST_INSTALL_PACKAGES`:** Removed from Google Play manifest (only needed for OTA APK installation)
- **`RECORD_AUDIO`:** Kept but declared optional (`android.hardware.microphone` with `required="false"`). Handled gracefully if denied — voice search button is disabled, text search always available as fallback

### 4. GPL v2 Compliance (Deferred)

- VoidStream is a fork of Jellyfin (GPL v2). Closed-source distribution may conflict with GPL v2
- **Status:** Deferred — needs legal review before Google Play submission

## Google Play Compliance Checklist

**IMPORTANT:** Run through this checklist before every Google Play Store submission.

### Pre-Submission Checks

- [ ] Build the Google Play flavor: `./gradlew assembleGoogleplayRelease`
- [ ] Install on emulator and verify:
  - [ ] App launches without crash
  - [ ] No "Check for Updates" in Settings
  - [ ] No "Beta Updates" toggle in Settings
  - [ ] No "Update Notifications" toggle in Settings
  - [ ] No "Support VoidStream" donate button in Settings
  - [ ] No forced update screen on startup
  - [ ] No `REQUEST_INSTALL_PACKAGES` in `aapt dump permissions` output
  - [ ] No FileProvider in merged manifest
- [ ] Test D-pad navigation through all screens
- [ ] Test RECORD_AUDIO denial (voice search degrades to text-only)
- [ ] Verify app icon meets Google Play requirements
- [ ] Prepare feature graphic (1024x500) and screenshots
- [ ] Verify content rating is accurate
- [ ] Data Safety form is up to date

### Verifying the Google Play APK

```bash
# Check permissions (REQUEST_INSTALL_PACKAGES should NOT appear)
aapt dump permissions app/build/outputs/apk/googleplay/release/voidstream-androidtv-v*.apk

# Check for FileProvider (should NOT appear)
aapt dump xmltree app/build/outputs/apk/googleplay/release/voidstream-androidtv-v*.apk AndroidManifest.xml | grep FileProvider

# Install and test
adb install -r app/build/outputs/apk/googleplay/release/voidstream-androidtv-v*.apk
```

### Building AAB for Store Submission

Google Play requires Android App Bundle (AAB) format:

```bash
./gradlew bundleGoogleplayRelease
# Output: app/build/outputs/bundle/googleplayRelease/voidstream-androidtv-v{VERSION}-googleplay-release.aab
```

## Privacy Disclosure

- **File:** `AMAZON_PRIVACY_DISCLOSURE.md` (root of repo)
- **Purpose:** Documents all data collection, transmission, and storage for Amazon Appstore submission
- **IMPORTANT:** Update this file whenever data collection or third-party integrations change
- **What to update:** Add any new permissions, network calls, third-party services, or local storage

## Licensing

- **License:** Proprietary (see `LICENSE`)
- **Branding:** "VoidStream" is proprietary — protected names, logos, and visual assets
- **Upstream:** Contains components from Jellyfin (GPL v2) — those components remain under GPL v2
- **Distribution:** Closed-source; users do not receive access to the code
- **Business model:** Monthly subscription or one-time purchase for Fire Stick users

## Project Info

- **Platform:** Android TV / Fire TV (Kotlin, Gradle)
- **Base:** Forked Jellyfin Android TV client
- **Application ID:** `org.voidstream.androidtv`
- **Namespace:** `org.jellyfin.androidtv` (preserved for Jellyfin SDK compatibility)
- **Goal:** Integrate with a custom IPTV plugin to be created
- **Brand:** Rebranded from "Moonfin" to "VoidStream"
- **OTA Updates:** Configured via GitHub Releases API (`ToastyToast25/VoidStream-FireTV`)
- **Logos:** Source files in `LOGOS/`, backups of old branding in `BACKUP-LOGOS/`
