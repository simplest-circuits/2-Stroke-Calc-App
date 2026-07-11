const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const functionsV1 = require("firebase-functions/v1");

const googlePlayServiceAccountJson = defineSecret("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
const express = require("express");
const cors = require("cors");
const { admin, db } = require("./lib/firebase");
const { registerHttpRoutes } = require("./registerHttpRoutes");
const { createUserLifecycleService } = require("./routes/services/userLifecycle");

const userLifecycle = createUserLifecycleService({ admin, db });

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

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
    secrets: [googlePlayServiceAccountJson],
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
