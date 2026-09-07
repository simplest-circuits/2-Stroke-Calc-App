"use strict";

const { APP_ID, APP_NAME, PACKAGE_NAME } = require("./adminWebChannel");

/**
 * Machine-readable catalog of admin capabilities for simplestsoft.de/admin.
 */
function buildAdminWebChannelManifest(apiBaseUrl) {
  const base = typeof apiBaseUrl === "string" ? apiBaseUrl.replace(/\/$/, "") : "";

  const capability = (id, method, path, summary, options = {}) => ({
    id,
    method,
    path,
    url: `${base}${path}`,
    summary,
    auth: "admin",
    tags: options.tags || [],
    query: options.query || null,
    body: options.body || null,
    longRunning: options.longRunning === true,
    destructive: options.destructive === true,
  });

  return {
    schemaVersion: 1,
    appId: APP_ID,
    appName: APP_NAME,
    packageName: PACKAGE_NAME,
    platform: "android",
    adminDashboardUrl: "https://simplestsoft.de/admin",
    apiBaseUrl: base,
    authModes: [
      {
        id: "firebase_bearer",
        type: "http_bearer",
        header: "Authorization",
        format: "Bearer <Firebase ID token>",
        description: "Gleiche Authentifizierung wie die Android-App (Admin-Nutzer).",
      },
      {
        id: "web_channel_hmac",
        type: "hmac_sha256",
        headers: {
          timestamp: "X-SS-Admin-Timestamp",
          signature: "X-SS-Admin-Signature",
          actingUserId: "X-SS-Admin-User-Id",
          appId: "X-SS-Admin-App-Id",
        },
        description:
          "Server-zu-Server vom SimplestSoft-Admin-Backend. HMAC über timestamp, method, path und SHA-256 des Request-Bodys.",
      },
    ],
    channel: {
      manifestPath: "/admin/web-channel/manifest",
      pingPath: "/admin/web-channel/ping",
      invokePath: "/admin/web-channel/invoke",
    },
    capabilities: [
      capability("live_stats", "GET", "/admin/live-stats", "Live-KPIs (Nutzer, Aktivität)", {
        tags: ["statistics"],
      }),
      capability("statistics", "GET", "/admin/statistics", "Aggregierte Nutzerstatistiken", {
        tags: ["statistics"],
      }),
      capability("users_list", "GET", "/admin/users", "Nutzerliste", { tags: ["users"] }),
      capability("users_search", "GET", "/admin/users/search", "Nutzer suchen", {
        query: { q: "string (min. 2)", limit: "number" },
        tags: ["users"],
      }),
      capability("user_reset_password", "POST", "/admin/users/:userId/reset-password", "Passwort-Reset-Link", {
        tags: ["users"],
      }),
      capability("user_ban", "POST", "/admin/users/:userId/ban", "Nutzer sperren/entsperren", {
        body: { banned: "boolean" },
        tags: ["users"],
      }),
      capability("user_activate", "POST", "/admin/users/:userId/activate", "Nutzer aktivieren/deaktivieren", {
        body: { active: "boolean" },
        tags: ["users"],
      }),
      capability("user_delete", "DELETE", "/admin/users/:userId", "Nutzer löschen", {
        tags: ["users"],
        destructive: true,
      }),
      capability("user_update_role", "POST", "/admin/users/:userId/update-role", "Rolle ändern", {
        body: { role: "USER|ADMIN" },
        tags: ["users"],
      }),
      capability("user_pro", "POST", "/admin/users/:userId/pro", "Pro-Status setzen", {
        body: { isPro: "boolean" },
        tags: ["users", "billing"],
      }),
      capability("settings_get", "GET", "/admin/settings", "App-Einstellungen lesen", { tags: ["settings"] }),
      capability("settings_put", "PUT", "/admin/settings", "App-Einstellungen speichern", {
        tags: ["settings"],
      }),
      capability("push_all", "POST", "/admin/notifications/push-all", "Push an alle Geräte", {
        body: { title: "string", body: "string" },
        tags: ["notifications"],
      }),
    ],
  };
}

module.exports = { buildAdminWebChannelManifest };
