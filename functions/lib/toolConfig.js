"use strict";

const TOOL_IDS = [
  "RPM_TACHOMETER",
  "PORT_TIMING_ASSIST",
  "VIBRATION_ANALYZER",
  "GPS_DYNO",
  "COMMUNITY_SETUPS",
];

/**
 * @param {Record<string, boolean | undefined> | null | undefined} stored
 * @returns {Record<string, boolean>}
 */
function normalizeToolAvailability(stored) {
  const source = stored && typeof stored === "object" ? stored : {};
  const result = {};
  for (const id of TOOL_IDS) {
    result[id] = source[id] !== false;
  }
  return result;
}

/**
 * @param {Record<string, boolean | undefined> | null | undefined} patch
 * @returns {Record<string, boolean>}
 */
function mergeToolAvailability(patch) {
  if (!patch || typeof patch !== "object") {
    return normalizeToolAvailability();
  }
  const merged = {};
  for (const id of TOOL_IDS) {
    if (Object.prototype.hasOwnProperty.call(patch, id)) {
      merged[id] = patch[id] !== false;
    }
  }
  return merged;
}

module.exports = {
  TOOL_IDS,
  normalizeToolAvailability,
  mergeToolAvailability,
};
