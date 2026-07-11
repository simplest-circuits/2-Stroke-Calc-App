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
 * @returns {import("googleapis").androidpublisher_v3.Androidpublisher}
 */
function createAndroidPublisher() {
  const auth = createPlayAuthClient();
  return google.androidpublisher({ version: "v3", auth });
}

/**
 * @param {{ packageName?: string, productId: string, purchaseToken: string }} params
 * @returns {Promise<import("googleapis").androidpublisher_v3.Schema$ProductPurchase>}
 */
async function getProductPurchase(params) {
  const packageName = params.packageName || DEFAULT_PACKAGE_NAME;
  const { productId, purchaseToken } = params;
  if (!purchaseToken || !productId) {
    throw new Error("purchaseToken und productId erforderlich");
  }
  if (productId !== PRO_PRODUCT_ID) {
    throw new Error("Ungueltiges Produkt");
  }

  const androidPublisher = createAndroidPublisher();
  const response = await androidPublisher.purchases.products.get({
    packageName,
    productId,
    token: purchaseToken,
  });
  return response.data || {};
}

/**
 * @param {{ packageName?: string, productId: string, purchaseToken: string, developerPayload?: string }} params
 * @returns {Promise<void>}
 */
async function acknowledgeProductPurchase(params) {
  const packageName = params.packageName || DEFAULT_PACKAGE_NAME;
  const { productId, purchaseToken, developerPayload } = params;
  if (!purchaseToken || !productId) {
    throw new Error("purchaseToken und productId erforderlich");
  }
  const androidPublisher = createAndroidPublisher();
  await androidPublisher.purchases.products.acknowledge({
    packageName,
    productId,
    token: purchaseToken,
    requestBody: developerPayload ? { developerPayload } : {},
  });
}

/**
 * @param {{ packageName?: string, startTimeMillis?: string|number, token?: string, maxResults?: number }} params
 * @returns {Promise<{ purchases: import("googleapis").androidpublisher_v3.Schema$VoidedPurchase[], nextPageToken: string|null }>}
 */
async function listVoidedPurchases(params = {}) {
  const packageName = params.packageName || DEFAULT_PACKAGE_NAME;
  const androidPublisher = createAndroidPublisher();
  const response = await androidPublisher.purchases.voidedpurchases.list({
    packageName,
    startTime: params.startTimeMillis != null ? String(params.startTimeMillis) : undefined,
    token: params.token || undefined,
    maxResults: params.maxResults || 1000,
    type: 0,
  });
  return {
    purchases: response.data?.voidedPurchases || [],
    nextPageToken: response.data?.tokenPagination?.nextPageToken || null,
  };
}

module.exports = {
  DEFAULT_PACKAGE_NAME,
  PRO_PRODUCT_ID,
  getProductPurchase,
  acknowledgeProductPurchase,
  listVoidedPurchases,
};
