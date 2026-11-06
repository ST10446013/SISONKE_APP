[README_UPDATED.md](https://github.com/user-attachments/files/34467546/README_UPDATED.md)
# Sisonke – Android Community Safety App

Sisonke is an Android application for reporting community incidents, viewing submitted reports, receiving community alerts and managing a user profile. The Android client is written in Kotlin and uses Firebase services for authentication, cloud data and messaging.

> **Project status:** This README describes the supplied source project and the setup needed to run its cloud-backed features. A successful Gradle build and physical-device test have not yet been verified in this environment. Complete the configuration and testing steps below before submitting or publishing the app.

## What's new in this functional update

This section summarises the features added or improved in the updated Sisonke project.

### 1. Dedicated incident reports screen
- Added an **Incident Reports** screen (`IncidentReportsActivity`) with its own layout.
- Reads report documents from the Cloud Firestore `incidents` collection.
- Displays the report reference, category, description, submission date, reporter UID, location (when coordinates are available) and whether a photo is attached.
- Uses a Firestore snapshot listener so the list can refresh when incident records are added or changed.
- Includes an empty state when no incident reports are available.

### 2. Live dashboard incident statistics
- Replaced the fixed/sample incident total with a count derived from the Firestore `incidents` collection.
- Removed fabricated community-feed cards and example municipal ticket entries from the updated screens.
- Dashboard totals depend on records available from Firestore rather than manually entered demonstration numbers.

### 3. Google Sign-In
- Added Google sign-in using Google Sign-In to obtain an ID token and Firebase Authentication to authenticate the user.
- Authentication errors are surfaced instead of treating a button tap as a successful login.
- **Setup required:** enable Google in Firebase Authentication, configure the OAuth client, register the Android package name and SHA-1/SHA-256 signing fingerprints, then download a refreshed `google-services.json`. Source code alone cannot activate Google OAuth.

### 4. Expanded language support
- Added/updated Android string resources for **English, Afrikaans and isiZulu**.
- The language setting applies the selected app locale and saves the preference.
- Supplied translations cover app-owned interface text including screen labels, login/registration validation, incident and municipal category choices, reporting messages, weather descriptions and notification titles.
- The app subscribes to a language-specific FCM topic: `community-alerts-en`, `community-alerts-af` or `community-alerts-zu`.
- **Known limitation:** user-written incident descriptions and arbitrary text returned by external services remain in their original language. Dynamic translation of that content requires a separately selected and configured translation service, which is not included. Review every screen and dialog during acceptance testing to identify any remaining untranslated strings.

### 5. Real-time community push notifications
- Added/updated the Firebase messaging service to receive FCM messages and display Android notifications.
- Added a notification channel for community safety alerts and a tap action that opens the dashboard.
- Added `functions/index.js`, a Firebase Cloud Function triggered when a new document is created in `incidents`.
- The function sends notifications to the English, Afrikaans and isiZulu community-alert topics, with a localized notification title.
- **Deployment required:** enable Firebase Cloud Messaging, deploy the Cloud Function, ensure devices subscribe to the matching topic, and grant notification permission (required on Android 13+).

### 6. Firestore-backed reporting and offline synchronisation
- Incident submissions are saved to `incidents` with category, description, reporter UID, timestamp and optional location/photo URL.
- Municipal reports and SOS alerts are stored in their respective Firestore collections.
- Firestore offline persistence supports cached reads and queued writes that synchronise when connectivity returns. This does not make every feature available offline: cloud authentication, push delivery, Maps, REST calls and image uploads still require connectivity.

### 7. Optional incident photo storage
- Added an upload integration for Supabase Storage using the `incident-images` bucket.
- Live uploads require a Supabase project, bucket, authenticated upload policies and local project configuration.

### Before relying on these features
The source code and cloud services still require project-specific setup and end-to-end testing. Configure Google OAuth, deploy the notification function, review and publish Firestore rules, and configure Supabase if photo uploads are required. A successful Gradle build and physical-device test have not been verified in this environment. See the setup and testing sections below.

---

## 1. Main technologies

- Kotlin and Android SDK
- Android Gradle Plugin 9.1.0; Java 17
- Firebase Authentication (email/password and Google)
- Cloud Firestore for incident and user data
- Firebase Cloud Messaging (FCM) and Firebase Cloud Functions for push notifications
- Supabase Storage for optional incident image uploads
- Google Maps and Google Play Services location libraries
- Retrofit for REST API integration points

## 2. Features in the source project

- User registration and login with Firebase Authentication.
- Google Sign-In, subject to Firebase and Google OAuth configuration.
- User settings and language selection, with Android resources for English, isiZulu and Afrikaans.
- Incident report submission to the Firestore `incidents` collection.
- An incident reports screen that reads the incident collection and listens for Firestore updates.
- Dashboard incident totals loaded from Firestore rather than a fixed sample count.
- Optional incident image upload through Supabase Storage.
- Municipal reports and SOS alert records in Firestore.
- FCM client service and a Cloud Function intended to send a notification when a new incident is created.
- Firestore offline persistence and queued writes, which synchronise when connectivity returns.

User-written incident descriptions and arbitrary text returned by external services are not automatically translated. Full translation of user-generated content requires a translation service and configured credentials.

## 3. New features and functional improvements in this update

The functional update expands the original Sisonke app with the following features and removes several pieces of demonstration-only content:

### Authentication and account preferences

- **Google Sign-In:** users can authenticate with a Google account through Google Sign-In and Firebase Authentication. This requires Google as an enabled Firebase sign-in provider, the correct OAuth configuration and registered SHA fingerprints.
- **Account registration and login:** email/password authentication remains available through Firebase Authentication. Passwords are handled by Firebase Authentication rather than stored as plain text by the app.
- **Persistent language preference:** the selected app language is saved and applied as the app locale. English, isiZulu and Afrikaans resources are included for supported interface text.

### Incident reporting and live incident list

- **Dedicated Incident Reports screen:** reports are read from the Firestore `incidents` collection and displayed with their available reference, category, description, date, reporter identifier, location and photo-attached status.
- **Live report updates:** the reports screen listens for Firestore changes, so newly submitted reports can appear without manually refreshing the screen.
- **Database-backed dashboard count:** the incident total is calculated from Firestore records instead of a hardcoded sample number.
- **Removal of fabricated content:** static sample incident counts, sample community-feed cards and the example municipal ticket list have been removed in favour of database-backed information or an appropriate empty state.
- **Municipal and SOS records:** municipal reports and SOS alerts are stored in their respective Firestore collections.

### Notifications, storage and offline support

- **Push-notification workflow:** the Android app includes an Firebase Cloud Messaging service, and a Firebase Cloud Function is included to send a community alert when a new incident document is created. The function must be deployed and notification permission granted before live push delivery can be tested.
- **Language-aware notification content:** localized notification titles are provided for English, Afrikaans and isiZulu.
- **Incident photo uploads:** optional incident images can be uploaded to the Supabase Storage bucket named `incident-images`, once the project URL, publishable key, bucket and access policies are configured.
- **Offline Firestore support:** supported Firestore reads use the local cache and pending writes can synchronise after the device reconnects. This is not full offline access to authentication, maps, push notifications or image uploads.

### Important scope notes

- Interface translations cover the app-owned text for which localized Android resources are supplied. User-written incident descriptions and arbitrary text returned by external services are not automatically translated; that requires a separately configured translation service.
- Google Sign-In, push notifications and Supabase uploads depend on external Firebase, Google and Supabase configuration. Their presence in the source code does not mean the cloud services are already deployed or configured.
- Retrofit API integration points are present, but the supplied project does not include a separately hosted REST API implementation. Firestore is the active incident data source in this project.

## 4. Requirements

Install the following before opening the project:

- Android Studio with support for Android Gradle Plugin 9.1.0.
- JDK 17.
- Android SDK Platform 37.1 (as requested by the app's compile SDK configuration) and the required build tools.
- An Android phone for final testing; enable Developer options and USB debugging, or use Android Studio's device deployment workflow.
- Internet access for Gradle dependency downloads and Firebase services.
- Node.js 20 and Firebase CLI if deploying the notification function.

## 5. Open and run the Android app

1. Extract the ZIP archive.
2. In Android Studio, choose **Open** and select the `SISONKE_APP` folder (the folder containing `settings.gradle.kts`).
3. Allow Android Studio to sync the Gradle project and install any missing SDK components.
4. Complete the Firebase, Google Sign-In, Maps and optional Supabase configuration described below.
5. Connect an Android phone with USB debugging enabled, or select a configured device.
6. Select the `app` run configuration and press **Run**.
7. Resolve any SDK or dependency sync errors shown by Android Studio before testing the app.

From the project root, a command-line build can be attempted with:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The debug APK, if the build succeeds, is normally written to `app/build/outputs/apk/debug/app-debug.apk`.

## 6. Firebase setup

The project is configured around Firebase project ID `sisonke-8e84a` and Android application ID `com.example.sisonke_app`. Confirm that you have access to this Firebase project before proceeding.

### 5.1 Register the Android app

1. Open the Firebase Console and select the correct project.
2. In Project settings, confirm that an Android app exists with package name `com.example.sisonke_app`.
3. Download the current `google-services.json` for that Android app.
4. Replace `app/google-services.json` with the downloaded file. Do not use a configuration file belonging to a different Firebase project or Android package.

### 5.2 Enable authentication providers

1. In Firebase Console, open **Authentication → Sign-in method**.
2. Enable **Email/Password**.
3. Enable **Google** and complete any required project support-email or OAuth consent configuration.
4. In Project settings, add the SHA-1 and SHA-256 fingerprints for the debug and release signing certificates.
5. To view the fingerprints, run:

```bash
./gradlew signingReport
```

6. Download a fresh `google-services.json` after updating OAuth settings and replace the app copy.
7. Test Google Sign-In on a physical device using the matching package name and signing certificate.

Google Sign-In will not work until the Firebase provider, OAuth client and signing fingerprints are correctly configured. Never commit private signing keys or passwords to source control.

### 5.3 Firestore database and rules

1. Enable Cloud Firestore in Firebase Console.
2. Review `firestore.rules` against your intended user roles, moderation process and data privacy requirements.
3. Publish the rules only after confirming they are appropriate for your deployment.
4. The app uses these collections:
   - `users`
   - `incidents`
   - `municipal_reports`
   - `sos_alerts`

Do not deploy permissive test rules to production. Test both allowed and denied access using accounts with different roles.

## 7. Real-time push notifications

The Android app contains an FCM messaging service. The project also includes a Cloud Function in `functions/index.js` that is intended to send a push notification when a document is created in `incidents`.

To deploy the function:

1. Install Node.js 20 and the Firebase CLI.
2. Sign in to the Firebase CLI with an account that has deployment permission for the Firebase project:

```bash
firebase login
```

3. Confirm the selected project:

```bash
firebase use sisonke-8e84a
```

4. Install the function dependencies:

```bash
cd functions
npm install
cd ..
```

5. Deploy the function:

```bash
firebase deploy --only functions:notifyCommunityOfIncident
```

Cloud Functions deployment may require the Firebase project to use the Blaze billing plan. Check the function logs in Firebase Console after deployment. On Android 13 and later, the user must grant notification permission. Also verify that the client topic subscription and the topic targeted by the Cloud Function match before relying on delivery; see `README_FUNCTIONALITY.md` and `functions/README.md` for implementation notes.

## 8. Supabase Storage setup (optional incident photos)

If incident photo uploads are required:

1. Create a Supabase project.
2. Create a Storage bucket named `incident-images`.
3. Configure authenticated upload policies for user-scoped file paths.
4. Add the following properties to `local.properties` in the project root:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_PUBLISHABLE_KEY
```

5. Sync and rebuild the Android project.

Do not put a Supabase service-role key in the Android app. The current implementation sends the Firebase ID token as a bearer token; configure Supabase to trust Firebase as a third-party JWT provider and ensure the Storage policies validate that identity. The current image URL format assumes a public bucket; use signed URLs if the bucket is private.

## 9. Google Maps configuration

Enable **Maps SDK for Android** in the Google Cloud project associated with the app. Configure an API key restricted to the Android package name and signing certificate. Do not publish an unrestricted key. Rotate any key that has been exposed publicly.

## 10. Data and offline behaviour

Firestore offline persistence allows supported reads and writes to use the local cache while the device is offline. Pending writes are synchronised when connectivity returns. This does not mean every screen or every external service works offline. Authentication, push delivery, Maps, REST endpoints and cloud image uploads require connectivity. Test conflict handling and stale cached data before release.

## 11. Testing checklist

Before recording the demonstration video or submitting the project, test on a real Android phone:

- [ ] Register a new account and verify the account in Firebase Authentication.
- [ ] Log out and log back in with email and password.
- [ ] Complete Google Sign-In with a configured Google account.
- [ ] Change app language and inspect all screens, dialogs, validation messages and notifications.
- [ ] Submit an incident and confirm it appears in Firestore and the incident reports screen.
- [ ] Submit multiple incidents and verify that dashboard counts match the actual records.
- [ ] Verify that the reports list updates when a second device/account submits a report.
- [ ] Test optional photo uploads and verify the uploaded file and URL.
- [ ] Send a test incident and confirm push notification delivery on a second device.
- [ ] Deny and grant notification permission to verify both states are handled clearly.
- [ ] Create a report while offline, reconnect and verify synchronisation.
- [ ] Check Firestore access using accounts with different permissions.
- [ ] Test the release build and verify app icon, version details and permissions.

## 12. Google Play preparation

Before publishing:

1. Create and securely store a release signing key. Back it up; losing it can prevent future app updates.
2. Configure the release signing settings without committing passwords or key files.
3. Update `versionCode` and `versionName` in `app/build.gradle.kts` for each release.
4. Build a signed Android App Bundle (AAB) from **Build → Generate Signed Bundle / APK** in Android Studio.
5. Complete the Play Console app listing, privacy policy, Data safety form, content rating, target audience and app access instructions.
6. Test an internal Play testing release before production rollout.
7. Confirm that Firebase SHA fingerprints include the Play App Signing certificate where applicable.

Publishing requires a Google Play Console developer account and completion of Google's current review and policy requirements.

## 13. Project structure

```text
SISONKE_APP/
├── app/
│   ├── google-services.json
│   └── src/main/
│       ├── java/com/example/sisonke_app/
│       │   ├── api/             # Retrofit API setup
│       │   ├── auth/            # Registration and login
│       │   ├── dashboard/       # Main dashboard
│       │   ├── incidents/       # Incident reporting and reports list
│       │   ├── notifications/   # Firebase messaging service
│       │   ├── settings/        # User settings and language selection
│       │   └── ...              # Other app screens and utilities
│       └── res/                 # Layouts, icons, strings and themes
├── functions/                   # Firebase Cloud Function
├── firestore.rules              # Firestore security rules
├── firebase.json                # Firebase CLI configuration
├── README.md                    # This guide
└── README_FUNCTIONALITY.md      # Detailed implementation notes
```

## 14. Troubleshooting

- **Google Sign-In fails:** check that Google is enabled in Firebase Authentication, the OAuth client exists, the package name is exact, SHA-1/SHA-256 are registered and `google-services.json` has been refreshed.
- **Firestore permission denied:** inspect the deployed rules and confirm the user is authenticated and allowed to perform the requested operation.
- **No push notification arrives:** verify FCM is enabled, notification permission is granted, the Cloud Function deployed successfully, the topic names match and the function logs show a successful send.
- **Photo upload fails:** verify Supabase URL/key, bucket name, JWT trust configuration and Storage policies.
- **Gradle sync fails:** check JDK 17, Android SDK Platform 37.1, internet access and Android Studio/Gradle compatibility.
- **Some content is not translated:** Android string resources cover app-owned interface text; user-generated descriptions and arbitrary server responses need a separately configured translation service to be translated dynamically.

## 15. Additional implementation notes

See `README_FUNCTIONALITY.md` for the feature-by-feature implementation notes, cloud configuration details and known limitations. See `functions/README.md` for the notification function deployment notes.
