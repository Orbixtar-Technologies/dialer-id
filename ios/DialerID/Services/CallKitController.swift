import AVFoundation
import CallKit
import Foundation

protocol CallKitControllerDelegate: AnyObject {
    func callKitDidStart(_ uuid: UUID)
    func callKitDidEnd(_ uuid: UUID)
    func callKitDidMute(_ muted: Bool)
}

final class CallKitController: NSObject, CXProviderDelegate {
    static let shared = CallKitController()

    weak var delegate: CallKitControllerDelegate?
    private let controller = CXCallController()
    private let provider: CXProvider
    private(set) var activeUUID: UUID?

    override init() {
        let config = CXProviderConfiguration()
        config.supportsVideo = false
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.phoneNumber]
        if #available(iOS 14.0, *) {
            config.includesCallsInRecents = true
        }
        provider = CXProvider(configuration: config)
        super.init()
        provider.setDelegate(self, queue: .main)
    }

    func startOutgoing(handle: String, uuid: UUID = UUID()) {
        activeUUID = uuid
        let action = CXStartCallAction(call: uuid, handle: CXHandle(type: .phoneNumber, value: handle))
        controller.request(CXTransaction(action: action)) { [weak self] error in
            if error != nil {
                self?.delegate?.callKitDidStart(uuid)
            }
        }
    }

    func reportOutgoingConnected() {
        guard let uuid = activeUUID else { return }
        provider.reportOutgoingCall(with: uuid, connectedAt: Date())
    }

    func reportOutgoingStarted() {
        guard let uuid = activeUUID else { return }
        provider.reportOutgoingCall(with: uuid, startedConnectingAt: Date())
    }

    func endCall() {
        guard let uuid = activeUUID else { return }
        let action = CXEndCallAction(call: uuid)
        controller.request(CXTransaction(action: action)) { _ in }
    }

    func providerDidReset(_ provider: CXProvider) {
        activeUUID = nil
        delegate?.callKitDidEnd(UUID())
    }

    func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        AudioSessionController.shared.activateForCall(speaker: false)
        delegate?.callKitDidStart(action.callUUID)
        action.fulfill()
    }

    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        delegate?.callKitDidEnd(action.callUUID)
        activeUUID = nil
        action.fulfill()
    }

    func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        delegate?.callKitDidMute(action.isMuted)
        action.fulfill()
    }

    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        AudioSessionController.shared.activateForCall(speaker: false)
    }
}
