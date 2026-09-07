"use strict";

const {
  mergeCalculatorAvailability,
  normalizeCalculatorAvailability,
} = require("../lib/calculatorConfig");
const {
  mergeToolAvailability,
  normalizeToolAvailability,
} = require("../lib/toolConfig");
const { mergeProModules, normalizeProModules } = require("../lib/proModuleConfig");
const { isDemoVehicleId } = require("../lib/demoVehicles");

/**
 * Minimal admin routes for 2-Stroke Lab (no billing / AI / application routes).
 */
function registerAdminRoutes(app, ctx) {
  const { admin, db, FieldValue, requireAdmin, deleteUserData } = ctx;

  app.post("/admin/set-admin", requireAdmin, async (req, res) => {
    try {
      const adminUserId = req.adminUserId;
      const { userId, isAdmin = true } = req.body || {};
      if (!userId) {
        return res.status(400).json({ error: "userId fehlt" });
      }
      if (userId === adminUserId && isAdmin !== true) {
        return res.status(400).json({ error: "Eigenes Admin-Recht kann nicht entfernt werden" });
      }
      await db.collection("users").doc(userId).set(
        {
          isAdmin: isAdmin === true,
          role: isAdmin === true ? "ADMIN" : "USER",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      res.json({ success: true, userId, isAdmin: isAdmin === true });
    } catch (error) {
      console.error("POST /admin/set-admin:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/check-admin/:userId", requireAdmin, async (req, res) => {
    try {
      const { userId } = req.params;
      const userDoc = await db.collection("users").doc(userId).get();
      if (!userDoc.exists) {
        return res.json({ userId, isAdmin: false, exists: false });
      }
      const userData = userDoc.data();
      res.json({
        userId,
        isAdmin: userData?.isAdmin === true,
        exists: true,
      });
    } catch (error) {
      console.error("GET /admin/check-admin:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/users", requireAdmin, async (req, res) => {
    try {
      const mapDeviceDocToResponse = (data) => {
        if (!data || typeof data !== "object") return null;
        const updatedAt = data.updatedAt?.toDate?.()
          ? data.updatedAt.toDate().toISOString()
          : (typeof data.updatedAt === "string" ? data.updatedAt : null);
        const fcmToken = typeof data.fcmToken === "string" ? data.fcmToken.trim() : "";
        const device = {
          applicationId: data.applicationId || null,
          appVersionName: data.appVersionName || null,
          appVersionCode: data.appVersionCode || null,
          buildType: data.buildType || null,
          deviceModel: data.deviceModel || null,
          deviceManufacturer: data.deviceManufacturer || null,
          deviceBrand: data.deviceBrand || null,
          deviceProduct: data.deviceProduct || null,
          androidVersion: data.androidVersion || null,
          androidSdk: data.androidSdk || null,
          locale: data.locale || null,
          screenWidthPx: data.screenWidthPx || null,
          screenHeightPx: data.screenHeightPx || null,
          screenDensityDpi: data.screenDensityDpi || null,
          pushEnabled: data.enabled !== false,
          hasFcmToken: fcmToken.length > 0,
          updatedAt,
        };
        const hasInfoKeys = [
          "applicationId",
          "appVersionName",
          "appVersionCode",
          "buildType",
          "deviceModel",
          "deviceManufacturer",
          "deviceBrand",
          "deviceProduct",
          "androidVersion",
          "androidSdk",
          "locale",
          "screenWidthPx",
          "screenHeightPx",
          "screenDensityDpi",
        ];
        const hasInfo = hasInfoKeys.some((key) => device[key] != null);
        return hasInfo ? device : null;
      };

      const toIsoString = (value) => {
        if (value?.toDate) return value.toDate().toISOString();
        if (typeof value === "string") return value;
        return null;
      };

      const toTrimmedStringOrNull = (value) => {
        if (value == null) return null;
        const text = String(value).trim();
        return text.length > 0 ? text : null;
      };

      const toVehicleSummary = (vehicleDoc, vehicleData) => {
        const fallbackId = typeof vehicleDoc?.id === "string" ? vehicleDoc.id : null;
        const explicitId = typeof vehicleData?.id === "string" ? vehicleData.id.trim() : "";
        const id = explicitId || fallbackId;
        if (isDemoVehicleId(id)) return null;
        const updatedAtMs = Number(vehicleData?.updatedAtMs);
        return {
          id,
          name: toTrimmedStringOrNull(vehicleData?.name),
          brand: toTrimmedStringOrNull(vehicleData?.brand),
          model: toTrimmedStringOrNull(vehicleData?.model),
          year: toTrimmedStringOrNull(vehicleData?.year),
          isActive: vehicleData?.isActive !== false,
          currentOdometerKm: toTrimmedStringOrNull(vehicleData?.currentOdometerKm),
          currentOperatingHours: toTrimmedStringOrNull(vehicleData?.currentOperatingHours),
          updatedAtMs: Number.isFinite(updatedAtMs) ? updatedAtMs : 0,
        };
      };

      const fetchUserVehicles = async (userId) => {
        const vehiclesSnapshot = await db.collection("users")
          .doc(userId)
          .collection("vehicles")
          .get();
        const vehicles = [];
        vehiclesSnapshot.forEach((vehicleDoc) => {
          const vehicleSummary = toVehicleSummary(vehicleDoc, vehicleDoc.data() || {});
          if (vehicleSummary) vehicles.push(vehicleSummary);
        });
        return vehicles.sort((a, b) => {
          if (a.updatedAtMs !== b.updatedAtMs) return b.updatedAtMs - a.updatedAtMs;
          const aName = a.name || [a.brand, a.model].filter(Boolean).join(" ");
          const bName = b.name || [b.brand, b.model].filter(Boolean).join(" ");
          return aName.localeCompare(bName);
        });
      };

      const devicesByUserId = new Map();
      const devicesSnapshot = await db.collectionGroup("devices").get();
      devicesSnapshot.forEach((deviceDoc) => {
        if (deviceDoc.id !== "default") return;
        const userRef = deviceDoc.ref.parent?.parent;
        if (!userRef) return;
        devicesByUserId.set(userRef.id, mapDeviceDocToResponse(deviceDoc.data() || {}));
      });

      const usersSnapshot = await db.collection("users").get();
      const users = [];
      for (const userDoc of usersSnapshot.docs) {
        const userData = userDoc.data();
        const userProPurchase = userData?.proPurchase;
        let firebaseUser = null;
        try {
          firebaseUser = await admin.auth().getUser(userDoc.id);
        } catch (e) {
          console.warn(`Auth user ${userDoc.id}:`, e.message);
        }
        const providerIds = Array.from(
          new Set(
            (firebaseUser?.providerData || [])
              .map((provider) => provider?.providerId)
              .filter((providerId) => typeof providerId === "string" && providerId.length > 0),
          ),
        );
        const isAdmin = userData.isAdmin === true;
        const role = isAdmin ? "ADMIN" : userData.role || "USER";
        const banned = userData.banned === true || userData.isBanned === true;
        const active = userData.active !== false && userData.isActive !== false && !banned;
        const userVehicles = await fetchUserVehicles(userDoc.id);
        const vehicles = userVehicles.slice(0, 5);
        users.push({
          id: userDoc.id,
          email: userData.email || firebaseUser?.email || "",
          displayName: userData.displayName || userData.name || firebaseUser?.displayName || "",
          role,
          active,
          banned,
          isPro: userData.isPro === true,
          phoneNumber: userData.phoneNumber || firebaseUser?.phoneNumber || null,
          emailVerified: firebaseUser?.emailVerified === true,
          authDisabled: firebaseUser?.disabled === true,
          providerIds,
          vehicleCount: userVehicles.length,
          vehicles,
          createdAt: toIsoString(userData.createdAt) || firebaseUser?.metadata?.creationTime || null,
          lastSignInAt: firebaseUser?.metadata?.lastSignInTime || null,
          lastRefreshAt: firebaseUser?.metadata?.lastRefreshTime || null,
          updatedAt: toIsoString(userData.updatedAt),
          lastPasswordResetAt: toIsoString(userData.lastPasswordReset),
          bannedAt: toIsoString(userData.bannedAt),
          proPurchase: userProPurchase && typeof userProPurchase === "object" ? {
            productId: userProPurchase.productId || null,
            orderId: userProPurchase.orderId || null,
            packageName: userProPurchase.packageName || null,
            purchaseType: userProPurchase.purchaseType ?? null,
            verifiedAt: toIsoString(userProPurchase.verifiedAt),
          } : null,
          device: devicesByUserId.get(userDoc.id) || null,
        });
      }
      res.json({ users });
    } catch (error) {
      console.error("GET /admin/users:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/users/search", requireAdmin, async (req, res) => {
    try {
      const query = typeof req.query?.q === "string" ? req.query.q.trim() : "";
      if (query.length < 2) {
        return res.status(400).json({ error: "Suchbegriff muss mindestens 2 Zeichen lang sein" });
      }
      const rawLimit = Number.parseInt(String(req.query?.limit ?? 25), 10);
      const limit = Number.isFinite(rawLimit) ? Math.min(Math.max(rawLimit, 1), 100) : 25;
      const queryLower = query.toLowerCase();

      const usersSnapshot = await db.collection("users").get();
      const users = [];
      for (const userDoc of usersSnapshot.docs) {
        if (users.length >= limit) break;
        const userData = userDoc.data() || {};
        let firebaseUser = null;
        try {
          firebaseUser = await admin.auth().getUser(userDoc.id);
        } catch (e) {
          // Auth user may be missing; fall back to Firestore fields.
        }
        const email = userData.email || firebaseUser?.email || "";
        const displayName = userData.displayName || userData.name || firebaseUser?.displayName || "";
        const haystack = `${userDoc.id} ${email} ${displayName}`.toLowerCase();
        if (!haystack.includes(queryLower)) continue;

        const isAdmin = userData.isAdmin === true;
        const role = isAdmin ? "ADMIN" : userData.role || "USER";
        const banned = userData.banned === true || userData.isBanned === true;
        const active = userData.active !== false && userData.isActive !== false && !banned;
        users.push({
          id: userDoc.id,
          email,
          displayName,
          role,
          active,
          banned,
        });
      }

      res.json({ users, nextCursor: null, totalUsers: users.length });
    } catch (error) {
      console.error("GET /admin/users/search:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/statistics", requireAdmin, async (req, res) => {
    try {
      const snap = await db.collection("users").get();
      const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
      let newToday = 0;
      let activeUsers = 0;
      let adminUsers = 0;
      let bannedUsers = 0;
      let inactiveUsers = 0;
      let estimatedStorageBytes = 0;

      snap.forEach((doc) => {
        const data = doc.data() || {};
        const ms = data.createdAt?.toMillis?.() ?? 0;
        if (ms >= dayAgo) newToday += 1;
        if (data.isActive !== false && data.active !== false && data.isBanned !== true && data.banned !== true) activeUsers += 1;
        if (data.isAdmin === true || String(data.role || "").toUpperCase() === "ADMIN") adminUsers += 1;
        if (data.isBanned === true || data.banned === true) bannedUsers += 1;
        if (data.isActive === false || data.active === false) inactiveUsers += 1;
        estimatedStorageBytes += JSON.stringify(data).length + doc.id.length;
      });

      res.json({
        totalUsers: snap.size,
        activeSessions: activeUsers,
        newToday,
        storageUsageMb: Number((estimatedStorageBytes / (1024 * 1024)).toFixed(2)),
        adminUsers,
        bannedUsers,
        inactiveUsers,
      });
    } catch (error) {
      console.error("GET /admin/statistics:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/live-stats", requireAdmin, async (req, res) => {
    try {
      const snap = await db.collection("users").get();
      const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
      let newToday = 0;
      let activeUsers = 0;
      let bannedUsers = 0;

      snap.forEach((doc) => {
        const data = doc.data() || {};
        const ms = data.createdAt?.toMillis?.() ?? 0;
        if (ms >= dayAgo) newToday += 1;
        if (data.isActive !== false && data.active !== false && data.isBanned !== true && data.banned !== true) {
          activeUsers += 1;
        }
        if (data.isBanned === true || data.banned === true) bannedUsers += 1;
      });

      res.json({
        totalUsers: snap.size,
        newUsersToday: newToday,
        activeListings: activeUsers,
        openReports: bannedUsers,
        demoListingCount: 0,
      });
    } catch (error) {
      console.error("GET /admin/live-stats:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/admin/settings", requireAdmin, async (req, res) => {
    try {
      const mainDoc = await db.collection("appConfig").doc("main").get();
      const main = mainDoc.exists ? mainDoc.data() : {};
      const hasProModulesConfig = mainDoc.exists && main.proModules != null;
      res.json({
        maintenanceMode: main.maintenanceMode === true,
        debugMode: main.debugMode === true,
        emailNotifications: main.emailNotificationsEnabled !== false,
        demoVehiclesEnabled: main.demoVehiclesEnabled !== false,
        calculatorAvailability: normalizeCalculatorAvailability(main.calculatorAvailability),
        toolAvailability: normalizeToolAvailability(main.toolAvailability),
        proModules: normalizeProModules(main.proModules, hasProModulesConfig),
      });
    } catch (error) {
      console.error("GET /admin/settings:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.put("/admin/settings", requireAdmin, async (req, res) => {
    try {
      const body = req.body || {};
      const update = {
        maintenanceMode: body.maintenanceMode === true,
        debugMode: body.debugMode === true,
        emailNotificationsEnabled: body.emailNotifications === true,
        demoVehiclesEnabled: body.demoVehiclesEnabled !== false,
        updatedAt: FieldValue.serverTimestamp(),
      };
      const mainDoc = await db.collection("appConfig").doc("main").get();
      const existingMain = mainDoc.exists ? mainDoc.data() : {};
      if (body.calculatorAvailability && typeof body.calculatorAvailability === "object") {
        const existing = existingMain.calculatorAvailability || {};
        update.calculatorAvailability = normalizeCalculatorAvailability({
          ...existing,
          ...mergeCalculatorAvailability(body.calculatorAvailability),
        });
      }
      if (body.toolAvailability && typeof body.toolAvailability === "object") {
        const existing = existingMain.toolAvailability || {};
        update.toolAvailability = normalizeToolAvailability({
          ...existing,
          ...mergeToolAvailability(body.toolAvailability),
        });
      }
      if (body.proModules && typeof body.proModules === "object") {
        const existing = existingMain.proModules || {};
        update.proModules = normalizeProModules(
          {
            ...existing,
            ...mergeProModules(body.proModules),
          },
          true,
        );
      }
      await db.collection("appConfig").doc("main").set(update, { merge: true });
      res.status(204).send();
    } catch (error) {
      console.error("PUT /admin/settings:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/users/:userId/reset-password", requireAdmin, async (req, res) => {
    try {
      const { userId } = req.params;
      let firebaseUser;
      try {
        firebaseUser = await admin.auth().getUser(userId);
      } catch (e) {
        return res.status(404).json({ error: "Benutzer nicht gefunden" });
      }
      if (!firebaseUser.email) {
        return res.status(400).json({ error: "Benutzer hat keine E-Mail-Adresse" });
      }
      const resetLink = await admin.auth().generatePasswordResetLink(firebaseUser.email);
      await db.collection("users").doc(userId).set(
        {
          lastPasswordReset: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      res.json({
        success: true,
        message: "Passwort-Reset-Link erzeugt",
        email: firebaseUser.email,
        resetLink,
      });
    } catch (error) {
      console.error("POST reset-password:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/users/:userId/ban", requireAdmin, async (req, res) => {
    try {
      const adminUserId = req.adminUserId;
      const { userId } = req.params;
      const { banned = true } = req.body || {};
      if (userId === adminUserId) {
        return res.status(400).json({ error: "Sie können sich nicht selbst sperren" });
      }
      await db.collection("users").doc(userId).set(
        {
          banned: banned === true,
          isBanned: banned === true,
          bannedAt: banned ? FieldValue.serverTimestamp() : null,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      try {
        await admin.auth().updateUser(userId, { disabled: banned === true });
      } catch (e) {
        console.warn("auth.updateUser ban:", e.message);
      }
      res.json({ success: true, userId, banned: banned === true });
    } catch (error) {
      console.error("POST ban:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.delete("/admin/users/:userId", requireAdmin, async (req, res) => {
    try {
      const adminUserId = req.adminUserId;
      const { userId } = req.params;
      if (userId === adminUserId) {
        return res.status(400).json({ error: "Sie können sich nicht selbst löschen" });
      }
      await deleteUserData(userId, { deleteAuthUser: true });
      res.json({ success: true, userId });
    } catch (error) {
      console.error("DELETE user:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/users/:userId/update-role", requireAdmin, async (req, res) => {
    try {
      const adminUserId = req.adminUserId;
      const { userId } = req.params;
      const { role } = req.body || {};
      const validRoles = ["USER", "ADMIN"];
      if (!role || !validRoles.includes(role)) {
        return res.status(400).json({ error: `Ungültige Rolle. Erlaubt: ${validRoles.join(", ")}` });
      }
      if (userId === adminUserId && role !== "ADMIN") {
        return res.status(400).json({ error: "Eigenes Admin-Recht kann nicht entfernt werden" });
      }
      const userRef = db.collection("users").doc(userId);
      const userDoc = await userRef.get();
      if (!userDoc.exists) {
        return res.status(404).json({ error: "Benutzer nicht gefunden" });
      }
      const isAdmin = role === "ADMIN";
      await userRef.set(
        {
          role,
          isAdmin,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      res.json({ success: true, newRole: role });
    } catch (error) {
      console.error("POST update-role:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/users/:userId/pro", requireAdmin, async (req, res) => {
    try {
      const { userId } = req.params;
      const { isPro = true } = req.body || {};
      const userRef = db.collection("users").doc(userId);
      const userDoc = await userRef.get();
      if (!userDoc.exists) {
        return res.status(404).json({ error: "Benutzer nicht gefunden" });
      }
      await userRef.set(
        {
          isPro: isPro === true,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      res.json({ success: true, userId, isPro: isPro === true });
    } catch (error) {
      console.error("POST pro:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/users/:userId/activate", requireAdmin, async (req, res) => {
    try {
      const adminUserId = req.adminUserId;
      const { userId } = req.params;
      const { active = true } = req.body || {};
      if (userId === adminUserId && active === false) {
        return res.status(400).json({ error: "Sie können sich nicht selbst deaktivieren" });
      }
      await db.collection("users").doc(userId).set(
        {
          active: active === true,
          isActive: active === true,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      try {
        await admin.auth().updateUser(userId, { disabled: active === false });
      } catch (e) {
        console.warn("auth.activate:", e.message);
      }
      res.json({ success: true, userId, active: active === true });
    } catch (error) {
      console.error("POST activate:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/admin/notifications/push-all", requireAdmin, async (req, res) => {
    try {
      const { title, body } = req.body || {};
      const notificationTitle = typeof title === "string" ? title.trim() : "";
      const notificationBody = typeof body === "string" ? body.trim() : "";
      if (!notificationTitle || !notificationBody) {
        return res.status(400).json({ error: "title und body erforderlich" });
      }

      const devicesSnapshot = await db.collectionGroup("devices").get();
      const tokenSet = new Set();
      devicesSnapshot.forEach((deviceDoc) => {
        const data = deviceDoc.data() || {};
        const token = typeof data.fcmToken === "string" ? data.fcmToken.trim() : "";
        const enabled = data.enabled !== false;
        if (enabled && token.length > 0) {
          tokenSet.add(token);
        }
      });

      const tokens = Array.from(tokenSet);
      if (tokens.length === 0) {
        return res.json({
          success: true,
          sentCount: 0,
          failureCount: 0,
          uniqueTokenCount: 0,
          message: "Keine FCM-Tokens",
        });
      }

      const response = await admin.messaging().sendEachForMulticast({
        tokens,
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
      });

      res.json({
        success: true,
        sentCount: response.successCount,
        failureCount: response.failureCount,
        uniqueTokenCount: tokens.length,
      });
    } catch (error) {
      console.error("POST push-all:", error);
      res.status(500).json({ error: error.message });
    }
  });
}

module.exports = { registerAdminRoutes };
