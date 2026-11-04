# Google Play release checklist

## Already done in this repository
- [x] `targetSdk`/`compileSdk` **36** (required for new apps and updates since 31 Aug 2026); AGP 8.9.1 / Gradle 8.11.1
- [x] Edge-to-edge insets and predictive-back-safe navigation (both enforced on API 36)
- [x] R8 minification + resource shrinking with keep rules (`app/proguard-rules.pro`)
- [x] Release signing driven by `keystore.properties` or CI secrets (never committed)
- [x] Version `1.0.0` (`versionCode 3`) — **increase `versionCode` for every upload**
- [x] Backup / data-extraction rules, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` runtime permission
- [x] In-app **account deletion** (Play policy for apps with account creation)
- [x] **User-generated-content reporting** (Play UGC policy) + write-only `reports` rules
- [x] Privacy policy (`docs/PRIVACY_POLICY.md`) linked in Settings
- [x] Translations: en, es, fr, ar

## You still need to do
1. **Signing key** – `keytool -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`.
   Copy `keystore.properties.example` to `keystore.properties` (git-ignored) or add the four `BAKEHUB_*` GitHub secrets,
   then run `./gradlew bundleRelease` (or push a `v1.0.0` tag). Enrol in **Play App Signing**.
2. **Firebase** – add the release **and** Play-signing SHA-1 fingerprints, re-download `google-services.json`
   (enables Google sign-in), enable Email + Google providers, deploy `firestore.rules` and `functions`.
3. **Privacy policy URL** – publish `docs/PRIVACY_POLICY.md` at a public URL, fill in your contact email, and confirm
   the `privacy_policy_url` string (`app/src/main/res/values/strings.xml`) points to it (it assumes the `main` branch).
4. **Play Console forms** – App content: privacy policy, **Data safety** (email, name, user IDs, user content, device
   token; encrypted in transit; deletion supported), content rating questionnaire, target audience (13+), ads: none,
   UGC declaration (reporting available).
5. **Store listing** – title, short/full description, 512×512 icon, 1024×500 feature graphic, ≥2 phone screenshots
   (add translated listings for es/fr/ar if you want them).
6. **Test first** – internal testing track on real devices (Android 8, 13, 16), including offline use, Arabic (RTL) and
   notification permission. New personal developer accounts must run a closed test with 12+ testers for 14 days before production.
7. **Moderation** – decide who reviews the `reports` collection in the Firebase console.
8. **Tighten rules (recommended)** – add `authorUid` to recipes and check it in `firestore.rules`.
9. Have a fluent speaker review the es/fr/ar translations.
