"use strict";

const { verifyProductPurchase, PRO_PRODUCT_ID, DEFAULT_PACKAGE_NAME } = require("../lib/playBilling");

/**
 * HTTP routes: user
 * @param {import("express").Express} app
 * @param {ReturnType<import("./createRouteContext").createRouteContext>} ctx
 */
function registerUserRoutes(app, ctx) {
  const { db, FieldValue, requireAuth, deleteUserData, admin, ensureUserProfile } = ctx;
  const sanitizeDeviceField = (value) => {
    if (value == null) return null;
    const text = String(value).trim();
    return text.length > 0 ? text.slice(0, 256) : null;
  };

  const extractDevicePayload = (device) => {
    if (!device || typeof device !== "object") return {};
    const fields = [
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
    const payload = {};
    fields.forEach((field) => {
      const value = sanitizeDeviceField(device[field]);
      if (value != null) payload[field] = value;
    });
    return payload;
  };

  app.post("/user/ensure-profile", requireAuth, async (req, res) => {
    try {
      const userId = req.userId;
      const authUser = await admin.auth().getUser(userId);
      await ensureUserProfile(authUser);
      res.status(204).send();
    } catch (error) {
      console.error("POST /user/ensure-profile:", error);
      res.status(500).json({ error: error.message || "Profil-Sync fehlgeschlagen" });
    }
  });

  app.post("/user/device-info", requireAuth, async (req, res) => {
    try {
      const userId = req.userId;
      const { userId: bodyUserId, device } = req.body || {};
      if (!bodyUserId || bodyUserId !== userId) {
        return res.status(400).json({ error: "userId erforderlich" });
      }
      const devicePayload = extractDevicePayload(device);
      if (Object.keys(devicePayload).length === 0) {
        return res.status(400).json({ error: "device erforderlich" });
      }
      await db
        .collection("users")
        .doc(userId)
        .collection("devices")
        .doc("default")
        .set(
          {
            ...devicePayload,
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );
      res.status(204).send();
    } catch (error) {
      console.error("POST /user/device-info:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/user/fcm-token", requireAuth, async (req, res) => {
    try {
      const userId = req.userId;
      const { token, userId: bodyUserId, device } = req.body || {};
      if (!token || !bodyUserId || bodyUserId !== userId) {
        return res.status(400).json({ error: "token und userId erforderlich" });
      }
      await db
        .collection("users")
        .doc(userId)
        .collection("devices")
        .doc("default")
        .set(
          {
            fcmToken: String(token).trim(),
            enabled: true,
            ...extractDevicePayload(device),
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );
      res.status(204).send();
    } catch (error) {
      console.error("POST /user/fcm-token:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.post("/user/verify-pro-purchase", requireAuth, async (req, res) => {
    try {
      const userId = req.userId;
      const { purchaseToken, productId, packageName } = req.body || {};
      if (!purchaseToken || !productId) {
        return res.status(400).json({ error: "purchaseToken und productId erforderlich" });
      }
      if (productId !== PRO_PRODUCT_ID) {
        return res.status(400).json({ error: "Ungültiges Produkt" });
      }
      const resolvedPackage = packageName || DEFAULT_PACKAGE_NAME;
      const valid = await verifyProductPurchase({
        packageName: resolvedPackage,
        productId,
        purchaseToken: String(purchaseToken).trim(),
      });
      if (!valid) {
        return res.status(400).json({ error: "Kauf konnte nicht verifiziert werden" });
      }
      await db.collection("users").doc(userId).set(
        {
          isPro: true,
          proPurchase: {
            productId,
            packageName: resolvedPackage,
            purchaseToken: String(purchaseToken).trim(),
            verifiedAt: FieldValue.serverTimestamp(),
          },
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      res.json({ isPro: true });
    } catch (error) {
      console.error("POST /user/verify-pro-purchase:", error);
      res.status(500).json({ error: error.message || "Kauf-Verifizierung fehlgeschlagen" });
    }
  });

  app.delete("/user/account", requireAuth, async (req, res) => {
    try {
      const userId = req.userId;
      await deleteUserData(userId, { deleteAuthUser: true });
      res.status(204).send();
    } catch (error) {
      console.error("DELETE /user/account:", error);
      res.status(500).json({ error: error.message || "Konto-Löschung fehlgeschlagen" });
    }
  });
}

module.exports = { registerUserRoutes };
