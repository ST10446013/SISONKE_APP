# Sisonke Android application – functional update

## Implemented in this project

- Email/password registration and login through Firebase Authentication.
- Google Sign-In through Google Sign-In and Firebase Auth credential exchange. The app checks that Google returns an ID token and surfaces authentication errors.
- User profile and preferences stored in Cloud Firestore.
- English, isiZulu and Afrikaans Android locale resources. The settings language selector applies the selected Android app locale and persists it.
- Incident submission to the `incidents` Firestore collection, including category, description, reporter UID, timestamp, optional coordinates and optional photo URL.
- A dedicated **Incident Reports** screen that listens to the `incidents` collection in real time and lists report reference, category, description, date, reporter UID, location and photo-attached status.
- Dashboard incident count loaded from the live Firestore collection; static sample incident counts and sample community-feed cards have been removed.
- Optional incident photos uploaded to the Supabase Storage `incident-images` bucket.
- Municipal report submission to `municipal_reports`; the fabricated example ticket list has been removed.
- SOS alert records stored in `sos_alerts`.
- Firebase Cloud Messaging language-specific topic subscription preference and Android message service.
- A Firebase Cloud Function in `functions/index.js` that sends an FCM push notification whenever a new Firestore incident is created, with a localized notification title for English, Afrikaans and isiZulu users.
- Firestore offline persistence is enabled by the Android SDK; pending Firestore writes are queued and synchronised when connectivity returns.

## Required setup before the app can run all cloud features

### 1. Firebase Authentication and Google Sign-In

1. Open Firebase Console for project `sisonke-8e84a`.
2. In Authentication → Sign-in method, enable **Email/Password** and **Google**.
3. In Project settings → Your apps → Android app, add the SHA-1 and SHA-256 fingerprints for the debug and release signing certificates.
   - Run `./gradlew signingReport` in Android Studio's terminal to obtain the fingerprints.
4. Download the updated `google-services.json` and replace `app/google-services.json`.
5. Confirm that the generated `default_web_client_id` is present (the Google Sign-In flow uses this OAuth web client ID).
6. Test Google Sign-In on a physical Android device using the same package name and signing certificate registered in Firebase.

Google OAuth cannot be fully activated solely by changing Android source code: the Firebase provider and signing-certificate fingerprints must be configured in the Firebase/Google Cloud project.

### 2. Firestore

- Enable Cloud Firestore.
- Publish `firestore.rules` after reviewing them for your deployment's moderation/admin requirements.
- The rules allow authenticated community members to read incident and municipal reports, while only the signed-in author can create a report under their own UID.
- Collections used by the app: `users`, `incidents`, `municipal_reports`, and `sos_alerts`.

### 3. Real-time push notifications

The Android app subscribes to the language-specific `community-alerts-en`, `community-alerts-af` or `community-alerts-zu` FCM topic and handles incoming messages in `SisonkeMessagingService`.

Deploy the included Cloud Function so incident creation triggers push delivery:

1. Install Node.js 20 and Firebase CLI.
2. From the project root, run `npm install` inside `functions/`.
3. Confirm the Firebase CLI is logged into the owner account for project `sisonke-8e84a`.
4. Run `firebase deploy --only functions:notifyCommunityOfIncident`.

Cloud Functions deployment may require the Firebase project to be on the Blaze billing plan. Android 13+ users must grant notification permission. A push cannot be delivered until the Cloud Function is deployed and FCM is enabled for the Firebase project.

### 4. Supabase Storage

- Create a Storage bucket named `incident-images`.
- Configure policies for authenticated uploads to user-scoped paths.
- Add these values to the project-root `local.properties` (do not commit this file):
  ```
  SUPABASE_URL=https://YOUR_PROJECT.supabase.co
  SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_PUBLISHABLE_KEY
  ```
- The current upload implementation sends the Firebase ID token as the bearer token. Supabase must be configured to trust Firebase as a third-party JWT provider and its Storage policies must validate that identity. Never put a Supabase service-role key in the Android app.
- The current image URL uses the public-bucket URL format; use signed URLs if the bucket is private.

### 5. Google Maps and REST API

- Enable Maps SDK for Android and restrict the Maps API key to this package and signing certificate. Rotate the key if it has been exposed publicly.
- Retrofit API definitions exist, but this ZIP does not contain a separately hosted REST API implementation. Firestore is the active incident database. `10.0.2.2` is emulator-only and must be replaced with the deployed API URL if the rubric requires a separate REST API.

## Verification status and remaining work

- XML resources were parsed for well-formedness.
- I could not run a Gradle compile in this environment: the Gradle wrapper attempted to download Gradle, but network access was unavailable. Therefore this ZIP is not represented as a successfully compiled or device-tested APK.
- Build in Android Studio with internet access and the required Android SDK installed, then test on a physical Android phone.
- Test Google Sign-In only after completing Firebase OAuth setup, push delivery after deploying the Cloud Function, Firestore access using the published rules, photo uploads using your configured Supabase project, and locale coverage across every screen.
- The visible Android layout labels, login/registration validation, incident and municipal category choices, weather descriptions, incident-reporting messages and push notification titles have English, Afrikaans and isiZulu resources. User-authored report descriptions and messages returned verbatim by external services remain in their original language; translating arbitrary user-generated content requires a translation service and credentials, which are not included in this project.
