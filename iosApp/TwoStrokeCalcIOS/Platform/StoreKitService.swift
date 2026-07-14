import Foundation
import StoreKit

@MainActor
final class StoreKitService {
    static let shared = StoreKitService()
    static let proProductId = "pro_version"

    private init() {}

    func loadProducts(appState: AppState) async {
        do {
            let products = try await Product.products(for: [Self.proProductId])
            appState.storeKitPrice = products.first?.displayPrice
        } catch {
            // App Store / StoreKit is unavailable in Simulator and browser testers (e.g. Appetize).
            appState.storeKitPrice = nil
        }
    }

    func purchasePro(appState: AppState) async throws -> Bool {
        let products = try await Product.products(for: [Self.proProductId])
        guard let product = products.first else {
            throw StoreKitError.productUnavailable
        }
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            let jws = verification.jwsRepresentation
            let verified = await SharedKitBridge.verifyProPurchase(
                token: jws,
                productId: Self.proProductId
            )
            await transaction.finish()
            return verified
        case .userCancelled:
            return false
        case .pending:
            return false
        @unknown default:
            return false
        }
    }

    func restorePurchases(appState: AppState) async throws -> Bool {
        var restored = false
        for await result in Transaction.currentEntitlements {
            let transaction = try checkVerified(result)
            if transaction.productID == Self.proProductId {
                let jws = result.jwsRepresentation
                restored = await SharedKitBridge.verifyProPurchase(
                    token: jws,
                    productId: Self.proProductId
                )
            }
        }
        return restored
    }

    static func friendlyMessage(for error: Error) -> String {
        if let storeKit = error as? StoreKitError {
            return storeKit.localizedDescription
        }
        let nsError = error as NSError
        if nsError.domain == NSURLErrorDomain || nsError.domain == "SKErrorDomain" {
            return L.t("settings_pro_billing_unavailable")
        }
        return error.localizedDescription
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw StoreKitError.verificationFailed
        case .verified(let safe):
            return safe
        }
    }
}

enum StoreKitError: LocalizedError {
    case productUnavailable
    case verificationFailed

    var errorDescription: String? {
        switch self {
        case .productUnavailable: return L.t("billing_product_unavailable")
        case .verificationFailed: return L.t("settings_pro_verification_failed")
        }
    }
}
