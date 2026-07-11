"use strict";

const { requireAuth, createRequireAdmin } = require("./middleware/auth");
const { createUserLifecycleService } = require("./services/userLifecycle");

function createRouteContext(ctx) {
  const { admin, db } = ctx;
  const { FieldValue } = admin.firestore;
  const requireAdmin = createRequireAdmin({ db });
  const userLifecycle = createUserLifecycleService({ admin, db });

  return {
    admin,
    db,
    FieldValue,
    requireAuth,
    requireAdmin,
    deleteUserData: userLifecycle.deleteUserData,
    ensureUserProfile: userLifecycle.ensureUserProfile,
    sendEmailToAdmins: async () => {},
  };
}

module.exports = { createRouteContext };
