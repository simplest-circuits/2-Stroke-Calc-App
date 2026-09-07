"use strict";

const DEMO_VEHICLE_IDS = new Set([
  "admin-demo-vespa-px125",
  "admin-demo-puch-maxi",
]);

function isDemoVehicleId(id) {
  if (typeof id !== "string") return true;
  const trimmed = id.trim();
  if (!trimmed) return true;
  return trimmed.startsWith("admin-demo-") || DEMO_VEHICLE_IDS.has(trimmed);
}

module.exports = {
  DEMO_VEHICLE_IDS,
  isDemoVehicleId,
};
