# BakeHub Privacy Policy

_Last updated: 4 November 2026_

BakeHub ("the app") is a baking and recipe-sharing app. This policy explains what data the app handles.

## Data we collect

| Data | Why | Where it is stored |
|---|---|---|
| Email address, display name, password (hashed by Firebase) or Google account ID | Create and secure your account | Firebase Authentication |
| Profile (level, XP, unit preference, saved recipe IDs) | Personalise the app | Cloud Firestore, `users/{your id}` |
| Grocery list items | Sync your list across devices | Cloud Firestore |
| Recipes you publish (title, ingredients, steps, display name) | Share with the community — these are **visible to other signed-in users** | Cloud Firestore |
| Reports you file about recipes | Moderation | Cloud Firestore (not readable by users) |
| Push-notification token | Send you new-recipe notifications (can be turned off in Settings) | Firebase Cloud Messaging / Firestore |
| Language, notification choice | Remember your settings | On your device only |

We do **not** collect location, contacts, photos, advertising IDs or payment information, and we show no ads.
The app does not sell data.

## Third parties

Firebase (Google) processes account, database and notification data. Recipe searches are sent to TheMealDB
(and optionally Spoonacular through our server); these requests contain only your search text.
Google Sign-In is optional.

## Offline storage

Recipes and your data are cached on your device so the app works offline. Signing out or deleting the app removes it.

## Your choices and deleting your data

- Turn notifications off in **Settings**.
- **Delete your account** in **Settings → Delete account**. This permanently deletes your profile, grocery list,
  saved recipes and the recipes you published, and removes your sign-in.
- You can also request deletion by contacting us at **[your contact email]**.

## Children

BakeHub is not directed at children under 13 and we do not knowingly collect their data.

## Security

Data is sent over HTTPS and protected by Firebase security rules so users can only access their own data.

## Changes and contact

We may update this policy; the date above will change. Questions: **[your contact email]**.
