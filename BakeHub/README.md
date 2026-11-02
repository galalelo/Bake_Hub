LINKS 
YOUTUBE LINK-https://youtube.com/shorts/WX5nSKwMGOk?feature=share
GITHUB LINK-https://github.com/galalelo/Bake_Hub.git


# BakeHub — Firebase Android App (Kotlin + XML)

This is the clickable prototype referenced as **Milestone M3** in the BakeHub
project plan ("Prototype (frontend) complete — all ten screens built and
navigable with mock/static data"). It implements every screen from Section 3
of the design document, using the same terracotta/gold/cream palette as the
wireframes and app icon, wired together with Navigation Compose.

## What's implemented

- **Onboarding → Login → Home** flow
- **Home** — greeting, streak/level snapshot, weekly challenge, recommended recipes
- **Search & Filter** — live text search over Firestore recipes plus the secured external recipe search function
- **Recipe Detail** — full recipe info, Metric/Imperial toggle, Save-to-Recipe-Box
- **Bake Mode** — step-by-step guided flow with a real countdown timer and
  `FLAG_KEEP_SCREEN_ON` so the screen won't lock mid-bake, matching Section 2.1.4
- **Recipe Box** — All / Saved / My Recipes / Offline tabs
- **Grocery List** — checkable combined ingredient list
- **Create Recipe** — dynamic ingredient/step form (not yet persisted)
- **Profile & Achievements** — XP/level progress bar, streak, badges
- **Settings** — unit preference and notification toggles

All data is now backed by **Firebase** — Authentication (email/password) and
Cloud Firestore (the database) — instead of the static `MockData.kt` used in
the earlier prototype. `MockData.kt` is still in the project as a fallback
(used only if Firestore hasn't loaded yet) and as the seed data uploaded to a
fresh Firestore database the first time the app runs.

**What's wired to Firebase today:**
- Sign up / Log in / Log out (Firebase Authentication)
- Recipes: Home, Search, Recipe Box, Recipe Detail, Bake Mode all read from
  the Firestore `recipes` collection; Create Recipe publishes to it
- Saved recipes (Recipe Box → Saved tab, the star button on Recipe Detail)
- Unit preference (Settings)

**Still mock/static** (kept out of scope for this pass — straightforward to
move to Firestore the same way): XP/level/streak/badges gamification, the
Weekly Challenge card, and the Grocery List.

## Firebase setup

The app requires a Firebase project. This working copy includes `google-services.json`; replace it with your own project configuration before publishing.

1. **Create a Firebase project.** Go to
   [console.firebase.google.com](https://console.firebase.google.com) → *Add
   project* → follow the prompts (Google Analytics is optional, you can skip
   it).
2. **Register the Android app.** In your new project, click the Android icon
   ("Add app") and enter:
   - Android package name: `com.bakehub.app`
   - (Nickname/SHA-1 are optional for this prototype — SHA-1 is only needed
     later if you add Google Sign-In.)
3. **Download `google-services.json`** and place it at
   `BakeHub/app/google-services.json` — directly next to
   `app/build.gradle.kts`. (Delete the
   `google-services.json.PLACEHOLDER-README.txt` file once you've done this.)
4. **Enable Email/Password sign-in.** In the Firebase console: *Build →
   Authentication → Get started → Sign-in method → Email/Password → Enable →
   Save.*
5. **Create the Firestore database.** *Build → Firestore Database → Create
   database.* Choose a region close to your users, and start in
   **production mode** (the security rules below will lock it down properly).
6. **Publish the security rules.** Copy the contents of `firestore.rules`
   (included in this project) into *Firestore Database → Rules* in the
   console, and click *Publish*. These rules mean: only signed-in users can
   read/write recipes, and each user can only read/write their own profile
   document.
7. **Sync and run.** Back in Android Studio, click *Sync Now* when prompted
   (this is what actually applies the Google Services plugin and downloads
   the Firebase libraries), then run the app. Sign up for a new account —
   this creates your Firebase Auth user and a matching Firestore `users/{uid}`
   document, and the app will automatically upload the built-in sample
   recipes into Firestore the first time it sees an empty `recipes`
   collection.

You can watch it all happen live in the Firebase console under
*Authentication → Users* and *Firestore Database → Data*.

### Optional: run it without a phone/emulator handy
Android Studio's built-in emulator (Tools → Device Manager) works fine for
this — no physical device is required to test the Firebase wiring.

## Opening the project

1. Open this folder in **Android Studio** (Koala/2024.1 or newer recommended).
2. Android Studio will detect that `gradle-wrapper.jar` is not included in
   this download (binary jars aren't practical to hand-write) and will offer
   to regenerate it automatically — accept that prompt, or run
   `gradle wrapper` once from a terminal if you have Gradle installed locally.
3. Let Gradle sync. The project targets **compileSdk 34 / minSdk 26** and
   uses the Compose BOM `2024.09.00` with Kotlin `1.9.24` / AGP `8.5.2`.
4. Run the `app` configuration on an emulator or device (Android 8.0+).

## Known limitations / next steps

- **Not yet compiled in this environment** — this was written without access
  to the Android SDK/Gradle network mirrors, so please do a build/sync in
  Android Studio and fix any small issues Android Studio's inspections flag
  (e.g. Compose API surface changes between library patch versions).
- Bake Mode's XP/streak award on completion is still stubbed (navigates back
  to Home rather than mutating saved state) — gamification fields
  (level/xp/streak/badges) are still mock data; move them into the same
  `users/{uid}` Firestore document the rest of the profile already uses.
- The Grocery List and Weekly Challenge card are still mock/static.
- No image loading yet — all recipe photos are solid placeholder blocks. If
  you want cover photos for user-created recipes, add Firebase Storage
  (Build → Storage in the console) and upload from Create Recipe.
- The Spoonacular proxy described in Section 4 of the design document is not
  integrated — Firebase/Firestore is the only backend right now.
- Firestore security rules (see `firestore.rules`) currently let any signed-in
  user edit any recipe; add an `authorUid` field to new recipes and check it
  in the rules once you want to restrict editing to the recipe's author.

## Project structure
```text
BakeHub/
├─ app/src/main/java/com/bakehub/app/
│  ├─ MainActivity.kt
│  └─ data/                 # Firebase Auth, Firestore, models and mappers
├─ app/src/main/res/layout/ # XML screen layouts
├─ functions/               # Secure external recipe search proxy
├─ firestore.rules
└─ .github/workflows/android.yml
```
## Part 3 Firebase + XML implementation

This version uses Firebase as the source of truth. The Android UI has been migrated from Jetpack Compose to Kotlin and XML layouts while retaining Firebase Authentication and Cloud Firestore repositories.

### Firebase features implemented in the Android app

- Email/password sign-up and login through Firebase Authentication.
- Firestore-backed user profiles, saved recipes, and live recipe listeners.
- Firestore recipe seeding on a fresh project.
- XML screens for onboarding, login, sign-up, home, search, recipe box, grocery list, profile, settings, recipe detail, create recipe, and Bake Mode.
- Offline-friendly fallback to the bundled sample recipes when Firestore has not loaded yet.
- External recipe attribution fields (`sourceUrl` and `imageUrl`) in the Firestore recipe model.

### Secure live recipe search

The `functions/` directory contains a Firebase Cloud Function named `searchExternalRecipes`. It calls Spoonacular from the server so the provider API key is not placed in the Android APK. The Android app calls the function through the Firebase Functions SDK and combines online results with Firestore results.

From the Firebase project root:

```bash
cd functions
npm install
cd ..
firebase functions:secrets:set SPOONACULAR_API_KEY
firebase deploy --only functions:searchExternalRecipes,firestore:rules
```

Enable Email/Password in Firebase Authentication, create the Firestore database, and confirm that the Android package name matches the package in `google-services.json`. Online results should always retain the original source link and provider attribution. Do not copy or redistribute full third-party instructions unless the provider licence permits it.

The Cloud Function uses the Spoonacular `complexSearch` endpoint. Its quota and licensing rules apply; configure billing/quotas in the provider account before public release.
