"use strict";

const { resolveAuthenticatedUid } = require("../../lib/auth");

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

function createRequireAdmin(deps) {
  const { db } = deps;
  return async function requireAdmin(req, res, next) {
    try {
      const userId = await resolveAuthenticatedUid(req, res);
      if (!userId) return;
      const adminUserDoc = await db.collection("users").doc(userId).get();
      const adminData = adminUserDoc.exists ? adminUserDoc.data() : null;
      const isAdminUser =
        adminData?.isAdmin === true ||
        String(adminData?.role || "").toUpperCase() === "ADMIN";
      if (!adminUserDoc.exists || !isAdminUser) {
        return res.status(403).json({ error: "Nur Admins" });
      }
      req.userId = userId;
      req.adminUserId = userId;
      next();
    } catch (error) {
      next(error);
    }
  };
}

module.exports = { requireAuth, createRequireAdmin };
