const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * Sends a localized push alert to the topic matching each user's app language.
 * The incident description is user-authored and is intentionally preserved.
 */
exports.notifyCommunityOfIncident = onDocumentCreated(
  "incidents/{incidentId}",
  async (event) => {
    const incident = event.data?.data();
    if (!incident) return;

    const category = String(incident.category || "New incident").slice(0, 80);
    const description = String(
      incident.description || "A new incident report was submitted."
    ).slice(0, 220);

    const messages = [
      {
        topic: "community-alerts-en",
        title: `New community report: ${category}`,
        body: description
      },
      {
        topic: "community-alerts-af",
        title: `Nuwe gemeenskapsverslag: ${category}`,
        body: description
      },
      {
        topic: "community-alerts-zu",
        title: `Umbiko omusha womphakathi: ${category}`,
        body: description
      }
    ];

    await Promise.all(messages.map(({ topic, title, body }) =>
      getMessaging().send({
        topic,
        notification: { title, body },
        data: {
          type: "incident_created",
          incidentId: String(event.params.incidentId)
        },
        android: {
          priority: "high",
          notification: {
            channelId: "sisonke_community_alerts",
            sound: "default"
          }
        }
      })
    ));
  }
);
