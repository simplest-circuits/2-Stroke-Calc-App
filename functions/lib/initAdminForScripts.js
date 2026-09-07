"use strict";

const fs = require("fs");
const os = require("os");
const path = require("path");
const admin = require("firebase-admin");

/** Public OAuth client used by the Firebase CLI (from firebase-tools/lib/api.js). */
const FIREBASE_TOOLS_CLIENT_ID =
  "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com";
const FIREBASE_TOOLS_CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi";

function firebaseToolsConfigPath() {
  return path.join(os.homedir(), ".config", "configstore", "firebase-tools.json");
}

function readFirebaseCliRefreshToken() {
  const configPath = firebaseToolsConfigPath();
  if (!fs.existsSync(configPath)) return null;
  try {
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const token = config?.tokens?.refresh_token;
    return typeof token === "string" && token.length > 0 ? token : null;
  } catch {
    return null;
  }
}

/**
 * Firestore Admin requires certificate or Application Default Credentials.
 * Materialize the Firebase CLI refresh token as a temporary ADC file.
 */
function materializeFirebaseCliAdc(refreshToken) {
  const adcPath = path.join(os.tmpdir(), `twostrokecalc-firebase-cli-adc-${process.pid}.json`);
  const payload = {
    type: "authorized_user",
    client_id: FIREBASE_TOOLS_CLIENT_ID,
    client_secret: FIREBASE_TOOLS_CLIENT_SECRET,
    refresh_token: refreshToken,
  };
  fs.writeFileSync(adcPath, JSON.stringify(payload), { encoding: "utf8", mode: 0o600 });
  process.env.GOOGLE_APPLICATION_CREDENTIALS = adcPath;
  process.on("exit", () => {
    try {
      fs.unlinkSync(adcPath);
    } catch {
      // ignore cleanup errors
    }
  });
  return adcPath;
}

/**
 * Initializes firebase-admin for local scripts.
 * Order: explicit service-account file -> env credentials
 * -> Firebase CLI login (as temporary ADC) -> Application Default Credentials.
 */
function initAdminForScripts(options = {}) {
  if (admin.apps.length) {
    return admin;
  }

  const projectId =
    options.projectId ||
    process.env.GCLOUD_PROJECT ||
    process.env.GOOGLE_CLOUD_PROJECT ||
    "strokecalc-app";

  const serviceAccountPath =
    options.serviceAccountPath ||
    process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    process.env.FIREBASE_SERVICE_ACCOUNT;

  if (serviceAccountPath && fs.existsSync(serviceAccountPath)) {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
    // Service-account JSON has private_key; ADC authorized_user does not.
    if (serviceAccount.private_key && serviceAccount.client_email) {
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: serviceAccount.project_id || projectId,
      });
      return admin;
    }
    // Fall through: treat as ADC file path already pointed by env/arg.
  }

  const refreshToken = readFirebaseCliRefreshToken();
  if (refreshToken && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    materializeFirebaseCliAdc(refreshToken);
  }

  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId,
  });
  return admin;
}

function credentialsHint() {
  return [
    "No usable credentials found.",
    "Fix one of:",
    "  1) firebase login",
    "  2) gcloud auth application-default login",
    "  3) set GOOGLE_APPLICATION_CREDENTIALS to a service-account JSON",
    "  4) npm run seed:engines -- --service-account=C:\\path\\to\\sa.json",
  ].join("\n");
}

module.exports = {
  initAdminForScripts,
  credentialsHint,
};
