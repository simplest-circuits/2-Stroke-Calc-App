"use strict";

const crypto = require("crypto");

const APP_ID = "2-stroke-calc";
const APP_NAME = "2-Stroke Calc";
const PACKAGE_NAME = "com.simplestsoft.twostrokecalc";
const ALLOWED_APP_IDS = new Set(["2-stroke-calc"]);
const ADMIN_WEB_ORIGINS = new Set([
  "https://simplestsoft.de",
  "https://www.simplestsoft.de",
]);
const SIGNATURE_MAX_SKEW_MS = 5 * 60 * 1000;

const HEADER_TIMESTAMP = "x-ss-admin-timestamp";
const HEADER_SIGNATURE = "x-ss-admin-signature";
const HEADER_USER_ID = "x-ss-admin-user-id";
const HEADER_APP_ID = "x-ss-admin-app-id";

function resolveAdminWebChannelSecret() {
  const configured = process.env.ADMIN_WEB_CHANNEL_SECRET;
  if (typeof configured === "string" && configured.trim().length >= 32) {
    return configured.trim();
  }
  return null;
}

function isAdminWebOrigin(origin) {
  return typeof origin === "string" && ADMIN_WEB_ORIGINS.has(origin.replace(/\/$/, ""));
}

function sha256Hex(value) {
  return crypto.createHash("sha256").update(value, "utf8").digest("hex");
}

function buildSignaturePayload({ timestamp, method, path, bodyHash }) {
  return `${timestamp}\n${method.toUpperCase()}\n${path}\n${bodyHash}`;
}

function signAdminWebChannelRequest(secret, { timestamp, method, path, bodyHash }) {
  const payload = buildSignaturePayload({ timestamp, method, path, bodyHash });
  return crypto.createHmac("sha256", secret).update(payload, "utf8").digest("hex");
}

function timingSafeEqualHex(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const left = Buffer.from(a, "utf8");
  const right = Buffer.from(b, "utf8");
  if (left.length !== right.length) return false;
  return crypto.timingSafeEqual(left, right);
}

function normalizeRequestPath(req) {
  const raw = typeof req.originalUrl === "string" ? req.originalUrl : req.url || "/";
  const pathOnly = raw.split("?")[0] || "/";
  return pathOnly.startsWith("/") ? pathOnly : `/${pathOnly}`;
}

function readHeader(req, name) {
  const value = req.get(name);
  return typeof value === "string" ? value.trim() : "";
}

/**
 * Verifies HMAC-signed requests from the central SimplestSoft admin backend.
 * @returns {{ actingUserId: string, appId: string|null, authMode: "web_channel" }|null}
 */
function verifyAdminWebChannelRequest(req) {
  const secret = resolveAdminWebChannelSecret();
  if (!secret) return null;

  const timestampRaw = readHeader(req, HEADER_TIMESTAMP);
  const signature = readHeader(req, HEADER_SIGNATURE);
  const actingUserId = readHeader(req, HEADER_USER_ID);
  const appId = readHeader(req, HEADER_APP_ID);

  if (!timestampRaw || !signature || !actingUserId) return null;
  if (appId && !ALLOWED_APP_IDS.has(appId)) return null;

  const timestamp = Number.parseInt(timestampRaw, 10);
  if (!Number.isFinite(timestamp)) return null;
  if (Math.abs(Date.now() - timestamp) > SIGNATURE_MAX_SKEW_MS) return null;

  const bodyString =
    req.rawBody != null
      ? (Buffer.isBuffer(req.rawBody) ? req.rawBody.toString("utf8") : String(req.rawBody))
      : "";
  const bodyHash = sha256Hex(bodyString);
  const expected = signAdminWebChannelRequest(secret, {
    timestamp: String(timestamp),
    method: req.method || "GET",
    path: normalizeRequestPath(req),
    bodyHash,
  });

  if (!timingSafeEqualHex(signature, expected)) return null;

  return {
    actingUserId,
    appId: appId || null,
    authMode: "web_channel",
  };
}

function createAdminWebChannelSignatureHeaders(secret, {
  method,
  path,
  body = "",
  actingUserId,
  timestamp = Date.now(),
  appId = APP_ID,
}) {
  const bodyString = typeof body === "string" ? body : JSON.stringify(body ?? {});
  const bodyHash = sha256Hex(bodyString);
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const signature = signAdminWebChannelRequest(secret, {
    timestamp: String(timestamp),
    method,
    path: normalizedPath,
    bodyHash,
  });

  return {
    [HEADER_TIMESTAMP]: String(timestamp),
    [HEADER_SIGNATURE]: signature,
    [HEADER_USER_ID]: actingUserId,
    [HEADER_APP_ID]: appId,
    "Content-Type": "application/json",
  };
}

module.exports = {
  APP_ID,
  APP_NAME,
  PACKAGE_NAME,
  ALLOWED_APP_IDS,
  ADMIN_WEB_ORIGINS,
  HEADER_TIMESTAMP,
  HEADER_SIGNATURE,
  HEADER_USER_ID,
  HEADER_APP_ID,
  resolveAdminWebChannelSecret,
  isAdminWebOrigin,
  sha256Hex,
  buildSignaturePayload,
  signAdminWebChannelRequest,
  verifyAdminWebChannelRequest,
  createAdminWebChannelSignatureHeaders,
  normalizeRequestPath,
};
