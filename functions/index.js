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

    // Only when status changes to DONE
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

    // 1) Create Firestore message under the user
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

    // 2) Optional FCM push
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
  }
);

exports.onAppointmentRated = onDocumentUpdated(
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

    // Only when rating is newly set/changed
    const beforeRating = before.rating;
    const afterRating = after.rating;

    if (beforeRating === afterRating) {
      return;
    }
    if (afterRating === null || afterRating === undefined) {
      return;
    }

    const barberId = after.barberId;
    if (!barberId) {
      return;
    }

    const ratingInt = Number(afterRating);

    if (!Number.isFinite(ratingInt)) {
      return;
    }
    if (ratingInt < 1 || ratingInt > 5) {
      return;
    }

    const barberRef = admin.firestore().collection("barbers").doc(barberId);

    await barberRef.set(
      {
        ratingSum: admin.firestore.FieldValue.increment(ratingInt),
        ratingCount: admin.firestore.FieldValue.increment(1),
      },
      {merge: true}
    );
  }
);

const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onAppointmentRated = onDocumentUpdated("appointments/{id}", async (event) => {
  if (!event.data) return;

  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before || !after) return;

  // Only when rating is newly set
  if (before.rating === after.rating) return;
  if (!after.rating) return;

  const barberId = after.barberId;
  const rating = after.rating;

  if (!barberId || typeof rating !== "number") return;

  await admin.firestore().collection("barbers").doc(barberId).update({
    ratingSum: admin.firestore.FieldValue.increment(rating),
    ratingCount: admin.firestore.FieldValue.increment(1),
  });
});