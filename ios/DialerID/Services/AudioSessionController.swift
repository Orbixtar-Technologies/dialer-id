import AVFoundation

final class AudioSessionController {
    static let shared = AudioSessionController()

    func activateForCall(speaker: Bool) {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .allowBluetoothA2DP])
        try? session.overrideOutputAudioPort(speaker ? .speaker : .none)
        try? session.setActive(true, options: [])
    }

    func setSpeaker(_ enabled: Bool) {
        try? AVAudioSession.sharedInstance().overrideOutputAudioPort(enabled ? .speaker : .none)
    }

    func deactivate() {
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }
}
