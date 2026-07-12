import Foundation
import sharedKit

enum CatalogBootstrap {
    static func loadIfNeeded() {
        guard IosKoinInitKt.iosCatalogEntryCount() == 0 else { return }
        guard let url = Bundle.main.url(forResource: "vehicle_catalog", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let json = String(data: data, encoding: .utf8) else { return }
        IosKoinInitKt.iosSeedCatalogJson(raw: json)
    }

    static func syncInBackground() {
        Task {
            try? await IosKoinInitKt.iosEnsureCatalogSynced()
        }
    }
}
