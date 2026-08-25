import Foundation

struct CallRate: Equatable {
    var name: String
    var prefix: String
    var wholesaleRatePerMin: Double

    var markedUpRatePerMin: Double {
        RateMarkup.apply(wholesaleRatePerMin)
    }
}

struct ListedCallRate: Equatable, Identifiable {
    var destination: String
    var markedUpRatePerMin: Double

    var id: String { destination }

    var sectionLetter: String {
        guard let ch = destination.first?.uppercased(), ("A"..."Z").contains(ch) else {
            return "#"
        }
        return ch
    }
}

enum OutboundBillingDecision: Equatable {
    case allowed(CallRate)
    case missingRate
    case insufficientCredit(minimumChargeUsd: Double)
}

enum OutboundBillingPolicy {
    static func evaluate(
        destination: String,
        balanceUsd: Double,
        catalog: CallRateCatalog
    ) -> OutboundBillingDecision {
        guard let rate = catalog.lookup(destination) else { return .missingRate }
        let minimum = CallChargeCalculator.minimumChargeUsd(markedUpRatePerMin: rate.markedUpRatePerMin)
        if balanceUsd + 1e-9 < minimum {
            return .insufficientCredit(minimumChargeUsd: minimum)
        }
        return .allowed(rate)
    }
}
