#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");
const { seedVehicleCatalog, defaultCatalogPath } = require("../lib/vehicleCatalogSeed");

function initFirebase(options = {}) {
  if (admin.apps.length) return;

  const serviceAccountPath =
    options.serviceAccountPath ||
    process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    process.env.FIREBASE_SERVICE_ACCOUNT;

  if (serviceAccountPath && fs.existsSync(serviceAccountPath)) {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id,
    });
    return;
  }

  admin.initializeApp({
    projectId: options.projectId || process.env.GCLOUD_PROJECT || "strokecalc-app",
  });
}

async function main() {
  const catalogArg = process.argv.find((arg) => arg.startsWith("--catalog="));
  const saArg = process.argv.find((arg) => arg.startsWith("--service-account="));
  const catalogPath = catalogArg
    ? path.resolve(catalogArg.slice("--catalog=".length))
    : defaultCatalogPath();

  initFirebase({
    serviceAccountPath: saArg ? path.resolve(saArg.slice("--service-account=".length)) : null,
  });
  const db = admin.firestore();

  console.log(`Seeding vehicle catalog from ${catalogPath}`);
  const result = await seedVehicleCatalog(db, {
    catalogPath,
    FieldValue: admin.firestore.FieldValue,
  });
  console.log(
    `Done: version=${result.version}, entries=${result.entryCount}, chunks=${result.chunkCount}, removed=${result.removedChunks}`,
  );
}

main().catch((error) => {
  console.error("Seed failed:", error);
  console.error(
    "Hint: set GOOGLE_APPLICATION_CREDENTIALS to a service-account JSON, or run gcloud auth application-default login",
  );
  process.exit(1);
});
