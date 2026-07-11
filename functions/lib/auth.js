const { admin } = require("./firebase");

async function resolveAuthenticatedUid(req, res) {
  const authHeader = req.get("Authorization") || "";
  const m = authHeader.match(/^Bearer\s+(.+)$/i);
  if (m && m[1]) {
    try {
      const decoded = await admin.auth().verifyIdToken(m[1].trim());
      return decoded.uid;
    } catch (err) {
      console.warn("verifyIdToken:", err.message);
      res.status(401).json({ error: "Ungültiges oder abgelaufenes Token" });
      return null;
    }
  }
  res.status(401).json({ error: "Authorization: Bearer <Firebase ID-Token> erforderlich" });
  return null;
}

module.exports = { resolveAuthenticatedUid };
