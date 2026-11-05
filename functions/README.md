# Sisonke real-time incident notifications

This Firebase Cloud Function sends an FCM push notification whenever a new
document is created in `incidents/{incidentId}`. Android clients subscribe to
the `community-alerts` topic after login.

## Deploy

1. Install Node.js 20 and Firebase CLI.
2. From the repository root, initialise Firebase Functions if this project
   does not already have a `firebase.json`:
   `firebase init functions` (choose JavaScript and Node 20; keep this source).
3. Ensure the Firebase project is `sisonke-8e84a`.
4. From the `functions` directory run `npm install`.
5. Deploy with `firebase deploy --only functions:notifyCommunityOfIncident`.

Cloud Functions deployment may require the Firebase project to use the Blaze
billing plan. Verify the deployed function logs in Firebase Console.

## Required Firebase configuration

- Cloud Firestore enabled.
- Firebase Cloud Messaging enabled.
- Android package name: `com.example.sisonke_app`.
- Android 13+ notification permission must be granted by the user.
- Firestore rules must allow authenticated users to create incident reports
  and read the incident collection as required by the app's user roles.

The function sends notifications to the FCM topic; it does not expose server
credentials or an FCM server key in the Android application.
