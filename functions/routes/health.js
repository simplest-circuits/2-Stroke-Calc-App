function registerHealthRoutes(app, _ctx) {
  app.get("/health", (req, res) => {
    res.json({ ok: true, service: "strokecalc-api" });
  });
}

module.exports = { registerHealthRoutes };
