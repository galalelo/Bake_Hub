# AI Use Statement — BakeHub

## Summary

BakeHub was developed by **[student/team name]** with the help of an AI assistant (Claude, by Anthropic)
used as a coding and documentation aid. **The app itself contains no AI features**: nothing in the running
app calls an AI model, and no user data is sent to an AI service.

## Where AI was used

| Area | How AI helped | What the author did |
|---|---|---|
| Code | Drafted and refactored Kotlin/XML, Firestore rules, Cloud Functions and Gradle configuration; suggested library choices (Credential Manager, FCM, Retrofit) | Reviewed the code, ran/changed it in Android Studio, chose the features and design |
| Features added with AI support | Google single sign-on, offline mode, push notifications, multi-language support, account deletion, recipe reporting, release configuration | Set the requirements, [tested on device/emulator], configured the Firebase project and keys |
| Translations | Drafted Spanish, French and Arabic UI strings | [Have a fluent speaker review before publishing] |
| Tests and CI | Wrote unit tests, security-rules tests and GitHub Actions workflows | [Ran them in CI and fixed failures] |
| Documentation | Drafted the README, privacy policy and release checklist | Checked them against the real app and edited them |

## How AI output was verified

- The JVM unit tests and Cloud Functions tests were executed; the translation test checks that every language
  contains every string with matching placeholders.
- Firestore rules are covered by emulator tests that run in CI.
- AI-generated code was **not** treated as correct by default: it was built, run and manually tested on [device/emulator].
- Security-sensitive decisions (API keys server-side, security rules, account deletion) were reviewed by the author.

## Limits and risks

- AI-written code and translations can contain mistakes; reviewing them is the author's responsibility.
- Third-party recipe content (TheMealDB) is not AI-generated and keeps its source attribution.
- No personal data was shared with the AI assistant beyond the project source code.
  The `google-services.json` in the repository holds public client identifiers only, not secrets.

## Ownership and honesty

The project idea, design, feature decisions, testing and submission are the author's own work. AI assistance is
disclosed here in line with [course/institution] guidelines on AI use.

> Items in **[brackets]** need your own details before submitting. Edit them so this statement is accurate.
