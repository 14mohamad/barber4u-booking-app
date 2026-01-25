const {
  onDocumentUpdated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");

const {logger} = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const COMPLIMENTS = [
  "Fresh cut! You’re looking sharp 🔥",
  "That haircut suits you perfectly 😄",
  "Clean look! Great choice ✨",
  "Sharp style — looks amazing 💯",
  "Nice! That fade is on point 😎",
];

// =====================================================
// 1) תור חדש -> הודעה לספר
// משתמשים ב-onDocumentWritten כי לפעמים התור נוצר
// בלי barberId/userId ורק אחרי זה מתעדכן.
// מוסיפים דגל barberNotified כדי לא לשלוח פעמיים.
// =====================================================
exports.notifyBarberNewAppointment = onDocumentWritten(
    "appointments/{id}",
    async (event) => {
      if (!event.data) return;

      const beforeSnap = event.data.before;
      const afterSnap = event.data.after;

      // אם המסמך נמחק - לא עושים כלום
      if (!afterSnap || !afterSnap.exists) return;

      const before =
      beforeSnap && beforeSnap.exists ? beforeSnap.data() : null;

      const after = afterSnap.data();
      if (!after) return;

      const appointmentId = event.params.id;

      const barberId = after.barberId;
      const customerId = after.userId;

      // אם עדיין אין מזהים - מחכים (יעבוד כשיתעדכן)
      if (!barberId || !customerId) return;

      // מניעת הודעות כפולות
      const alreadyNotifiedBefore =
      before && before.barberNotified === true;

      const alreadyNotifiedAfter =
      after.barberNotified === true;

      if (alreadyNotifiedBefore || alreadyNotifiedAfter) return;

      const customerName = after.userName || "";
      const customerEmail = after.userEmail || "";
      const branchName = after.branchName || "";
      const date = after.date || "";
      const time = after.time || "";

      const parts = [];
      parts.push("New appointment request.");
      if (customerName) parts.push(`Customer: ${customerName}`);
      if (customerEmail) parts.push(`Email: ${customerEmail}`);
      parts.push(`Date: ${date}  Time: ${time}`);
      if (branchName) parts.push(`Branch: ${branchName}`);

      const text = parts.join("\n");

      const messageDoc = {
        type: "NEW_APPOINTMENT",
        target: "APPOINTMENT",
        appointmentId,
        barberId,
        userId: customerId,
        userName: customerName,
        branchId: after.branchId || "",
        branchName,
        status: after.status || "PENDING",
        text,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        seen: false,
      };

      // 1) יוצרים הודעה אצל הספר
      await admin
          .firestore()
          .collection("users")
          .doc(barberId)
          .collection("messages")
          .add(messageDoc);

      // 2) מסמנים שהודעה כבר נשלחה לספר (כדי לא לשלוח שוב)
      await admin
          .firestore()
          .collection("appointments")
          .doc(appointmentId)
          .set({barberNotified: true}, {merge: true});

      // 3) Optional push לספר
      const barberUserDoc = await admin
          .firestore()
          .collection("users")
          .doc(barberId)
          .get();

      if (!barberUserDoc.exists) return;

      const token = barberUserDoc.data().fcmToken;
      if (!token) return;

      await admin.messaging().send({
        token,
        notification: {
          title: "New appointment",
          body: "You have a new appointment request. Tap to view.",
        },
        data: {
          type: "NEW_APPOINTMENT",
          target: "APPOINTMENT",
          appointmentId,
        },
      });
    },
);

// =====================================================
// 2) שינוי סטטוס ע"י הספר -> הודעה ללקוח
// (APPROVED / REJECTED / CANCELLED / PENDING וכו')
// לא שולחים DONE כאן כי יש פונקציה נפרדת לדירוג.
// =====================================================
exports.notifyCustomerStatusChanged = onDocumentUpdated(
    "appointments/{id}",
    async (event) => {
      if (!event.data) return;

      const before = event.data.before.data();
      const after = event.data.after.data();
      if (!before || !after) return;

      // רק אם הסטטוס השתנה
      if (before.status === after.status) return;

      const appointmentId = event.params.id;

      const customerId = after.userId;
      if (!customerId) return;

      const barberName = after.barberName || "your barber";
      const branchName = after.branchName || "";
      const newStatus = after.status || "";

      // DONE מטופל בפונקציה אחרת (דירוג)
      if (newStatus === "DONE") return;

      const parts = [];
      parts.push("Appointment update.");
      parts.push(`Status: ${newStatus}`);
      parts.push(`Barber: ${barberName}`);
      if (branchName) parts.push(`Branch: ${branchName}`);

      const text = parts.join("\n");

      const messageDoc = {
        type: "APPOINTMENT_STATUS",
        target: "APPOINTMENT",
        appointmentId,
        barberId: after.barberId || "",
        barberName,
        branchId: after.branchId || "",
        branchName,
        status: newStatus,
        text,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        seen: false,
      };

      await admin
          .firestore()
          .collection("users")
          .doc(customerId)
          .collection("messages")
          .add(messageDoc);

      // Optional push ללקוח
      const customerDoc = await admin
          .firestore()
          .collection("users")
          .doc(customerId)
          .get();

      if (!customerDoc.exists) return;

      const token = customerDoc.data().fcmToken;
      if (!token) return;

      await admin.messaging().send({
        token,
        notification: {
          title: "Appointment update",
          body: `Status changed: ${newStatus}. Tap to view.`,
        },
        data: {
          type: "APPOINTMENT_STATUS",
          target: "APPOINTMENT",
          appointmentId,
          status: newStatus,
        },
      });
    },
);

// =====================================================
// 3) סטטוס משתנה ל-DONE -> הודעה ללקוח לדרג
// =====================================================
exports.notifyAppointmentDone = onDocumentUpdated(
    "appointments/{id}",
    async (event) => {
      if (!event.data) return;

      const before = event.data.before.data();
      const after = event.data.after.data();
      if (!before || !after) return;

      // רק כשסטטוס משתנה ל-DONE
      if (before.status === after.status) return;
      if (after.status !== "DONE") return;

      const appointmentId = event.params.id;
      const userId = after.userId;
      if (!userId) return;

      const barberName = after.barberName || "your barber";
      const branchName = after.branchName || "";

      const complimentIndex = Math.floor(
          Math.random() * COMPLIMENTS.length,
      );
      const compliment = COMPLIMENTS[complimentIndex];

      const messageText =
      `${compliment}\nPlease rate ${barberName} (0–5 stars).`;

      const messageDoc = {
        type: "RATE_REQUEST",
        target: "RATE",
        appointmentId,
        barberId: after.barberId || "",
        barberName,
        branchId: after.branchId || "",
        branchName,
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

      // Optional push
      const userDoc = await admin
          .firestore()
          .collection("users")
          .doc(userId)
          .get();

      if (!userDoc.exists) return;

      const token = userDoc.data().fcmToken;
      if (!token) return;

      await admin.messaging().send({
        token,
        notification: {
          title: "Appointment done",
          body: `${compliment} Tap to rate ${barberName}.`,
        },
        data: {
          type: "RATE_REQUEST",
          target: "RATE",
          appointmentId,
          barberName,
          branchName,
        },
      });
    },
);

// =====================================================
// 4) כשהדירוג משתנה -> עדכון סכום/כמות דירוגים אצל הספר
// =====================================================
exports.onAppointmentRated = onDocumentUpdated(
    "appointments/{id}",
    async (event) => {
      if (!event.data) return;

      const before = event.data.before.data();
      const after = event.data.after.data();
      if (!before || !after) return;

      const beforeRating = before.rating;
      const afterRating = after.rating;

      // רק אם הדירוג השתנה והוגדר
      if (beforeRating === afterRating) return;
      if (afterRating === null || afterRating === undefined) return;

      const barberId = after.barberId;
      if (!barberId) return;

      const rating = Number(afterRating);
      if (!Number.isFinite(rating) || rating < 1 || rating > 5) return;

      const barberRef = admin.firestore().collection("barbers").doc(barberId);

      try {
        await barberRef.set(
            {
              ratingSum: admin.firestore.FieldValue.increment(rating),
              ratingCount: admin.firestore.FieldValue.increment(1),
            },
            {merge: true},
        );

        logger.info(
            "Updated barber rating aggregates",
            {barberId, rating},
        );
      } catch (err) {
        logger.error("Failed updating barber aggregates", err);
      }
    },
);
