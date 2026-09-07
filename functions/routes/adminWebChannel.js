"use strict";

const {
  APP_ID,
  APP_NAME,
  PACKAGE_NAME,
  resolveAdminWebChannelSecret,
  createAdminWebChannelSignatureHeaders,
} = require("../lib/adminWebChannel");
const { buildAdminWebChannelManifest } = require("../lib/adminWebChannelManifest");

const INVOKE_BLOCKED_PATHS = new Set([
  "/admin/web-channel/invoke",
  "/admin/web-channel/manifest",
  "/admin/set-admin",
  "/admin/check-admin",
]);

/**
 * Base URL for manifest links and invoke self-calls.
 * Never trusts X-Forwarded-Host (SSRF). Prefer ADMIN_API_PUBLIC_BASE_URL.
 */
function resolveApiBaseUrl(req) {
  const configured = String(process.env.ADMIN_API_PUBLIC_BASE_URL || "").trim().replace(/\/$/, "");
  if (configured) return configured;
  const host = String(req.get("host") || "").split(",")[0].trim();
  if (!host) return "";
  return `https://${host}`;
}

function normalizeInvokePath(path) {
  const trimmed = String(path || "").trim();
  if (!trimmed.startsWith("/admin/")) return null;
  return trimmed.split("?")[0];
}

function isInvokePathAllowed(path) {
  const normalized = normalizeInvokePath(path);
  if (!normalized) return false;
  if (INVOKE_BLOCKED_PATHS.has(normalized)) return false;
  if (normalized.startsWith("/admin/set-admin")) return false;
  if (normalized.startsWith("/admin/check-admin")) return false;
  const manifest = buildAdminWebChannelManifest("");
  return manifest.capabilities.some((entry) => {
    if (entry.path === normalized) return true;
    const pattern = entry.path.replace(/:[^/]+/g, "[^/]+");
    return new RegExp(`^${pattern}$`).test(normalized);
  });
}

function buildInvokeQueryString(query) {
  if (!query || typeof query !== "object" || Array.isArray(query)) return "";
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value == null || value === "") return;
    params.set(key, String(value));
  });
  const serialized = params.toString();
  return serialized.length > 0 ? `?${serialized}` : "";
}

/**
 * HTTP routes: secure admin web channel for simplestsoft.de/admin
 */
function registerAdminWebChannelRoutes(app, ctx) {
  const { requireAdmin } = ctx;

  app.get("/admin/web-channel/manifest", requireAdmin, (req, res) => {
    try {
      const manifest = buildAdminWebChannelManifest(resolveApiBaseUrl(req));
      res.json(manifest);
    } catch (error) {
      console.error("GET /admin/web-channel/manifest:", error);
      res.status(500).json({ error: error.message || "Manifest konnte nicht erzeugt werden" });
    }
  });

  app.get("/admin/web-channel/ping", requireAdmin, (req, res) => {
    res.json({
      ok: true,
      appId: APP_ID,
      appName: APP_NAME,
      packageName: PACKAGE_NAME,
      authMode: req.adminAuthMode || "firebase_bearer",
      adminUserId: req.adminUserId || null,
      webChannelConfigured: resolveAdminWebChannelSecret() != null,
      timestamp: new Date().toISOString(),
    });
  });

  app.post("/admin/web-channel/invoke", requireAdmin, async (req, res) => {
    try {
      if (req.adminAuthMode !== "web_channel") {
        return res.status(403).json({
          error: "invoke_requires_web_channel",
          message: "Der Invoke-Endpunkt ist nur für das SimplestSoft-Admin-Backend (HMAC) vorgesehen.",
        });
      }

      const method = String(req.body?.method || "GET").trim().toUpperCase();
      const path = normalizeInvokePath(req.body?.path);
      const query = req.body?.query;
      const body = req.body?.body;

      if (!["GET", "POST", "PUT", "DELETE", "PATCH"].includes(method)) {
        return res.status(400).json({ error: "invalid_method" });
      }
      if (!path) {
        return res.status(400).json({ error: "invalid_path", message: "Pfad muss mit /admin/ beginnen" });
      }
      if (!isInvokePathAllowed(path)) {
        return res.status(403).json({ error: "path_not_allowed", path });
      }

      const secret = resolveAdminWebChannelSecret();
      if (!secret) {
        return res.status(503).json({ error: "web_channel_not_configured" });
      }

      const queryString = buildInvokeQueryString(query);
      const targetPath = `${path}${queryString}`;
      const bodyString =
        body == null || method === "GET" || method === "DELETE" ? "" : JSON.stringify(body);
      const headers = createAdminWebChannelSignatureHeaders(secret, {
        method,
        path,
        body: bodyString,
        actingUserId: req.adminUserId,
        appId: req.adminChannelAppId || APP_ID,
      });

      const response = await fetch(`${resolveApiBaseUrl(req)}${targetPath}`, {
        method,
        headers,
        body: bodyString.length > 0 ? bodyString : undefined,
      });

      const text = await response.text();
      let parsed;
      try {
        parsed = text.length > 0 ? JSON.parse(text) : null;
      } catch {
        parsed = { raw: text.slice(0, 2000) };
      }

      res.status(response.status).json({
        invoked: true,
        method,
        path: targetPath,
        status: response.status,
        data: parsed,
      });
    } catch (error) {
      console.error("POST /admin/web-channel/invoke:", error);
      res.status(500).json({ error: error.message || "Invoke fehlgeschlagen" });
    }
  });
}

module.exports = { registerAdminWebChannelRoutes };
