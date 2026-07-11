"use strict";

const { CALCULATOR_IDS } = require("./calculatorConfig");

const PRO_MODULE_IDS = ["VEHICLES", ...CALCULATOR_IDS];

/** Default Pro-gated modules when admin has not configured proModules yet. */
const DEFAULT_PRO_MODULES = {
  VEHICLES: true,
  TIMING: true,
  DC_CABLE: true,
  FLUID: true,
  GEAR: true,
};

/**
 * @param {Record<string, boolean | undefined> | null | undefined} stored
 * @param {boolean} hasStoredConfig whether proModules was ever saved on appConfig/main
 * @returns {Record<string, boolean>}
 */
function normalizeProModules(stored, hasStoredConfig = false) {
  const source = stored && typeof stored === "object" ? { ...stored } : {};
  if (
    Object.prototype.hasOwnProperty.call(source, "ELECTROLYTE") &&
    !Object.prototype.hasOwnProperty.call(source, "FLUID")
  ) {
    source.FLUID = source.ELECTROLYTE;
  }
  const result = {};
  for (const id of PRO_MODULE_IDS) {
    if (Object.prototype.hasOwnProperty.call(source, id)) {
      result[id] = source[id] === true;
    } else if (!hasStoredConfig) {
      result[id] = DEFAULT_PRO_MODULES[id] === true;
    } else {
      result[id] = false;
    }
  }
  return result;
}

/**
 * @param {Record<string, boolean | undefined> | null | undefined} patch
 * @returns {Record<string, boolean>}
 */
function mergeProModules(patch) {
  if (!patch || typeof patch !== "object") {
    return {};
  }
  const merged = {};
  for (const id of PRO_MODULE_IDS) {
    if (Object.prototype.hasOwnProperty.call(patch, id)) {
      merged[id] = patch[id] === true;
    }
  }
  return merged;
}

module.exports = {
  PRO_MODULE_IDS,
  DEFAULT_PRO_MODULES,
  normalizeProModules,
  mergeProModules,
};
