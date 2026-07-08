import UIKit

// MARK: - Share Card Style

enum ShareCardStyle: String, CaseIterable, Identifiable {
    case minimal   // 极简
    case film      // 胶片
    case magazine  // 杂志
    case polaroid  // 拍立得

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .minimal: return "极简"
        case .film: return "胶片"
        case .magazine: return "杂志"
        case .polaroid: return "拍立得"
        }
    }

    var iconName: String {
        switch self {
        case .minimal: return "circle.lefthalf.filled"
        case .film: return "film"
        case .magazine: return "book.pages"
        case .polaroid: return "rectangle.on.rectangle"
        }
    }
}

// MARK: - Share Card Generator

enum ShareCardGenerator {

    // MARK: - Constants

    private static let cardWidth: CGFloat = 1080
    private static let cardAspectRatio: CGFloat = 3.0 / 4.0
    private static var cardHeight: CGFloat { cardWidth / cardAspectRatio }
    private static let cornerRadius: CGFloat = 24
    private static let photoInsetHorizontal: CGFloat = 80
    private static let photoInsetVertical: CGFloat = 72
    private static let topPadding: CGFloat = 120
    private static let bottomReserved: CGFloat = 300
    private static let maxPhotoDimension: CGFloat = 1920

    // MARK: - Photo Scaling

    private static func scaledPhoto(from photo: UIImage) -> UIImage? {
        let size = photo.size
        guard size.width > 0, size.height > 0 else { return nil }
        let maxDim = max(size.width, size.height)
        guard maxDim > maxPhotoDimension else { return photo }
        let ratio = maxPhotoDimension / maxDim
        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        UIGraphicsBeginImageContextWithOptions(newSize, true, 1.0)
        defer { UIGraphicsEndImageContext() }
        photo.draw(in: CGRect(origin: .zero, size: newSize))
        return UIGraphicsGetImageFromCurrentImageContext() ?? photo
    }

    private static func loadLogo() -> UIImage? {
        return UIImage(named: "logo-glass-LiveCompose")
    }

    // MARK: - Public API

    /// Generate a share card with a specific style.
    static func generate(
        photo: UIImage,
        style: ShareCardStyle = .minimal,
        date: Date = Date(),
        detectionMethod: String? = nil,
        iso: Float? = nil,
        shutterSpeed: Double? = nil,
        aperture: Double? = nil,
        imageWidth: Int? = nil,
        imageHeight: Int? = nil
    ) -> UIImage? {
        guard let photo = scaledPhoto(from: photo) else { return nil }

        switch style {
        case .minimal:
            return generateMinimal(photo: photo, date: date, detectionMethod: detectionMethod,
                                   iso: iso, shutterSpeed: shutterSpeed, aperture: aperture,
                                   imageWidth: imageWidth, imageHeight: imageHeight)
        case .film:
            return generateFilm(photo: photo, date: date, detectionMethod: detectionMethod,
                                iso: iso, shutterSpeed: shutterSpeed, aperture: aperture,
                                imageWidth: imageWidth, imageHeight: imageHeight)
        case .magazine:
            return generateMagazine(photo: photo, date: date, detectionMethod: detectionMethod,
                                    iso: iso, shutterSpeed: shutterSpeed, aperture: aperture,
                                    imageWidth: imageWidth, imageHeight: imageHeight)
        case .polaroid:
            return generatePolaroid(photo: photo, date: date, detectionMethod: detectionMethod,
                                    iso: iso, shutterSpeed: shutterSpeed, aperture: aperture,
                                    imageWidth: imageWidth, imageHeight: imageHeight)
        }
    }

    /// Legacy compatibility: generate using default (minimal) style.
    static func generate(
        photo: UIImage,
        date: Date = Date(),
        detectionMethod: String? = nil,
        iso: Float? = nil,
        shutterSpeed: Double? = nil,
        aperture: Double? = nil,
        imageWidth: Int? = nil,
        imageHeight: Int? = nil
    ) -> UIImage? {
        return generate(photo: photo, style: .minimal, date: date,
                        detectionMethod: detectionMethod, iso: iso,
                        shutterSpeed: shutterSpeed, aperture: aperture,
                        imageWidth: imageWidth, imageHeight: imageHeight)
    }

    /// Generate a small preview thumbnail for the style picker.
    static func generatePreview(
        photo: UIImage,
        style: ShareCardStyle,
        size: CGSize = CGSize(width: 160, height: 213)
    ) -> UIImage? {
        guard let photo = scaledPhoto(from: photo) else { return nil }

        let originalWidth = cardWidth
        let originalHeight = cardHeight
        let scale = size.width / originalWidth

        let previewCardWidth = size.width
        let previewCardHeight = originalHeight * scale

        // Temporarily override constants for preview
        let saved: (CGFloat, CGFloat, CGFloat, CGFloat, CGFloat, CGFloat) =
            (cardWidth, cardHeight, topPadding, bottomReserved, photoInsetHorizontal, photoInsetVertical)

        // We can't override static let, so we generate a full-size card and scale down
        guard let fullCard = generate(photo: photo, style: style) else { return nil }

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: size, format: format)

        return renderer.image { _ in
            fullCard.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    // MARK: - Style: 极简 Minimal

    private static func generateMinimal(
        photo: UIImage, date: Date, detectionMethod: String?, iso: Float?,
        shutterSpeed: Double?, aperture: Double?, imageWidth: Int?, imageHeight: Int?
    ) -> UIImage? {
        let cardSize = CGSize(width: cardWidth, height: cardHeight)
        let photoAreaWidth = cardWidth - photoInsetHorizontal * 2
        let photoSize = photo.size
        let photoAspect = photoSize.width / photoSize.height

        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        let maxPhotoHeight = cardHeight - topPadding - bottomReserved
        if drawHeight > maxPhotoHeight {
            drawHeight = maxPhotoHeight
            drawWidth = drawHeight * photoAspect
        }

        let photoRect = CGRect(
            x: (cardWidth - drawWidth) / 2,
            y: topPadding + (maxPhotoHeight - drawHeight) / 2,
            width: drawWidth,
            height: drawHeight
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: cardSize, format: format)

        let logo = loadLogo()
        let dateStr = formattedDate(date)

        return renderer.image { ctx in
            let cardRect = CGRect(origin: .zero, size: cardSize)

            UIColor.white.setFill()
            UIBezierPath(roundedRect: cardRect, cornerRadius: cornerRadius).fill()

            // Photo area
            UIColor.white.setFill()
            let photoBgPath = UIBezierPath(roundedRect: photoRect, cornerRadius: 8)
            photoBgPath.fill()

            ctx.cgContext.saveGState()
            photoBgPath.addClip()
            photo.draw(in: photoRect)
            ctx.cgContext.restoreGState()

            UIColor(white: 0.88, alpha: 1).setStroke()
            photoBgPath.lineWidth = 1
            photoBgPath.stroke()

            // Bottom branding
            let bottomY = photoRect.maxY + 36
            if let logo {
                let logoSize: CGFloat = 56
                let logoRect = CGRect(x: (cardWidth - logoSize) / 2, y: bottomY, width: logoSize, height: logoSize)
                logo.draw(in: logoRect)
            }

            let titleY = bottomY + 64
            drawTitle("构妙 · LiveCompose", at: CGPoint(x: cardWidth / 2, y: titleY), fontSize: 34)
            drawDate(dateStr, at: CGPoint(x: cardWidth / 2, y: titleY + 44), fontSize: 22)

            let paramsY = titleY + 44 + 42
            drawParams(detectionMethod: detectionMethod, iso: iso, shutterSpeed: shutterSpeed,
                       aperture: aperture, imageWidth: imageWidth, imageHeight: imageHeight,
                       at: CGPoint(x: cardWidth / 2, y: paramsY))

            drawBottomLine(at: paramsY + 48)
        }
    }

    // MARK: - Style: 胶片 Film

    private static func generateFilm(
        photo: UIImage, date: Date, detectionMethod: String?, iso: Float?,
        shutterSpeed: Double?, aperture: Double?, imageWidth: Int?, imageHeight: Int?
    ) -> UIImage? {
        let cardSize = CGSize(width: cardWidth, height: cardHeight)
        let filmBorder: CGFloat = 40
        let sprocketHoleRadius: CGFloat = 6
        let sprocketSpacing: CGFloat = 40
        let photoAreaWidth = cardWidth - filmBorder * 2
        let photoAreaHeight = cardHeight - filmBorder * 2 - 200

        let photoSize = photo.size
        let photoAspect = photoSize.width / photoSize.height
        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        if drawHeight > photoAreaHeight {
            drawHeight = photoAreaHeight
            drawWidth = drawHeight * photoAspect
        }

        let photoRect = CGRect(
            x: (cardWidth - drawWidth) / 2,
            y: filmBorder + (photoAreaHeight - drawHeight) / 2,
            width: drawWidth,
            height: drawHeight
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: cardSize, format: format)

        let dateStr = formattedDate(date)

        return renderer.image { ctx in
            let cardRect = CGRect(origin: .zero, size: cardSize)

            // Dark film background
            UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1).setFill()
            UIBezierPath(roundedRect: cardRect, cornerRadius: cornerRadius).fill()

            // Sprocket holes
            UIColor(white: 0.25, alpha: 1).setFill()
            for y in stride(from: filmBorder + 20, to: cardHeight - filmBorder - 20, by: sprocketSpacing) {
                let leftHole = CGRect(x: filmBorder / 2 - sprocketHoleRadius,
                                       y: y - sprocketHoleRadius,
                                       width: sprocketHoleRadius * 2,
                                       height: sprocketHoleRadius * 2)
                let rightHole = CGRect(x: cardWidth - filmBorder / 2 - sprocketHoleRadius,
                                        y: y - sprocketHoleRadius,
                                        width: sprocketHoleRadius * 2,
                                        height: sprocketHoleRadius * 2)
                UIBezierPath(roundedRect: leftHole, cornerRadius: sprocketHoleRadius).fill()
                UIBezierPath(roundedRect: rightHole, cornerRadius: sprocketHoleRadius).fill()
            }

            // Photo on white backing
            let photoBgPath = UIBezierPath(roundedRect: photoRect.insetBy(dx: -8, dy: -8), cornerRadius: 4)
            UIColor.white.setFill()
            photoBgPath.fill()

            ctx.cgContext.saveGState()
            UIBezierPath(roundedRect: photoRect, cornerRadius: 2).addClip()
            photo.draw(in: photoRect)
            ctx.cgContext.restoreGState()

            // Bottom info
            let infoY = photoRect.maxY + 24
            drawTitle("构妙 · LiveCompose", at: CGPoint(x: cardWidth / 2, y: infoY), fontSize: 28, color: .white)
            drawDate(dateStr, at: CGPoint(x: cardWidth / 2, y: infoY + 36), fontSize: 18, color: UIColor(white: 0.7, alpha: 1))

            let paramsY = infoY + 36 + 32
            drawParams(detectionMethod: detectionMethod, iso: iso, shutterSpeed: shutterSpeed,
                       aperture: aperture, imageWidth: imageWidth, imageHeight: imageHeight,
                       at: CGPoint(x: cardWidth / 2, y: paramsY), color: UIColor(white: 0.6, alpha: 1))
        }
    }

    // MARK: - Style: 杂志 Magazine

    private static func generateMagazine(
        photo: UIImage, date: Date, detectionMethod: String?, iso: Float?,
        shutterSpeed: Double?, aperture: Double?, imageWidth: Int?, imageHeight: Int?
    ) -> UIImage? {
        let cardSize = CGSize(width: cardWidth, height: cardHeight)
        let headerHeight: CGFloat = 200
        let photoAreaWidth = cardWidth - 60
        let photoAreaHeight = cardHeight - headerHeight - 200

        let photoSize = photo.size
        let photoAspect = photoSize.width / photoSize.height
        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        if drawHeight > photoAreaHeight {
            drawHeight = photoAreaHeight
            drawWidth = drawHeight * photoAspect
        }

        let photoRect = CGRect(
            x: (cardWidth - drawWidth) / 2,
            y: headerHeight + (photoAreaHeight - drawHeight) / 2,
            width: drawWidth,
            height: drawHeight
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: cardSize, format: format)

        let logo = loadLogo()
        let dateStr = formattedDate(date)

        return renderer.image { ctx in
            let cardRect = CGRect(origin: .zero, size: cardSize)

            // Warm beige background
            UIColor(red: 0.98, green: 0.96, blue: 0.92, alpha: 1).setFill()
            UIBezierPath(roundedRect: cardRect, cornerRadius: cornerRadius).fill()

            // Header area with accent bar
            UIColor(red: 0.85, green: 0.30, blue: 0.25, alpha: 1).setFill()
            UIBezierPath(rect: CGRect(x: 0, y: 0, width: cardWidth, height: 8)).fill()

            // Magazine header
            let headerFont = UIFont(name: "Georgia-Bold", size: 48) ?? UIFont.systemFont(ofSize: 48, weight: .bold)
            let headerAttr: [NSAttributedString.Key: Any] = [
                .font: headerFont,
                .foregroundColor: UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1)
            ]
            let headerText = "构妙"
            let headerSize = headerText.size(withAttributes: headerAttr)
            headerText.draw(
                at: CGPoint(x: (cardWidth - headerSize.width) / 2, y: 40),
                withAttributes: headerAttr
            )

            let subFont = UIFont.systemFont(ofSize: 16, weight: .regular)
            let subAttr: [NSAttributedString.Key: Any] = [
                .font: subFont,
                .foregroundColor: UIColor(white: 0.4, alpha: 1),
                .kern: 4.0
            ]
            let subText = "LIVECAPTURE"
            let subSize = subText.size(withAttributes: subAttr)
            subText.draw(
                at: CGPoint(x: (cardWidth - subSize.width) / 2, y: 40 + headerSize.height + 4),
                withAttributes: subAttr
            )

            // Decorative line under header
            UIColor(red: 0.85, green: 0.30, blue: 0.25, alpha: 1).setStroke()
            let headerLine = UIBezierPath()
            headerLine.move(to: CGPoint(x: cardWidth * 0.35, y: 40 + headerSize.height + subSize.height + 14))
            headerLine.addLine(to: CGPoint(x: cardWidth * 0.65, y: 40 + headerSize.height + subSize.height + 14))
            headerLine.lineWidth = 1.5
            headerLine.stroke()

            // Photo
            ctx.cgContext.saveGState()
            UIBezierPath(roundedRect: photoRect, cornerRadius: 4).addClip()
            photo.draw(in: photoRect)
            ctx.cgContext.restoreGState()

            UIColor(white: 0.85, alpha: 1).setStroke()
            UIBezierPath(roundedRect: photoRect, cornerRadius: 4).lineWidth = 1
            UIBezierPath(roundedRect: photoRect, cornerRadius: 4).stroke()

            // Bottom info
            let infoY = photoRect.maxY + 24
            drawDate(dateStr, at: CGPoint(x: cardWidth / 2, y: infoY), fontSize: 18, color: UIColor(white: 0.4, alpha: 1), font: UIFont(name: "Georgia", size: 18) ?? UIFont.systemFont(ofSize: 18))

            let paramsY = infoY + 30
            drawParams(detectionMethod: detectionMethod, iso: iso, shutterSpeed: shutterSpeed,
                       aperture: aperture, imageWidth: imageWidth, imageHeight: imageHeight,
                       at: CGPoint(x: cardWidth / 2, y: paramsY), color: UIColor(white: 0.5, alpha: 1))

            // Footer
            if let logo {
                let logoSize: CGFloat = 40
                let logoRect = CGRect(x: (cardWidth - logoSize) / 2, y: cardHeight - 70, width: logoSize, height: logoSize)
                logo.draw(in: logoRect)
            }
        }
    }

    // MARK: - Style: 拍立得 Polaroid

    private static func generatePolaroid(
        photo: UIImage, date: Date, detectionMethod: String?, iso: Float?,
        shutterSpeed: Double?, aperture: Double?, imageWidth: Int?, imageHeight: Int?
    ) -> UIImage? {
        let cardSize = CGSize(width: cardWidth, height: cardHeight)
        let borderTop: CGFloat = 60
        let borderSide: CGFloat = 50
        let borderBottom: CGFloat = 100
        let photoAreaWidth = cardWidth - borderSide * 2
        let photoAreaHeight = cardHeight - borderTop - borderBottom

        let photoSize = photo.size
        let photoAspect = photoSize.width / photoSize.height
        var drawWidth = photoAreaWidth
        var drawHeight = drawWidth / photoAspect
        if drawHeight > photoAreaHeight {
            drawHeight = photoAreaHeight
            drawWidth = drawHeight * photoAspect
        }

        let photoRect = CGRect(
            x: (cardWidth - drawWidth) / 2,
            y: borderTop + (photoAreaHeight - drawHeight) / 2,
            width: drawWidth,
            height: drawHeight
        )

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: cardSize, format: format)

        let dateStr = formattedDate(date)

        return renderer.image { ctx in
            let cardRect = CGRect(origin: .zero, size: cardSize)

            // Polaroid white frame
            UIColor(red: 0.97, green: 0.97, blue: 0.95, alpha: 1).setFill()
            UIBezierPath(roundedRect: cardRect, cornerRadius: 8).fill()

            // Subtle shadow behind photo
            let shadowPath = UIBezierPath(roundedRect: photoRect.insetBy(dx: -6, dy: -6), cornerRadius: 2)
            UIColor(white: 0, alpha: 0.08).setFill()
            shadowPath.fill()

            // Photo
            ctx.cgContext.saveGState()
            UIBezierPath(roundedRect: photoRect, cornerRadius: 2).addClip()
            photo.draw(in: photoRect)
            ctx.cgContext.restoreGState()

            UIColor(white: 0.85, alpha: 1).setStroke()
            UIBezierPath(roundedRect: photoRect, cornerRadius: 2).lineWidth = 0.5
            UIBezierPath(roundedRect: photoRect, cornerRadius: 2).stroke()

            // Bottom polaroid area - hand-drawn feel
            let bottomAreaY = cardHeight - borderBottom + 12
            drawTitle("构妙 · LiveCompose", at: CGPoint(x: cardWidth / 2, y: bottomAreaY), fontSize: 26, color: UIColor(white: 0.25, alpha: 1))

            drawDate(dateStr, at: CGPoint(x: cardWidth / 2, y: bottomAreaY + 32), fontSize: 16, color: UIColor(white: 0.45, alpha: 1))
        }
    }

    // MARK: - Drawing Helpers

    private static func drawTitle(_ text: String, at center: CGPoint, fontSize: CGFloat, color: UIColor = .black, font: UIFont? = nil) {
        let titleFont = font ?? UIFont.systemFont(ofSize: fontSize, weight: .bold)
        let attr: [NSAttributedString.Key: Any] = [.font: titleFont, .foregroundColor: color]
        let size = text.size(withAttributes: attr)
        text.draw(
            in: CGRect(x: center.x - size.width / 2, y: center.y - size.height / 2, width: size.width, height: size.height),
            withAttributes: attr
        )
    }

    private static func drawDate(_ text: String, at center: CGPoint, fontSize: CGFloat, color: UIColor = UIColor(white: 0.4, alpha: 1), font: UIFont? = nil) {
        let dateFont = font ?? UIFont.systemFont(ofSize: fontSize, weight: .regular)
        let attr: [NSAttributedString.Key: Any] = [.font: dateFont, .foregroundColor: color]
        let size = text.size(withAttributes: attr)
        text.draw(
            in: CGRect(x: center.x - size.width / 2, y: center.y - size.height / 2, width: size.width, height: size.height),
            withAttributes: attr
        )
    }

    private static func drawParams(
        detectionMethod: String?, iso: Float?, shutterSpeed: Double?,
        aperture: Double?, imageWidth: Int?, imageHeight: Int?,
        at center: CGPoint, color: UIColor = UIColor(white: 0.5, alpha: 1)
    ) {
        var paramParts: [String] = []
        if let method = detectionMethod { paramParts.append(method) }
        if let iso { paramParts.append("ISO \(Int(iso))") }
        if let s = shutterSpeed { paramParts.append(shutterDisplay(s)) }
        if let a = aperture { paramParts.append("f/\(String(format: "%.1f", a))") }
        if let w = imageWidth, let h = imageHeight { paramParts.append("\(w)×\(h)") }

        guard !paramParts.isEmpty else { return }

        let paramsText = paramParts.joined(separator: "  ·  ")
        let paramsFont = UIFont.systemFont(ofSize: 20, weight: .regular)
        let paramsAttr: [NSAttributedString.Key: Any] = [.font: paramsFont, .foregroundColor: color]
        let paramsSize = paramsText.size(withAttributes: paramsAttr)
        paramsText.draw(
            in: CGRect(x: max(center.x - paramsSize.width / 2, photoInsetHorizontal),
                       y: center.y - paramsSize.height / 2,
                       width: min(paramsSize.width, cardWidth - photoInsetHorizontal * 2),
                       height: paramsSize.height),
            withAttributes: paramsAttr
        )
    }

    private static func drawBottomLine(at y: CGFloat) {
        let line = UIBezierPath()
        line.move(to: CGPoint(x: cardWidth * 0.25, y: y))
        line.addLine(to: CGPoint(x: cardWidth * 0.75, y: y))
        UIColor(white: 0.8, alpha: 1).setStroke()
        line.lineWidth = 1
        line.stroke()
    }

    // MARK: - Formatting

    private static func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy年M月d日 HH:mm"
        return formatter.string(from: date)
    }

    private static func shutterDisplay(_ speed: Double) -> String {
        if speed >= 1 { return "\(Int(speed))s" }
        else { return "1/\(Int(1.0 / speed))s" }
    }
}