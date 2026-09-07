"use strict";

const fs = require("fs");
const path = require("path");

const COLLECTION = "engineCatalog";
const META_DOC_ID = "meta";
const CHUNK_SIZE = 400;
const BATCH_LIMIT = 400;

function defaultCatalogPath() {
  return path.resolve(__dirname, "../../androidApp/src/main/assets/engine_catalog.json");
}

function chunkDocId(index) {
  return `chunk_${index}`;
}

async function deleteExistingChunks(db) {
  const snapshot = await db.collection(COLLECTION).get();
  if (snapshot.empty) return 0;

  let deleted = 0;
  let batch = db.batch();
  let ops = 0;

  for (const doc of snapshot.docs) {
    if (doc.id === META_DOC_ID) continue;
    batch.delete(doc.ref);
    ops += 1;
    deleted += 1;
    if (ops >= BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      ops = 0;
    }
  }
  if (ops > 0) {
    await batch.commit();
  }
  return deleted;
}

async function seedEngineCatalog(db, options = {}) {
  const catalogPath = options.catalogPath || defaultCatalogPath();
  const FieldValue = options.FieldValue || require("firebase-admin").firestore.FieldValue;

  if (!fs.existsSync(catalogPath)) {
    throw new Error(`Catalog file not found: ${catalogPath}`);
  }

  const catalog = JSON.parse(fs.readFileSync(catalogPath, "utf8"));
  const entries = Array.isArray(catalog.entries) ? catalog.entries : [];
  if (entries.length === 0) {
    throw new Error("Catalog has no entries");
  }

  const removed = await deleteExistingChunks(db);

  const chunkIds = [];
  let batch = db.batch();
  let ops = 0;

  for (let offset = 0; offset < entries.length; offset += CHUNK_SIZE) {
    const index = Math.floor(offset / CHUNK_SIZE);
    const id = chunkDocId(index);
    chunkIds.push(id);
    const chunkEntries = entries.slice(offset, offset + CHUNK_SIZE);
    batch.set(db.collection(COLLECTION).doc(id), {
      index,
      entryCount: chunkEntries.length,
      entries: chunkEntries,
    });
    ops += 1;
    if (ops >= BATCH_LIMIT) {
      await batch.commit();
      batch = db.batch();
      ops = 0;
    }
  }
  if (ops > 0) {
    await batch.commit();
  }

  await db.collection(COLLECTION).doc(META_DOC_ID).set({
    version: catalog.version ?? 1,
    entryCount: entries.length,
    source: catalog.source ?? "",
    chunkIds,
    chunkSize: CHUNK_SIZE,
    updatedAt: FieldValue.serverTimestamp(),
  });

  return {
    entryCount: entries.length,
    chunkCount: chunkIds.length,
    removedChunks: removed,
    version: catalog.version ?? 1,
  };
}

module.exports = {
  COLLECTION,
  META_DOC_ID,
  CHUNK_SIZE,
  defaultCatalogPath,
  seedEngineCatalog,
};
