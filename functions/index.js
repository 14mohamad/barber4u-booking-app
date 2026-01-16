const {onDocumentUpdated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

const COMPLIMENTS = [
  "Fresh cut! You’re looking sharp 🔥",
  "That haircut suits you perfectly 😄",
  "Clean look! Great choice ✨",
  "Sharp style — looks amazing 💯",
  "Nice! That fade is on point 😎",
];

exports.notifyAppointmentDone = onDocumentUpdated(
    "appointments/{id}",
    async (event) => {
      if (!event.data) {
        return;
      }

      const beforeSnap = event.data.before;
      const afterSnap = event.data.after;

      if (!beforeSnap || !afterSnap) {
        return;
      }

      const before = beforeSnap.data();
      const after = afterSnap.data();

      if (!before || !after) {
        return;
      }

      if (before.status === after.status) {
        return;
      }

      if (after.status !== "DONE") {
        return;
      }

      const appointmentId = event.params.id;
      const userId = after.userId;

      if (!userId) {
        return;
      }

      const barberName = after.barberName || "your barber";
      const branchName = after.branchName || "";

      const compliment =
      COMPLIMENTS[Math.floor(Math.random() * COMPLIMENTS.length)];

      const messageText =
      compliment +
      "\nPlease rate " +
      barberName +
      " (0–5 stars).";

      const messageDoc = {
        type: "RATE_REQUEST",
        appointmentId: appointmentId,
        barberId: after.barberId || "",
        barberName: barberName,
        branchId: after.branchId || "",
        branchName: branchName,
        text: messageText,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        seen: false,
      };

      await admin
          .firestore()
          .collection("users")
          .doc(userId)
          .collection("messages")
          .add(messageDoc);

      const userDoc = await admin
          .firestore()
          .collection("users")
          .doc(userId)
          .get();

      if (!userDoc.exists) {
        return;
      }

      const token = userDoc.data().fcmToken;

      if (!token) {
        return;
      }

      const pushBody =
      compliment +
      " Tap to rate " +
      barberName +
      ".";

      const push = {
        token: token,
        notification: {
          title: "Appointment done",
          body: pushBody,
        },
        data: {
          type: "RATE_REQUEST",
          appointmentId: appointmentId,
          barberName: barberName,
          branchName: branchName,
        },
      };

      await admin.messaging().send(push);
    },
);
