"use strict";

const { google } = require("googleapis");

const DEFAULT_PACKAGE_NAME = "com.simplestsoft.twostrokecalc";
const PRO_PRODUCT_ID = "pro_version";

/**
 * @returns {import("google-auth-library").JWT | import("google-auth-library").GoogleAuth | undefined}
 */
function createPlayAuthClient() {
  const json = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
  if (json) {
    const credentials = JSON.parse(json);
    return new google.auth.JWT({
      email: credentials.client_email,
      key: credentials.private_key,
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
  }
  return new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
}

/**
 * @param {{ packageName?: string, productId: string, purchaseToken: string }} params
 * @returns {Promise<boolean>}
 */
async function verifyProductPurchase(params) {
  const packageName = params.packageName || DEFAULT_PACKAGE_NAME;
  const { productId, purchaseToken } = params;
  if (!purchaseToken || !productId) {
    return false;
  }
  if (productId !== PRO_PRODUCT_ID) {
    return false;
  }

  const auth = createPlayAuthClient();
  const androidPublisher = google.androidpublisher({ version: "v3", auth });
  const response = await androidPublisher.purchases.products.get({
    packageName,
    productId,
    token: purchaseToken,
  });
  const data = response.data || {};
  return data.purchaseState === 0;
}

module.exports = {
  DEFAULT_PACKAGE_NAME,
  PRO_PRODUCT_ID,
  verifyProductPurchase,
};
