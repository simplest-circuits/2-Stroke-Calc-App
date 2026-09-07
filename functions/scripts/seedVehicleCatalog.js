#!/usr/bin/env node
"use strict";

const path = require("path");
const { seedVehicleCatalog, defaultCatalogPath } = require("../lib/vehicleCatalogSeed");
const { initAdminForScripts, credentialsHint } = require("../lib/initAdminForScripts");

async function main() {
  const catalogArg = process.argv.find((arg) => arg.startsWith("--catalog="));
  const saArg = process.argv.find((arg) => arg.startsWith("--service-account="));
  const catalogPath = catalogArg
    ? path.resolve(catalogArg.slice("--catalog=".length))
    : defaultCatalogPath();

  const admin = initAdminForScripts({
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
  console.error("Seed failed:", error.message || error);
  if (String(error.message || error).includes("Could not load the default credentials")) {
    console.error(credentialsHint());
  }
  process.exit(1);
});
