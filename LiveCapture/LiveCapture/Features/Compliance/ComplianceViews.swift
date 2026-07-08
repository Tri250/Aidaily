//
//  ComplianceViews.swift
//  LiveCapture
//
//  国内合规页面：隐私政策、用户协议、账号注销、青少年模式、ICP备案
//

import SwiftUI
import WebKit

#if os(iOS)

// MARK: - 隐私政策页面

struct PrivacyPolicyView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("隐私政策")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .padding(.bottom, 8)

                    Text("最后更新日期：2025年1月1日")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    section("一、我们收集的信息") {
                        Text("""
                        在您使用 LiveCapture（构妙）服务的过程中，我们可能会收集以下信息：

                        1. 相机权限：用于拍摄照片和视频，这是本应用的核心功能。
                        2. 相册权限：用于保存拍摄的照片和视频到您的设备相册。
                        3. 麦克风权限：仅在您录制视频时用于采集音频。
                        4. 位置信息：您可以选择在照片中添加位置标签。
                        5. 设备信息：包括设备型号、操作系统版本等，用于优化应用性能。
                        6. 崩溃日志：用于分析和修复应用问题。
                        """)
                    }

                    section("二、信息的使用") {
                        Text("""
                        我们收集的信息仅用于以下目的：
                        - 提供和改善我们的摄影服务
                        - 分析和优化应用性能
                        - 修复应用崩溃和错误
                        - 遵守法律法规要求

                        我们不会将您的个人信息用于任何未在本政策中说明的用途。
                        """)
                    }

                    section("三、信息的存储") {
                        Text("""
                        您的照片和视频数据存储在您的设备本地，我们不会上传到任何云端服务器。
                        我们仅在您的设备上处理数据，不会将您的照片传输到外部服务器。
                        所有 AI 检测和滤镜处理均在设备端完成。
                        """)
                    }

                    section("四、信息的共享") {
                        Text("""
                        我们不会与任何第三方分享您的个人信息，除非：
                        - 获得您的明确同意
                        - 法律法规要求
                        - 保护我们的合法权益
                        """)
                    }

                    section("五、您的权利") {
                        Text("""
                        根据《中华人民共和国个人信息保护法》，您享有以下权利：
                        - 查阅、复制您的个人信息
                        - 更正、补充您的个人信息
                        - 删除您的个人信息
                        - 撤回同意
                        - 注销账号

                        您可以在应用设置中行使上述权利，或通过客服联系我们。
                        """)
                    }

                    section("六、未成年人保护") {
                        Text("""
                        我们非常重视对未成年人个人信息的保护。如果您是未满 14 周岁的未成年人，请在法定监护人的陪同下使用本应用。
                        我们提供了青少年模式，家长可以设置使用时长限制和夜间禁用时段。
                        """)
                    }

                    section("七、联系我们") {
                        Text("""
                        如果您对本隐私政策有任何疑问或建议，请通过以下方式联系我们：
                        邮箱：privacy@livecapture.app
                        """)
                    }
                }
                .padding(20)
            }
            .navigationTitle("隐私政策")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }

    private func section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .fontWeight(.semibold)
            content()
                .font(.body)
                .foregroundColor(.primary)
                .lineSpacing(4)
        }
    }
}

// MARK: - 用户协议页面

struct UserAgreementView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("用户协议")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .padding(.bottom, 8)

                    Text("最后更新日期：2025年1月1日")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    section("一、服务条款的接受") {
                        Text("""
                        欢迎使用 LiveCapture（构妙）。通过下载、安装或使用本应用，您同意遵守本用户协议的所有条款和条件。
                        如果您不同意本协议的任何条款，请不要使用本应用。
                        """)
                    }

                    section("二、服务描述") {
                        Text("""
                        LiveCapture 是一款 AI 智能摄影助手应用，提供以下服务：
                        - AI 智能构图指导
                        - 实时滤镜和美颜效果
                        - 照片和视频拍摄
                        - 照片编辑和管理
                        """)
                    }

                    section("三、用户行为规范") {
                        Text("""
                        您在使用本应用时，同意不从事以下行为：
                        - 违反中华人民共和国法律法规
                        - 侵犯他人知识产权或隐私权
                        - 传播违法或不当内容
                        - 干扰本应用的正常运行
                        - 利用本应用进行任何非法活动
                        """)
                    }

                    section("四、知识产权") {
                        Text("""
                        本应用及其所有内容（包括但不限于代码、界面设计、图标、AI 模型）的知识产权归开发者所有。
                        未经开发者书面许可，您不得复制、修改、分发或创建衍生作品。
                        """)
                    }

                    section("五、免责声明") {
                        Text("""
                        本应用按"现状"提供，不提供任何明示或暗示的保证。
                        开发者不保证本应用无错误或无中断，也不对因使用本应用而产生的任何损失承担责任。
                        """)
                    }

                    section("六、协议修改") {
                        Text("""
                        我们保留随时修改本用户协议的权利。修改后的协议将在应用内公布，继续使用本应用即表示您接受修改后的协议。
                        """)
                    }
                }
                .padding(20)
            }
            .navigationTitle("用户协议")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }

    private func section(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .fontWeight(.semibold)
            content()
                .font(.body)
                .foregroundColor(.primary)
                .lineSpacing(4)
        }
    }
}

// MARK: - 账号注销页面

struct AccountDeletionView: View {
    @StateObject private var service = AccountDeletionService.shared
    @State private var showConfirmation = false
    @State private var showDeletionAlert = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                // 状态区域
                Section {
                    if service.deletionRequested {
                        HStack {
                            Image(systemName: "clock.badge.checkmark")
                                .foregroundColor(.orange)
                                .font(.title2)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("注销请求已提交")
                                    .font(.headline)
                                Text("冷静期剩余 \(service.remainingDays) 天")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    } else {
                        HStack {
                            Image(systemName: "person.crop.circle.badge.xmark")
                                .foregroundColor(.red)
                                .font(.title2)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("账号注销")
                                    .font(.headline)
                                Text("注销后将永久删除您的所有数据")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                // 注销说明
                Section("注销须知") {
                    Label("注销后所有照片数据将被永久删除", systemImage: "photo.on.rectangle.angled")
                    Label("注销后有 \(service.coolingOffDays) 天冷静期，期间可取消", systemImage: "clock")
                    Label("冷静期结束后自动执行数据删除", systemImage: "trash")
                    Label("删除后数据无法恢复，请提前备份", systemImage: "exclamationmark.triangle")
                }

                // 操作按钮
                Section {
                    if service.deletionRequested {
                        Button(role: .destructive) {
                            showDeletionAlert = true
                        } label: {
                            HStack {
                                Spacer()
                                Text("立即执行数据删除")
                                Spacer()
                            }
                        }
                        .disabled(service.remainingDays > 0)

                        Button {
                            service.cancelDeletion()
                        } label: {
                            HStack {
                                Spacer()
                                Text("取消注销请求")
                                Spacer()
                            }
                        }
                    } else {
                        Button(role: .destructive) {
                            showConfirmation = true
                        } label: {
                            HStack {
                                Spacer()
                                Text("请求注销账号")
                                Spacer()
                            }
                        }
                    }
                }

                // 数据导出
                Section("数据导出") {
                    Button {
                        if let url = service.exportPersonalData() {
                            let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                               let rootVC = windowScene.windows.first?.rootViewController {
                                rootVC.present(activityVC, animated: true)
                            }
                        }
                    } label: {
                        Label("导出个人数据", systemImage: "square.and.arrow.up")
                    }
                }
            }
            .navigationTitle("账号管理")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
            .alert("确认注销", isPresented: $showConfirmation) {
                Button("确认注销", role: .destructive) {
                    service.requestDeletion()
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("注销后将进入 \(service.coolingOffDays) 天冷静期，冷静期结束后所有数据将被永久删除。")
            }
            .alert("确认删除", isPresented: $showDeletionAlert) {
                Button("确认删除", role: .destructive) {
                    service.executeDataDeletion()
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("此操作不可撤销！所有照片、设置和缓存数据将被永久删除。")
            }
        }
    }
}

// MARK: - 青少年模式页面

struct YouthModeView: View {
    @StateObject private var youthManager = YouthModeManager.shared
    @State private var password = ""
    @State private var showPasswordSheet = false
    @State private var passwordAction: PasswordAction = .toggle

    enum PasswordAction {
        case toggle
        case setPassword
    }

    var body: some View {
        NavigationStack {
            List {
                // 状态
                Section {
                    HStack {
                        Image(systemName: youthManager.isYouthModeEnabled ? "lock.shield.fill" : "lock.shield")
                            .foregroundColor(youthManager.isYouthModeEnabled ? .green : .gray)
                            .font(.title2)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("青少年模式")
                                .font(.headline)
                            Text(youthManager.isYouthModeEnabled ? "已开启" : "已关闭")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                        Toggle("", isOn: $youthManager.isYouthModeEnabled)
                            .onChange(of: youthManager.isYouthModeEnabled) { _, newValue in
                                if newValue && !youthManager.hasSetPassword {
                                    passwordAction = .setPassword
                                    showPasswordSheet = true
                                }
                            }
                    }
                }

                if youthManager.isYouthModeEnabled {
                    // 时长限制
                    Section("每日使用时长限制") {
                        HStack {
                            Text("时长限制")
                            Spacer()
                            Picker("", selection: $youthManager.dailyTimeLimit) {
                                Text("30分钟").tag(30)
                                Text("45分钟").tag(45)
                                Text("60分钟").tag(60)
                                Text("90分钟").tag(90)
                                Text("120分钟").tag(120)
                            }
                        }

                        HStack {
                            Text("今日使用")
                            Spacer()
                            Text(formattedUsage)
                                .foregroundColor(youthManager.isDailyLimitExceeded ? .red : .secondary)
                        }
                    }

                    // 夜间禁用
                    Section("夜间禁用时段") {
                        HStack {
                            Text("开始时间")
                            Spacer()
                            Picker("", selection: $youthManager.nightBanStartHour) {
                                ForEach(20...23, id: \.self) { hour in
                                    Text("\(hour):00").tag(hour)
                                }
                            }
                        }

                        HStack {
                            Text("结束时间")
                            Spacer()
                            Picker("", selection: $youthManager.nightBanEndHour) {
                                ForEach(5...8, id: \.self) { hour in
                                    Text("\(hour):00").tag(hour)
                                }
                            }
                        }

                        if youthManager.isInNightBanPeriod {
                            Label("当前处于夜间禁用时段", systemImage: "moon.zzz.fill")
                                .foregroundColor(.orange)
                        }
                    }
                }

                // 密码设置
                Section {
                    Button {
                        passwordAction = .setPassword
                        showPasswordSheet = true
                    } label: {
                        Label("设置/修改密码", systemImage: "key")
                    }
                } footer: {
                    Text("密码用于保护青少年模式设置，请妥善保管")
                }
            }
            .navigationTitle("青少年模式")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showPasswordSheet) {
                PasswordSheetView(action: passwordAction) { password in
                    if youthManager.hasSetPassword {
                        return youthManager.verifyPassword(password)
                    } else {
                        youthManager.setPassword(password)
                        return true
                    }
                }
            }
        }
    }

    private var formattedUsage: String {
        let minutes = Int(youthManager.todayUsageSeconds / 60)
        if minutes < 60 {
            return "\(minutes) 分钟"
        } else {
            let hours = minutes / 60
            let mins = minutes % 60
            return "\(hours) 小时 \(mins) 分钟"
        }
    }
}

private struct PasswordSheetView: View {
    let action: YouthModeView.PasswordAction
    let onConfirm: (String) -> Bool
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showError = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "lock.shield")
                    .font(.system(size: 48))
                    .foregroundColor(.blue)
                    .padding(.top, 40)

                Text(action == .setPassword ? "设置密码" : "输入密码")
                    .font(.title2)
                    .fontWeight(.semibold)

                SecureField("请输入密码", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(.numberPad)
                    .padding(.horizontal, 40)

                if action == .setPassword {
                    SecureField("请确认密码", text: $confirmPassword)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                        .padding(.horizontal, 40)
                }

                if showError {
                    Text("密码错误或两次密码不一致")
                        .font(.caption)
                        .foregroundColor(.red)
                }

                Button {
                    if action == .setPassword {
                        guard password == confirmPassword, password.count >= 4 else {
                            showError = true
                            return
                        }
                    }
                    if onConfirm(password) {
                        dismiss()
                    } else {
                        showError = true
                    }
                } label: {
                    Text("确认")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .padding(.horizontal, 40)

                Spacer()
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("取消") { dismiss() }
                }
            }
        }
    }
}

// MARK: - ICP 备案展示

struct ICPFilingView: View {
    let info = ICPFilingInfo.fromBundle()

    var body: some View {
        VStack(spacing: 12) {
            if !info.icpNumber.isEmpty {
                Link(destination: URL(string: info.icpLink) ?? URL(string: "https://beian.miit.gov.cn")!) {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.checkered")
                            .font(.system(size: 10))
                        Text(info.icpNumber)
                            .font(.caption2)
                    }
                    .foregroundColor(.secondary)
                }
            }

            if let nsNumber = info.networkSecurityNumber, !nsNumber.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "lock.shield")
                        .font(.system(size: 10))
                    Text(nsNumber)
                        .font(.caption2)
                }
                .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - 个人信息收集清单视图

struct PersonalInfoCollectionView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(PersonalInfoCollection.allCategories, id: \.category.rawValue) { item in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.category.rawValue)
                                    .font(.headline)
                                Text(item.category.purpose)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text(item.category.isRequired ? "必要" : "可选")
                                .font(.caption)
                                .foregroundColor(item.category.isRequired ? .orange : .secondary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(
                                    Capsule()
                                        .fill(item.category.isRequired ? Color.orange.opacity(0.15) : Color.gray.opacity(0.1))
                                )
                        }
                    }
                } header: {
                    Text("我们收集以下个人信息以提供更好的服务")
                } footer: {
                    Text("标记为"必要"的权限是应用核心功能所需的，标记为"可选"的权限您可以自由选择是否授权。")
                }
            }
            .navigationTitle("个人信息收集清单")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}

#endif