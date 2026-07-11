"use strict";

const CALCULATOR_IDS = [
  "TIMING",
  "PORT_AREA",
  "IGNITION",
  "DC_CABLE",
  "FLUID",
  "COMPRESSION",
  "SQUISH_BAND",
  "MEAN_PRESSURE",
  "GEAR",
  "EXHAUST",
  "COUNTERWEIGHT",
  "VARIATOR_WEIGHT",
  "FUEL_MIX",
  "CARB_JET",
  "DYNO_INERTIA",
  "VEHICLE_DYNAMICS",
];

/**
 * @param {Record<string, boolean | undefined> | null | undefined} stored
 * @returns {Record<string, boolean>}
 */
function normalizeCalculatorAvailability(stored) {
  const source = stored && typeof stored === "object" ? { ...stored } : {};
  if (
    Object.prototype.hasOwnProperty.call(source, "ELECTROLYTE") &&
    !Object.prototype.hasOwnProperty.call(source, "FLUID")
  ) {
    source.FLUID = source.ELECTROLYTE;
  }
  const result = {};
  for (const id of CALCULATOR_IDS) {
    result[id] = source[id] !== false;
  }
  return result;
}

/**
 * @param {Record<string, boolean | undefined> | null | undefined} patch
 * @returns {Record<string, boolean>}
 */
function mergeCalculatorAvailability(patch) {
  if (!patch || typeof patch !== "object") {
    return normalizeCalculatorAvailability();
  }
  const merged = {};
  for (const id of CALCULATOR_IDS) {
    if (Object.prototype.hasOwnProperty.call(patch, id)) {
      merged[id] = patch[id] !== false;
    }
  }
  return merged;
}

module.exports = {
  CALCULATOR_IDS,
  normalizeCalculatorAvailability,
  mergeCalculatorAvailability,
};
