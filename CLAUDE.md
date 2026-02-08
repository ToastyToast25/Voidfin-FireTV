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

### Architecture Detection (How GitHub APK Works)

The **GitHub APK** includes all 4 architectures (ARM 32/64, x86 32/64) but Android **automatically detects** the device's architecture during installation and extracts only the matching libraries:

**Universal APK Contents:**

- `lib/armeabi-v7a/` — 32-bit ARM libraries
- `lib/arm64-v8a/` — 64-bit ARM libraries
- `lib/x86/` — 32-bit Intel libraries
- `lib/x86_64/` — 64-bit Intel libraries

**Automatic Installation:**

| Device Type | Installed Libraries | Example Devices                  |
|-------------|---------------------|----------------------------------|
| ARM 32-bit  | Only `armeabi-v7a`  | Older Fire TVs, older phones     |
| ARM 64-bit  | Only `arm64-v8a`    | Newer Android TVs, most phones   |
| x86 32-bit  | Only `x86`          | Older Android emulators          |
| x86_64      | Only `x86_64`       | Newer emulators, Chromebooks     |

**Result:** Even though the GitHub APK is 45 MB, the **installed app size** is similar to the platform-specific builds because only one architecture's libraries are extracted. This is handled automatically by Android's Package Manager — no user action required.

**Why platform-specific builds?**

- **Faster downloads**: Smaller APK = faster download for store users
- **Store compliance**: Amazon requires 32-bit only, Google Play requires 64-bit support
- **Optimization**: Each store gets exactly what it needs, nothing more

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
5. **CRITICAL - Conditional DI Injection:**

   **Problem:** If a service is conditionally registered (e.g., only for github flavor), using `by inject()` will crash on other flavors when the property is accessed.

   **Wrong Pattern (CRASHES on amazon/googleplay):**
   ```kotlin
   private val updateCheckerService: UpdateCheckerService by inject()
   ```

   **Correct Pattern (Safe for all flavors):**
   ```kotlin
   import org.koin.android.ext.android.get  // Add this import

   private val updateCheckerService: UpdateCheckerService? by lazy {
       if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
           get<UpdateCheckerService>()
       } else {
           null
       }
   }
   ```

   Then use safe-call operators: `updateCheckerService?.someMethod()`

   **Why:** Koin's `by inject()` is lazy but crashes when resolved if the bean isn't registered. Using `by lazy { if (condition) get<T>() else null }` makes injection truly conditional.

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

GitHub Actions workflows are in `.github/workflows/`. All workflows use the composite action `.github/actions/setup-android` for consistent Android build setup with optimized caching.

### Core Build Workflows

#### App Build (`app-build.yaml`)
- **Triggers:** Push to `master`/`release-*` and PRs (skips docs/assets changes)
- **What it does:** Builds debug APKs with optimization flags, uploads artifacts
- **Optimizations:** `--parallel --build-cache --configuration-cache`, 4GB heap
- **Retention:** 14 days for build artifacts
- **Concurrency:** Cancels outdated runs when new commits pushed

#### Store Compliance (`store-compliance.yaml`)
- **Triggers:** Push to `master`/`release-*` and PRs (skips docs/assets changes)
- **What it does:** Builds Amazon and Google Play debug APKs, runs automated compliance checks
- **Matrix strategy:** Single job tests both flavors (reduced from 124 to 73 lines)
- **Compliance checks:**
  - No `REQUEST_INSTALL_PACKAGES` permission (prohibited by both stores)
  - No `FileProvider` in manifest (not needed for store builds)
  - BuildConfig flags present (`IS_AMAZON_BUILD` / `IS_GOOGLE_PLAY_BUILD`)
  - No OTA update UI strings
- **Badge status:** Green (passing) = compliant and ready for store submission
- **Note:** Debug builds avoid requiring signing keys in CI. Gating is flavor-based, not build-type-based.
- **Concurrency:** Cancels outdated runs

### Code Quality & Testing

#### Code Quality (`code-quality.yaml`)
- **Triggers:** Push to `master`/`release-*` and PRs (skips docs/assets changes)
- **Jobs:**
  - **Detekt**: Kotlin static analysis, uploads SARIF report to GitHub Security tab
  - **Android Lint**: Android-specific linting (performance, security, accessibility), uploads HTML/XML reports
- **Benefits:** Catch code quality issues early, inline PR annotations, automated style enforcement
- **Concurrency:** Cancels outdated runs

#### Test (`test.yaml`)
- **Triggers:** Push to `master`/`release-*` and PRs (skips docs/assets changes)
- **What it does:** Runs `testDebugUnitTest`, uploads test reports on failure
- **Benefits:** Prevent regressions, foundation for expanding test coverage
- **Concurrency:** Cancels outdated runs

### Release Automation

#### Release (`release.yaml`)
- **Triggers:** Push to `master` when `gradle.properties` changes
- **What it does:** Enterprise-grade automated release pipeline with parallel builds, compliance validation, and checksums
- **Architecture:**
  - **Matrix builds**: All 3 flavors (github, amazon, googleplay) build in parallel for speed
  - **Store compliance**: Automated validation that Amazon/Google Play builds meet store policies
  - **Checksums**: SHA-256 hashes generated for all APKs/AABs for security verification
  - **AAB support**: Google Play flavor builds both APK (testing) and AAB (store submission)
- **Pipeline Steps:**
  1. **Check version bump**: Compares current vs previous `gradle.properties`
  2. **Build matrix (parallel):**
     - Decode signing keystore from GitHub Secrets
     - Run unit tests for each flavor
     - Build signed APKs (all flavors) and AAB (Google Play only)
     - Verify APK signatures with jarsigner
     - Generate SHA-256 checksums
     - Upload artifacts (90-day retention)
  3. **Validate compliance (parallel):**
     - **Amazon**: Check for prohibited `REQUEST_INSTALL_PACKAGES` permission and `FileProvider`
     - **Google Play**: Check for prohibited permissions and components
  4. **Create release:**
     - Generate changelog from git commits (excludes badge updates and `[skip ci]`)
     - Detect pre-release versions (`-alpha`, `-beta`, `-rc`)
     - Create GitHub release with all APKs, AABs, and checksums
     - Add formatted release notes with download instructions
- **Required GitHub Secrets:**
  - `RELEASE_KEYSTORE_BASE64` - Base64-encoded release.keystore (see setup below)
  - `KEYSTORE_PASSWORD` - Frostbite2531!
  - `KEY_ALIAS` - voidstream
  - `KEY_PASSWORD` - Frostbite2531!hrm
- **Usage:** Bump version in `gradle.properties`, commit, push → release auto-created in ~15 minutes
- **Benefits:**
  - Zero manual build steps
  - Store-ready APKs validated automatically
  - Parallel builds are 3x faster than sequential
  - Security checksums prevent tampering
  - Test results saved on failure

##### Setting Up GitHub Secrets

**One-time setup** to enable automated releases:

```bash
# 1. Navigate to repo directory
cd /path/to/VoidStream-FireTV

# 2. Encode keystore to base64 (single line, no wrapping)
base64 -w 0 release.keystore > keystore.base64
# On macOS: base64 -i release.keystore -o keystore.base64

# 3. Copy the base64 string
cat keystore.base64
# Select all and copy to clipboard

# 4. Add secrets to GitHub
# Go to: Settings → Secrets and variables → Actions → New repository secret

# Add these 4 secrets:
# - Name: RELEASE_KEYSTORE_BASE64
#   Value: [paste base64 string from step 3]
#
# - Name: KEYSTORE_PASSWORD
#   Value: Frostbite2531!
#
# - Name: KEY_ALIAS
#   Value: voidstream
#
# - Name: KEY_PASSWORD
#   Value: Frostbite2531!hrm

# 5. Clean up the base64 file (contains sensitive data)
rm keystore.base64
```

**Verify secrets are set:**
```bash
# Trigger workflow manually to test
# Go to: Actions → Release → Run workflow → Run workflow
# Or: Make a dummy version bump to trigger automatically
```

**Security notes:**
- Never commit `keystore.properties` or `release.keystore` to git (already in `.gitignore`)
- Base64 encoding is not encryption - secrets are only protected by GitHub's access controls
- Rotate keystore passwords if secrets are ever exposed

### Security & Compliance

#### Security (`security.yaml`)
- **Triggers:** Push, PRs, weekly schedule (Sunday midnight UTC)
- **Jobs:**
  - **Dependency Review** (PRs only): Fails on moderate+ severity vulnerabilities
  - **SBOM Generation**: Creates dependency tree for all modules (90-day retention)
  - **Secret Scanning**: Gitleaks scans entire git history for hardcoded secrets
  - **CodeQL Analysis**: Semantic code analysis for Java/Kotlin (SQL injection, XSS, etc.)
- **Benefits:** Catch vulnerabilities early, supply chain transparency, prevent credential leaks

### Developer Experience

#### PR Automation (`pr-automation.yaml`)
- **Triggers:** PR open/update (size labels), daily schedule (stale cleanup)
- **Jobs:**
  - **PR Size Label**: Auto-labels PRs by lines changed (xs/s/m/l/xl)
  - **Stale PR Management**: Marks inactive PRs (30 days), auto-closes (7 days after stale)
- **Benefits:** Visual PR complexity indicator, automatic PR hygiene

#### Pre-commit Hooks (`.pre-commit-config.yaml`)
- **Local validation** before commits to catch issues early
- **Hooks:** Detekt, ktlint format, trailing whitespace, YAML syntax, large files, line endings
- **Setup:** `pip install pre-commit && pre-commit install`
- **Benefits:** Catch issues before pushing, reduce CI failures

### Automation & Maintenance

#### Update README Badges (`update-badges.yaml`)
- **Triggers:** Push to `master` when `libs.versions.toml` or `gradle-wrapper.properties` change
- **What it does:** Parses version numbers, updates shields.io badge URLs in `README.md`
- **Versions tracked:** Android SDK, Kotlin, Java, Gradle, Jellyfin SDK, Media3, Compose
- **Commit:** `Update README version badges [skip ci]` (prevents infinite loop)

#### Dependabot (`.github/dependabot.yml`)
- **Schedule:** Weekly on Monday mornings
- **Gradle dependencies:** Grouped updates for AndroidX, Kotlin, Jellyfin SDK (max 10 PRs)
- **GitHub Actions:** Weekly updates for workflow actions (max 5 PRs)
- **Benefits:** Automated security fixes, keep dependencies up-to-date, grouped PRs reduce noise

### Performance Optimizations

All workflows include:
- **Path filters**: Skip builds for doc/asset-only changes (saves 30-40% CI minutes)
- **Concurrency controls**: Cancel outdated runs on new commits (saves 15-25% CI minutes)
- **Gradle optimization**: `--parallel --build-cache --configuration-cache` + 4GB heap (20-30% faster builds)
- **Composite action**: `.github/actions/setup-android` for DRY build setup
- **Build scans**: Gradle Develocity integration for performance insights (CI-only)

### CI Efficiency Summary

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Build time | ~6 min | ~3-4 min | 33-50% faster |
| CI minutes/month | 800 | 300-400 | 50-62% reduction |
| Code duplication | High | Minimal | Matrix + composite action |
| Quality gates | 0 | 5+ | Detekt, Lint, Tests, Security |
| Release process | Manual | Automated | 100% time savings |

### When Bumping Versions
- Badge workflow runs **automatically** — no manual README edits needed
- Release workflow runs **automatically** — just bump version and push
- The Release badge (`img.shields.io/github/release/...`) is dynamic
- If adding a new badge, add corresponding `sed` replacement in `update-badges.yaml`

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

### 4. Architecture (Amazon)

- **Policy:** Amazon Fire TV devices are 32-bit ARM only
- **Our solution:** Amazon flavor builds only include `armeabi-v7a` ABI (32-bit ARM)
- **Benefit:** Smaller APK size (~30-40% reduction), optimized for Fire TV hardware
- **Configuration:** Set in `app/build.gradle.kts` via `ndk.abiFilters`

### 5. GPL v2 Compliance (Amazon - Deferred)

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

### 4. Architecture (Google Play)

- **Policy:** Google Play requires 64-bit support starting August 1, 2026 ([TV app requirements](https://developer.android.com/training/tv/publishing/distribute))
- **Additional:** Apps must support 16 KB page sizes starting August 1, 2026
- **Our solution:** Google Play flavor includes both `arm64-v8a` (64-bit, required) and `armeabi-v7a` (32-bit, for compatibility)
- **Configuration:** Set in `app/build.gradle.kts` via `ndk.abiFilters`
- **16 KB page sizes:** Android apps that follow best practices should already be compatible. Test on devices with 16 KB page sizes to verify.

### 5. GPL v2 Compliance (Google Play - Deferred)

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

- **License:** GPL v2 (Open Source - see `LICENSE`)
- **Core Client:** Free and open source under GPL v2
- **Plugins:** Separate closed-source modules (not GPL-covered, distributed separately)
- **Branding:** "VoidStream" trademark, logos, and visual assets remain proprietary
- **Distribution:** Open source on GitHub, available on app stores for convenience
- **Source Access:** Available to all users who receive the application
- **Business model:**
  - Free base client with core Jellyfin features
  - Paid premium plugins (IPTV, advanced features, cloud sync)
  - App store convenience (paid downloads for easy access)

## Project Info

- **Platform:** Android TV / Fire TV (Kotlin, Gradle)
- **Base:** Forked Jellyfin Android TV client
- **Application ID:** `org.voidstream.androidtv`
- **Namespace:** `org.jellyfin.androidtv` (preserved for Jellyfin SDK compatibility)
- **Goal:** Integrate with a custom IPTV plugin to be created
- **Brand:** Rebranded from "Moonfin" to "VoidStream"
- **OTA Updates:** Configured via GitHub Releases API (`ToastyToast25/VoidStream-FireTV`)
- **Logos:** Source files in `LOGOS/`, backups of old branding in `BACKUP-LOGOS/`
