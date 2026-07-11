"use strict";

function createUserLifecycleService({ admin, db }) {
  async function ensureUserProfile(user) {
    if (!user?.uid) return;
    const { FieldValue } = admin.firestore;
    const userRef = db.collection("users").doc(user.uid);
    const snapshot = await userRef.get();
    const email = user.email || null;
    const displayName =
      user.displayName ||
      (email ? email.split("@")[0] : null) ||
      null;
    const payload = {
      email,
      displayName,
      photoUrl: user.photoURL || null,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (!snapshot.exists) {
      payload.role = "USER";
      payload.isAdmin = false;
      payload.isPro = false;
      payload.active = true;
      payload.banned = false;
      payload.createdAt = FieldValue.serverTimestamp();
    }
    await userRef.set(payload, { merge: true });
  }

  async function deleteUserData(userId, options = {}) {
    const deleteAuthUser = options.deleteAuthUser !== false;
    const userRef = db.collection("users").doc(userId);
    const subcollections = await userRef.listCollections();

    for (const collectionRef of subcollections) {
      const docs = await collectionRef.get();
      if (docs.empty) continue;
      let batch = db.batch();
      let ops = 0;
      for (const doc of docs.docs) {
        batch.delete(doc.ref);
        ops += 1;
        if (ops >= 450) {
          await batch.commit();
          batch = db.batch();
          ops = 0;
        }
      }
      if (ops > 0) {
        await batch.commit();
      }
    }

    await userRef.delete().catch(() => undefined);

    if (deleteAuthUser) {
      await admin.auth().deleteUser(userId).catch(() => undefined);
    }
  }

  return { deleteUserData, ensureUserProfile };
}

module.exports = { createUserLifecycleService };
