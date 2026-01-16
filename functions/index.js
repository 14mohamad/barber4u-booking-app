const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.notifyAppointmentDone = onDocumentUpdated(
  "appointments/{id}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after) return;

    // Only when status changes to DONE
    if (before.status === after.status) return;
    if (after.status !== "DONE") return;

    const userId = after.userId;
    if (!userId) return;

    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(userId)
      .get();

    if (!userDoc.exists) return;

    const token = userDoc.data().fcmToken;
    if (!token) return;

    const msg = {
      token,
      data: {
        type: "APPOINTMENT_DONE",
        appointmentId: event.params.id,
        barberName: after.barberName || "",
        branchName: after.branchName || "",
      },
    };

    await admin.messaging().send(msg);
  }
);
