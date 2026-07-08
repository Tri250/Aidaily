import SwiftUI

#if os(iOS)

/// Style picker for share cards with preview thumbnails.
struct ShareCardStylePicker: View {
    @Binding var selectedStyle: ShareCardStyle
    let photo: UIImage
    let record: PhotoRecord

    @State private var previews: [ShareCardStyle: UIImage] = [:]
    @State private var isGenerating = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("选择卡片风格")
                .font(DesignSystem.Typography.headline)
                .foregroundColor(DesignSystem.Colors.textPrimary)
                .padding(.horizontal, 16)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(ShareCardStyle.allCases) { style in
                        styleButton(for: style)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
        .onAppear {
            generatePreviews()
        }
    }

    // MARK: - Style Button

    private func styleButton(for style: ShareCardStyle) -> some View {
        let isSelected = selectedStyle == style

        return Button {
            withAnimation(DesignSystem.Animation.quick) {
                selectedStyle = style
            }
        } label: {
            VStack(spacing: 8) {
                // Preview thumbnail
                ZStack {
                    if let preview = previews[style] {
                        Image(uiImage: preview)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 72, height: 96)
                            .clipped()
                    } else {
                        RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                            .fill(DesignSystem.Colors.backgroundSecondary)
                            .frame(width: 72, height: 96)
                            .overlay {
                                if isGenerating {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: style.iconName)
                                        .font(.system(size: 20))
                                        .foregroundColor(DesignSystem.Colors.textTertiary)
                                }
                            }
                    }
                }
                .frame(width: 72, height: 96)
                .clipShape(RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small))
                .overlay(
                    RoundedRectangle(cornerRadius: DesignSystem.CornerRadius.small)
                        .strokeBorder(
                            isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.gray3,
                            lineWidth: isSelected ? 2.5 : 1
                        )
                )
                .shadow(
                    color: isSelected ? DesignSystem.Colors.primary.opacity(0.3) : Color.clear,
                    radius: 6, x: 0, y: 2
                )

                // Style name
                Text(style.displayName)
                    .font(DesignSystem.Typography.caption1)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundColor(isSelected ? DesignSystem.Colors.primary : DesignSystem.Colors.textSecondary)
                    .lineLimit(1)
            }
            .frame(width: 80)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Generate Previews

    private func generatePreviews() {
        guard !isGenerating else { return }
        isGenerating = true

        let previewSize = CGSize(width: 160, height: 213)

        DispatchQueue.global(qos: .userInitiated).async {
            var result: [ShareCardStyle: UIImage] = [:]

            for style in ShareCardStyle.allCases {
                let preview = ShareCardGenerator.generatePreview(
                    photo: photo,
                    style: style,
                    size: previewSize
                )
                result[style] = preview
            }

            DispatchQueue.main.async {
                self.previews = result
                self.isGenerating = false
            }
        }
    }
}

#endif