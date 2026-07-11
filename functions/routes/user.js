"use strict";

const crypto = require("node:crypto");
const {
  getProductPurchase,
  acknowledgeProductPurchase,
  PRO_PRODUCT_ID,
  DEFAULT_PACKAGE_NAME,
} = require("../lib/playBilling");

function hashUserIdForPlay(userId) {
  return crypto.createHash("sha256").update(String(userId), "utf8").digest("hex");
}

function purchaseTokenDocId(token) {
  return crypto.createHash("sha256").update(String(token), "utf8").digest("hex");
}

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
      const { purchaseToken, productId } = req.body || {};
      if (!purchaseToken || !productId) {
        return res.status(400).json({ error: "purchaseToken und productId erforderlich" });
      }
      if (productId !== PRO_PRODUCT_ID) {
        return res.status(400).json({ error: "Ungültiges Produkt" });
      }
      const normalizedToken = String(purchaseToken).trim();
      if (!normalizedToken) {
        return res.status(400).json({ error: "purchaseToken ist leer" });
      }
      const resolvedPackage = DEFAULT_PACKAGE_NAME;
      const purchase = await getProductPurchase({
        packageName: resolvedPackage,
        productId,
        purchaseToken: normalizedToken,
      });
      if (purchase.purchaseState !== 0) {
        return res.status(400).json({ error: "Kauf konnte nicht verifiziert werden" });
      }
      if (purchase.productId && purchase.productId !== productId) {
        return res.status(400).json({ error: "Produkt stimmt nicht mit Kauf ueberein" });
      }

      const expectedObfuscatedAccountId = hashUserIdForPlay(userId);
      if (
        purchase.obfuscatedExternalAccountId &&
        purchase.obfuscatedExternalAccountId !== expectedObfuscatedAccountId
      ) {
        return res.status(409).json({
          error: "Kauf gehoert zu einem anderen Konto",
        });
      }

      const tokenRef = db.collection("playPurchaseTokens").doc(purchaseTokenDocId(normalizedToken));
      const userRef = db.collection("users").doc(userId);
      let isReplayForSameUser = false;
      await db.runTransaction(async (tx) => {
        const tokenSnap = await tx.get(tokenRef);
        if (tokenSnap.exists) {
          const tokenData = tokenSnap.data() || {};
          if (tokenData.userId && tokenData.userId !== userId) {
            throw new Error("purchase_token_already_used");
          }
          isReplayForSameUser = true;
        }
        tx.set(
          tokenRef,
          {
            userId,
            purchaseToken: normalizedToken,
            productId,
            packageName: resolvedPackage,
            orderId: purchase.orderId || null,
            purchaseState: purchase.purchaseState ?? null,
            acknowledgementState: purchase.acknowledgementState ?? null,
            purchaseType: purchase.purchaseType ?? null,
            obfuscatedExternalAccountId: purchase.obfuscatedExternalAccountId || null,
            lastVerifiedAt: FieldValue.serverTimestamp(),
            verified: true,
            voidedAt: null,
          },
          { merge: true },
        );
        tx.set(
          userRef,
          {
            isPro: true,
            proPurchase: {
              productId,
              packageName: resolvedPackage,
              purchaseToken: normalizedToken,
              orderId: purchase.orderId || null,
              purchaseType: purchase.purchaseType ?? null,
              verifiedAt: FieldValue.serverTimestamp(),
            },
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );
      });

      if (purchase.acknowledgementState !== 1) {
        await acknowledgeProductPurchase({
          packageName: resolvedPackage,
          productId,
          purchaseToken: normalizedToken,
        });
        await tokenRef.set(
          {
            acknowledgementState: 1,
            acknowledgedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );
      }

      res.json({
        isPro: true,
        reusedToken: isReplayForSameUser,
        purchaseType: purchase.purchaseType ?? null,
      });
    } catch (error) {
      console.error("POST /user/verify-pro-purchase:", error);
      if (error?.message === "purchase_token_already_used") {
        return res.status(409).json({ error: "purchaseToken wurde bereits von einem anderen Konto verwendet" });
      }
      const status = error?.response?.status;
      if (status === 400 || status === 404) {
        return res.status(400).json({ error: "Kauf konnte nicht verifiziert werden" });
      }
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
