"use strict";

const { normalizeCalculatorAvailability } = require("../lib/calculatorConfig");
const { normalizeProModules } = require("../lib/proModuleConfig");

/**
 * Public app configuration (no auth required).
 * @param {import("express").Express} app
 * @param {ReturnType<import("./createRouteContext").createRouteContext>} ctx
 */
function registerConfigRoutes(app, ctx) {
  const { db } = ctx;

  app.get("/config/calculators", async (_req, res) => {
    try {
      const mainDoc = await db.collection("appConfig").doc("main").get();
      const main = mainDoc.exists ? mainDoc.data() : {};
      res.json({
        calculators: normalizeCalculatorAvailability(main.calculatorAvailability),
      });
    } catch (error) {
      console.error("GET /config/calculators:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/config/pro-modules", async (_req, res) => {
    try {
      const mainDoc = await db.collection("appConfig").doc("main").get();
      const main = mainDoc.exists ? mainDoc.data() : {};
      const hasProModulesConfig = mainDoc.exists && main.proModules != null;
      res.json({
        modules: normalizeProModules(main.proModules, hasProModulesConfig),
      });
    } catch (error) {
      console.error("GET /config/pro-modules:", error);
      res.status(500).json({ error: error.message });
    }
  });

  app.get("/config/demo-vehicles", async (_req, res) => {
    try {
      const mainDoc = await db.collection("appConfig").doc("main").get();
      const main = mainDoc.exists ? mainDoc.data() : {};
      res.json({
        enabled: main.demoVehiclesEnabled !== false,
      });
    } catch (error) {
      console.error("GET /config/demo-vehicles:", error);
      res.status(500).json({ error: error.message });
    }
  });
}

module.exports = { registerConfigRoutes };
