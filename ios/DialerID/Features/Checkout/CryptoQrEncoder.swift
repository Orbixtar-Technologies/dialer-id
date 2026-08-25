import CoreImage
import CoreImage.CIFilterBuiltins
import SwiftUI
import UIKit

enum CryptoQrEncoder {
    static func image(from payload: String, size: CGFloat = 220) -> UIImage? {
        let data = Data(payload.utf8)
        let filter = CIFilter.qrCodeGenerator()
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage else { return nil }
        let scale = size / output.extent.width
        let scaled = output.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        let context = CIContext()
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}

struct CryptoQRView: View {
    let payload: String

    var body: some View {
        if let image = CryptoQrEncoder.image(from: payload) {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .accessibilityLabel("QR code for the payment address")
        }
    }
}
