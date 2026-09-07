"use strict";

const { resolveAuthenticatedUid } = require("../../lib/auth");
const { verifyAdminWebChannelRequest } = require("../../lib/adminWebChannel");

async function requireAuth(req, res, next) {
  try {
    const userId = await resolveAuthenticatedUid(req, res);
    if (!userId) return;
    req.userId = userId;
    next();
  } catch (error) {
    next(error);
  }
}

async function verifyAdminUser(db, userId) {
  const adminUserDoc = await db.collection("users").doc(userId).get();
  const adminData = adminUserDoc.exists ? adminUserDoc.data() : null;
  if (!adminUserDoc.exists) {
    return { ok: false, status: 403, error: "Nur Admins" };
  }
  const isAdminUser =
    adminData?.isAdmin === true ||
    String(adminData?.role || "").toUpperCase() === "ADMIN";
  if (!isAdminUser) {
    return { ok: false, status: 403, error: "Nur Admins" };
  }
  return { ok: true, adminData };
}

function createRequireAdmin(deps) {
  const { db } = deps;
  return async function requireAdmin(req, res, next) {
    try {
      const webChannel = verifyAdminWebChannelRequest(req);
      if (webChannel) {
        const verification = await verifyAdminUser(db, webChannel.actingUserId);
        if (!verification.ok) {
          return res.status(verification.status).json({ error: verification.error });
        }
        req.userId = webChannel.actingUserId;
        req.adminUserId = webChannel.actingUserId;
        req.adminAuthMode = webChannel.authMode;
        req.adminChannelAppId = webChannel.appId;
        return next();
      }

      const userId = await resolveAuthenticatedUid(req, res);
      if (!userId) return;

      const verification = await verifyAdminUser(db, userId);
      if (!verification.ok) {
        return res.status(verification.status).json({ error: verification.error });
      }

      req.userId = userId;
      req.adminUserId = userId;
      req.adminAuthMode = "firebase_bearer";
      next();
    } catch (error) {
      next(error);
    }
  };
}

module.exports = { requireAuth, createRequireAdmin };
