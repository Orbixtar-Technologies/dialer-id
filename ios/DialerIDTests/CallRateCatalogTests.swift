import XCTest
@testable import DialerID

final class CallRateCatalogTests: XCTestCase {
    private let catalog = CallRateCatalog.fromRates([
        CallRate(name: "Canada Quebec Northeastern", prefix: "1418595", wholesaleRatePerMin: 0.0150),
        CallRate(name: "United Kingdom", prefix: "44", wholesaleRatePerMin: 0.0200),
        CallRate(name: "United Kingdom Mobile", prefix: "447", wholesaleRatePerMin: 0.0400),
        CallRate(name: "United States", prefix: "1", wholesaleRatePerMin: 0.0100)
    ])

    func testMarkupIsFortyPercent() {
        XCTAssertEqual(RateMarkup.rateMarkupPercent, 40)
        XCTAssertEqual(RateMarkup.rateMarkupMultiplier, 1.40, accuracy: 0.0001)
        XCTAssertEqual(RateMarkup.apply(0.0150), 0.0210, accuracy: 0.0001)
        XCTAssertEqual(catalog.lookup("14185951234")?.markedUpRatePerMin ?? 0, 0.0210, accuracy: 0.0001)
    }

    func testLongestPrefixWins() {
        XCTAssertEqual(catalog.lookup("+447911123456")?.name, "United Kingdom Mobile")
        XCTAssertEqual(catalog.lookup("+447911123456")?.prefix, "447")
        XCTAssertEqual(catalog.lookup("00442079460000")?.name, "United Kingdom")
        XCTAssertEqual(catalog.lookup("00442079460000")?.prefix, "44")
    }

    func testNanpTenDigitRetriesWithLeadingOne() {
        XCTAssertEqual(catalog.lookup("4185951234")?.name, "Canada Quebec Northeastern")
        XCTAssertEqual(catalog.lookup("4185951234")?.prefix, "1418595")
    }

    func testMissingRateReturnsNil() {
        XCTAssertNil(catalog.lookup("99999"))
        XCTAssertNil(catalog.lookup(""))
    }

    func testDebitChargesCeilWholeMinutesFromConnect() {
        let marked = RateMarkup.apply(0.0150)
        XCTAssertEqual(CallChargeCalculator.billedMinutes(durationSeconds: 0), 0)
        XCTAssertEqual(CallChargeCalculator.expectedMinutes(elapsedSeconds: 0), 1)
        XCTAssertEqual(CallChargeCalculator.expectedMinutes(elapsedSeconds: 59), 1)
        XCTAssertEqual(CallChargeCalculator.expectedMinutes(elapsedSeconds: 60), 1)
        XCTAssertEqual(CallChargeCalculator.expectedMinutes(elapsedSeconds: 61), 2)
        XCTAssertEqual(CallChargeCalculator.chargeUsd(durationSeconds: 0, markedUpRatePerMin: marked), 0, accuracy: 0.0001)
        XCTAssertEqual(CallChargeCalculator.chargeUsd(durationSeconds: 1, markedUpRatePerMin: marked), 0.0210, accuracy: 0.0001)
        XCTAssertEqual(CallChargeCalculator.chargeUsd(durationSeconds: 61, markedUpRatePerMin: marked), 0.0420, accuracy: 0.0001)
        XCTAssertEqual(CallChargeCalculator.minimumChargeUsd(markedUpRatePerMin: marked), 0.0210, accuracy: 0.0001)
    }

    func testParseJsonAppliesMarkupAndLookup() {
        let json = """
        [
          {"NAME":"Canada Quebec Northeastern","PREFIX":"1418595","RATE":"0.0150","INCREMENT":"6"},
          {"NAME":"United Kingdom","PREFIX":"44","RATE":"0.0200","INCREMENT":"6"}
        ]
        """
        let parsed = CallRateCatalog.fromJSON(json)
        XCTAssertEqual(parsed.lookup("+14185959999")?.markedUpRatePerMin ?? 0, 0.0210, accuracy: 0.0001)
        XCTAssertEqual(parsed.listedRates.first { $0.destination == "United Kingdom" }?.markedUpRatePerMin ?? 0, 0.0280, accuracy: 0.0001)
    }

    func testJsonPrefixPlusIsIndexedAsDigits() {
        let parsed = CallRateCatalog.fromJSON(
            """
            [{"NAME":"United States","PREFIX":"+1","RATE":"0.0100","INCREMENT":"6"}]
            """
        )
        XCTAssertEqual(parsed.lookup("+14155552671")?.name, "United States")
        XCTAssertEqual(parsed.lookup("0014155552671")?.prefix, "1")
    }

    func testNumericJsonRateIsParsed() {
        let parsed = CallRateCatalog.fromJSON(
            """
            [{"NAME":"Afghanistan","PREFIX":"93","RATE":0.273,"INCREMENT":60}]
            """
        )
        XCTAssertEqual(parsed.lookup("+93701234567")?.markedUpRatePerMin ?? 0, 0.3822, accuracy: 0.0001)
    }

    func testOutboundBillingGates() {
        XCTAssertEqual(
            OutboundBillingPolicy.evaluate(destination: "+14155552671", balanceUsd: 1, catalog: catalog),
            .allowed(catalog.lookup("+14155552671")!)
        )
        XCTAssertEqual(
            OutboundBillingPolicy.evaluate(destination: "99999", balanceUsd: 10, catalog: catalog),
            .missingRate
        )
        if case .insufficientCredit(let minimum) = OutboundBillingPolicy.evaluate(
            destination: "+14155552671",
            balanceUsd: 0,
            catalog: catalog
        ) {
            XCTAssertEqual(minimum, 0.0140, accuracy: 0.0001)
        } else {
            XCTFail("expected insufficient credit")
        }
    }
}
