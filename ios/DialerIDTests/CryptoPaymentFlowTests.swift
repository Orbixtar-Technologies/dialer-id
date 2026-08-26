import XCTest
@testable import DialerID

final class CryptoPaymentFlowTests: XCTestCase {
    func testGroupsNetworksUnderSharedCoinId() {
        let groups = CryptoCoinCatalog.buildGroups(
            availableTickers: ["usdterc20", "usdttrc20", "btc"],
            fullCurrencies: [
                NowFullCurrency(code: "usdterc20", name: "Tether", network: "eth"),
                NowFullCurrency(code: "usdttrc20", name: "Tether", network: "trx"),
                NowFullCurrency(code: "btc", name: "Bitcoin", network: "btc")
            ],
            minAmountsUsd: [
                "usdterc20": 5.0,
                "usdttrc20": 3.0,
                "btc": 10.0
            ]
        )
        let usdt = groups.first { $0.id == "usdt" }
        XCTAssertEqual(usdt?.networks.count, 2)
        XCTAssertEqual(usdt?.isEnabledForAmount(5.0), true)
        XCTAssertEqual(usdt?.isEnabledForAmount(2.0), false)
    }

    func testDisabledNetworkShowsMinReason() {
        let option = CryptoCoinCatalog.buildGroups(
            availableTickers: ["btc"],
            fullCurrencies: [],
            minAmountsUsd: ["btc": 12.5]
        ).first?.networks.first
        XCTAssertEqual(option?.disabledReason(5.0), "Min $12.50")
        XCTAssertEqual(option?.isEnabledForAmount(15.0), true)
    }

    func testPendingPaymentExpiresAfterValidityWindow() {
        let createdAt = Int64(Date().timeIntervalSince1970 * 1000) - PendingPayment.validityMs - 1
        let payment = PendingPayment(
            paymentId: "1",
            kind: .topUp,
            amountUsd: 25,
            payCurrency: "btc",
            network: "Bitcoin",
            payAddress: "addr",
            payAmount: 0.001,
            extraId: "",
            qrPayload: "addr",
            status: "waiting",
            createdAt: createdAt,
            expiresAt: createdAt + PendingPayment.validityMs
        )
        XCTAssertTrue(payment.isExpired)
        XCTAssertTrue(payment.isTerminal)
    }

    func testPendingPaymentFromCheckoutUsesNetworkAndQr() {
        let checkout = NowCheckout(
            paymentId: "99",
            payAddress: "abc",
            payAmount: 0.5,
            payCurrency: "eth",
            extraId: "",
            network: "Ethereum",
            paymentStatus: "waiting",
            orderId: "order-1",
            priceAmountUsd: 25,
            createdAt: 1_700_000_000_000
        )
        let pending = PendingPayment.fromCheckout(checkout, kind: .topUp, qrPayload: "0xabc")
        XCTAssertEqual(pending.paymentId, "99")
        XCTAssertEqual(pending.network, "Ethereum")
        XCTAssertEqual(pending.qrPayload, "0xabc")
        XCTAssertEqual(pending.expiresAt, checkout.createdAt + PendingPayment.validityMs)
    }

    func testParsesMinAmountFiatEquivalent() {
        let response = NowMinAmountResponse(
            currencyFrom: "usd",
            currencyTo: "btc",
            minAmount: 0.00005,
            fiatEquivalentUsd: 4.25
        )
        XCTAssertEqual(response.fiatEquivalentUsd, 4.25, accuracy: 0.001)
        XCTAssertEqual(response.minAmount, 0.00005, accuracy: 0.0000001)
        XCTAssertEqual(response.resolvedMinAmountUsd(), 4.25, accuracy: 0.001)
    }

    func testResolvedMinAmountUsdIgnoresCryptoMinAmountFallback() {
        let response = NowMinAmountResponse(
            currencyFrom: "usd",
            currencyTo: "btc",
            minAmount: 0.00005,
            fiatEquivalentUsd: 0
        )
        XCTAssertEqual(response.resolvedMinAmountUsd(), 0, accuracy: 0)
    }

    func testCryptoPayUriBitcoinAndRipple() {
        XCTAssertEqual(
            CryptoPayUri.encode(address: "abc", payCurrency: "btc", payAmount: 0.5, extraId: nil),
            "bitcoin:abc?amount=0.5"
        )
        XCTAssertEqual(
            CryptoPayUri.encode(address: "r1", payCurrency: "xrp", payAmount: 1, extraId: "99"),
            "ripple:r1?dt=99&amount=1"
        )
    }

    func testWalletCredit() {
        XCTAssertEqual(WalletCredit.applyIntendedTopUp(currentBalanceUsd: 1.25, intendedTopUpUsd: 25), 26.25, accuracy: 0.0001)
        XCTAssertEqual(WalletCredit.applyIntendedTopUp(currentBalanceUsd: 1.25, intendedTopUpUsd: -5), 1.25, accuracy: 0.0001)
    }

    func testDeviceFeePolicy() {
        XCTAssertEqual(DeviceFeePolicy.deviceFeeUsd, 50)
        XCTAssertTrue(DeviceFeePolicy.isConfirmedStatus("finished"))
        XCTAssertTrue(DeviceFeePolicy.isFailedStatus("expired"))
        XCTAssertTrue(DeviceFeePolicy.shouldChargeNewDevice(alreadyRegistered: false))
        XCTAssertFalse(DeviceFeePolicy.shouldChargeNewDevice(alreadyRegistered: true))
    }
}

final class CallerIdDisplayTests: XCTestCase {
    func testStripsSipUriToLocalPart() {
        XCTAssertEqual(CallerIdDisplay.publicIdentity("13251@sip.sipup.org"), "13251")
        XCTAssertEqual(CallerIdDisplay.publicIdentity("sip:13251@sip.sipup.org"), "13251")
        XCTAssertEqual(CallerIdDisplay.publicIdentity("<sip:13251@sip.sipup.org>"), "13251")
        XCTAssertEqual(CallerIdDisplay.publicIdentity("+18442469637"), "+18442469637")
    }

    func testHidesProviderHostAndBrandLabels() {
        XCTAssertEqual(CallerIdDisplay.publicIdentity("sip.sipup.org"), "")
        XCTAssertEqual(CallerIdDisplay.publicLabel("SIPUp line", fallbackNumber: "13251@sip.sipup.org"), "13251")
        XCTAssertEqual(CallerIdDisplay.publicLabel("Desk", fallbackNumber: "13251@sip.sipup.org"), "Desk")
    }

    func testRedactRemovesProviderFromSentences() {
        let cleaned = CallerIdDisplay.redactUserText("Registered with sip.sipup.org as 13251@sip.sipup.org")
        XCTAssertFalse(cleaned.lowercased().contains("sipup"))
        XCTAssertFalse(cleaned.contains("@"))
        XCTAssertTrue(cleaned.contains("Registered"))
    }
}
