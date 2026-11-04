# BakeHub

[![CI](https://github.com/galalelo/Bake_Hub/actions/workflows/android.yml/badge.svg)](https://github.com/galalelo/Bake_Hub/actions/workflows/android.yml)

A social baking app for Android: discover and publish recipes, follow them step by step in a
hands-free **Bake Mode** with real countdown timers, and keep a synced grocery list.

- Demo video: https://youtube.com/shorts/WX5nSKwMGOk?feature=share
- Source: https://github.com/galalelo/Bake_Hub.git

Built with **Kotlin + XML views**, **Firebase** (Authentication, Cloud Firestore, Cloud Messaging,
Cloud Functions) and the free **TheMealDB** API.

## Features

| Area | What it does |
|---|---|
| **Sign-in (SSO)** | Email/password **and Google single sign-on** (Credential Manager → Firebase Auth). First-time Google users get a profile automatically. |
| **Recipes** | Browse, search (Firestore + TheMealDB), save to your Recipe Box, publish your own, report inappropriate recipes. |
| **Bake Mode** | Step-by-step guidance, per-step countdown timers, screen stays awake, notification when a timer ends. |
| **Grocery list** | Add / edit / check off / delete / share, synced per user in Firestore. |
| **Offline mode** | Firestore on-disk cache (100 MB), offline banner, writes queue and sync on reconnect, TheMealDB recipes you opened stay available offline. |
| **Real-time notifications** | Firestore listeners keep every screen live; a Cloud Function pushes an FCM notification when someone publishes a new recipe (opt-out in Settings). |
| **Languages** | English, Español, Français, العربية (RTL). Switch in **Settings → Language** or via Android 13+ *App languages*. |
| **Account control** | In-app **Delete account** (removes profile, grocery list and your published recipes) and privacy-policy link, as Google Play requires. |

## Architecture

```text
app/src/main/java/com/bakehub/app/
├─ BakeHubApp.kt            app start-up (prefs, network monitor, caches, notification channels)
├─ MainActivity.kt          screen router + XML view building
├─ data/                    AuthRepository, UserRepository, RecipeRepository (Firestore),
│                           MealMapper (TheMealDB → Recipe), OnlineRecipeCache, Models
├─ data/remote, repository/ Retrofit client for TheMealDB
├─ notifications/           FCM service, channels, topic subscription
└─ util/                    NetworkMonitor, LocaleHelper/LanguageTags, AppPreferences
functions/                  Cloud Functions: notifyNewRecipe (FCM), searchExternalRecipes (Spoonacular proxy)
firestore.rules             security rules  (+ firestore.rules.test.js)
docs/                       AI use write-up, privacy policy, release checklist
```

## Getting started

Requirements: Android Studio (Ladybug/2024.2 or newer), JDK 17, Node.js 20 (for functions/tests).

1. **Firebase project** – create one at <https://console.firebase.google.com>, add an Android app with
   package `com.bakehub.app`, and put its `google-services.json` in `app/` (replace the existing file with your own).
2. **Authentication** – enable the *Email/Password* and *Google* sign-in providers.
3. **Google SSO** – add your debug/release **SHA-1** fingerprints under *Project settings → Your apps*
   (`./gradlew signingReport`), then **re-download `google-services.json`**. The Google services plugin then
   generates `default_web_client_id`, which the app picks up automatically. Until then the
   "Continue with Google" button explains that it isn't configured.
4. **Firestore** – create the database, then publish the rules: `firebase deploy --only firestore:rules`.
5. **Cloud Functions** (notifications + external search):
   ```bash
   cd functions && npm ci && cd ..
   firebase functions:secrets:set SPOONACULAR_API_KEY      # only needed for searchExternalRecipes
   firebase deploy --only functions
   ```
   The `region` in `functions/index.js` must match your Firestore location.
6. Open the folder in Android Studio, sync, and run on a device/emulator (Android 8.0+).

## Testing

| Suite | Command | Covers |
|---|---|---|
| Android unit tests (JVM) | `./gradlew testDebugUnitTest` | TheMealDB → recipe mapping (steps, timers, time estimates), models, profile defaults, language tags, **translation completeness** (every language has every string with matching placeholders) |
| Cloud Functions | `cd functions && npm test` | notification message builder |
| Firestore rules | `npm ci && npm run test:rules` (needs Java 17) | users, grocery items, recipes, **reports** and FCM-token access rules, against the local emulator |
| Static checks | `./gradlew lintDebug` | Android lint (report uploaded by CI) |

**Continuous integration** (`.github/workflows/android.yml`) runs all of the above on every push and pull request,
plus a debug APK build and an R8-minified release bundle build. `release.yml` builds a signed `.aab` on version tags.

## Releasing to Google Play

See [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) (signing, target API 36, store listing, data safety, account deletion, UGC reporting).

## Documentation

- [`docs/AI_USE.md`](docs/AI_USE.md) – how AI was used in this project
- [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md) – privacy policy used for the store listing and in-app link

## Known limitations

- XP/level/streak/badges and the Weekly Challenge are still sample data.
- Recipe ownership in Firestore rules is matched on `authorName`; adding an `authorUid` field and checking it in
  `firestore.rules` would make this stricter.
- Recipe content from TheMealDB and user-created recipes is shown in its original language (only the app UI is translated).
- Unit conversion (Metric/Imperial) is stored as a preference but not yet applied to ingredient amounts.

## Credits and attribution

Recipe data and images from [TheMealDB](https://www.themealdb.com). Online results keep their original
source links. External search (optional) uses the Spoonacular API through a server-side function so the API key
is never shipped in the app.
