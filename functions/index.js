const { onRequest } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const functionsV1 = require("firebase-functions/v1");
const crypto = require("node:crypto");

const googlePlayServiceAccountJson = defineSecret("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
const adminWebChannelSecret = defineSecret("ADMIN_WEB_CHANNEL_SECRET");
const express = require("express");
const cors = require("cors");
const { admin, db } = require("./lib/firebase");
const { registerHttpRoutes } = require("./registerHttpRoutes");
const { createUserLifecycleService } = require("./routes/services/userLifecycle");
const { DEFAULT_PACKAGE_NAME, listVoidedPurchases } = require("./lib/playBilling");

const userLifecycle = createUserLifecycleService({ admin, db });

const app = express();
app.use(cors({ origin: true }));
app.use(
  express.json({
    verify: (req, _res, buf) => {
      req.rawBody = buf;
    },
  }),
);

app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  next();
});

registerHttpRoutes(app, { admin, db });

app.use((req, res) => {
  res.status(404).json({
    error: "Route not found",
    method: req.method,
    path: req.path,
  });
});

exports.api = onRequest(
  {
    region: "europe-west3",
    timeoutSeconds: 120,
    memory: "512MiB",
    cors: true,
    invoker: "public",
    secrets: [googlePlayServiceAccountJson, adminWebChannelSecret],
  },
  app,
);

/** Legt users/{uid} an, sobald ein Firebase-Auth-Konto erstellt wird. */
exports.onAuthUserCreated = functionsV1
  .region("europe-west3")
  .auth.user()
  .onCreate(async (user) => {
    try {
      await userLifecycle.ensureUserProfile(user);
      console.log("Provisioned Firestore profile for auth user:", user.uid, user.email || "");
    } catch (error) {
      console.error("onAuthUserCreated:", user.uid, error);
      throw error;
    }
  });

/** Aggregiert Sterne-Bewertungen serverseitig auf communitySetups/{setupId}. */
exports.onCommunitySetupRatingWritten = onDocumentWritten(
  {
    region: "europe-west3",
    document: "communitySetups/{setupId}/ratings/{userId}",
    memory: "256MiB",
    timeoutSeconds: 60,
  },
  async (event) => {
    const setupId = event.params.setupId;
    if (!setupId) return;

    const ratingsSnap = await db.collection("communitySetups").doc(setupId).collection("ratings").get();
    let sum = 0;
    let count = 0;
    ratingsSnap.forEach((doc) => {
      const value = Number(doc.data()?.value);
      if (Number.isFinite(value) && value >= 1 && value <= 5) {
        sum += value;
        count += 1;
      }
    });
    const ratingAverage = count > 0 ? Math.round((sum / count) * 100) / 100 : 0;
    await db.collection("communitySetups").doc(setupId).set(
      {
        ratingAverage,
        ratingCount: count,
        updatedAtMs: Date.now(),
      },
      { merge: true },
    );
  },
);

exports.syncVoidedPurchases = onSchedule(
  {
    region: "europe-west3",
    schedule: "every 6 hours",
    timeZone: "UTC",
    memory: "512MiB",
    timeoutSeconds: 120,
    secrets: [googlePlayServiceAccountJson],
  },
  async () => {
    const syncDocRef = db.collection("appConfig").doc("playBilling");
    const syncDoc = await syncDocRef.get();
    const nowMs = Date.now();
    const defaultStart = nowMs - (30 * 24 * 60 * 60 * 1000);
    const startTimeMillis = syncDoc.data()?.voidedSyncSeenStartTime || defaultStart;

    let pageToken = null;
    let processed = 0;
    do {
      const result = await listVoidedPurchases({
        packageName: DEFAULT_PACKAGE_NAME,
        startTimeMillis,
        token: pageToken || undefined,
        maxResults: 1000,
      });
      pageToken = result.nextPageToken || null;

      for (const voidedPurchase of result.purchases) {
        const purchaseToken = voidedPurchase.purchaseToken;
        if (!purchaseToken) continue;

        const tokenDocId = crypto.createHash("sha256").update(String(purchaseToken), "utf8").digest("hex");
        const tokenRef = db.collection("playPurchaseTokens").doc(tokenDocId);
        const tokenSnap = await tokenRef.get();
        if (!tokenSnap.exists) continue;

        const tokenData = tokenSnap.data() || {};
        const userId = tokenData.userId;
        const batch = db.batch();
        batch.set(
          tokenRef,
          {
            voidedAt: admin.firestore.FieldValue.serverTimestamp(),
            voidedReason: voidedPurchase.voidedReason ?? null,
            voidedSource: voidedPurchase.voidedSource ?? null,
            voidedTimeMillis: voidedPurchase.voidedTimeMillis || null,
            lastVoidedSyncAt: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true },
        );

        if (userId) {
          const userRef = db.collection("users").doc(userId);
          const userSnap = await userRef.get();
          const activeToken = userSnap.get("proPurchase.purchaseToken");
          if (activeToken && activeToken === purchaseToken) {
            batch.set(
              userRef,
              {
                isPro: false,
                proPurchaseRevokedAt: admin.firestore.FieldValue.serverTimestamp(),
                proPurchaseRevocationReason: "voided_purchase",
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
              },
              { merge: true },
            );
          }
        }
        await batch.commit();
        processed += 1;
      }
    } while (pageToken);

    await syncDocRef.set(
      {
        voidedSyncSeenStartTime: nowMs,
        voidedSyncLastRunAt: admin.firestore.FieldValue.serverTimestamp(),
        voidedSyncLastProcessedCount: processed,
      },
      { merge: true },
    );

    console.log("syncVoidedPurchases complete", { processed, startTimeMillis });
  },
);
