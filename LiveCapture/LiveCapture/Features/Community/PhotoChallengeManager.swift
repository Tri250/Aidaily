//
//  PhotoChallengeManager.swift
//  LiveCapture
//
//  照片挑战管理器 - 本地化挑战轮换
//

import Foundation
import Combine

#if os(iOS)

final class PhotoChallengeManager: ObservableObject {
    @Published var currentChallenge: PhotoChallenge?
    @Published var pastChallenges: [PhotoChallenge] = []
    @Published var upcomingChallenges: [PhotoChallenge] = []

    private let storageKey = "livecapture.challenges"
    private let weekInSeconds: TimeInterval = 7 * 24 * 60 * 60

    init() {
        loadChallenges()
        checkAndRotateChallenge()
    }

    // MARK: - 12 个内置挑战定义

    private func builtInChallenges() -> [PhotoChallenge] {
        let now = Date()
        let calendar = Calendar.current

        // 基准周：从当前周一开始
        var baseMonday = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now))!
        // 对齐到周一零点
        let weekday = calendar.component(.weekday, from: baseMonday)
        let daysToMonday = (weekday - calendar.firstWeekday + 7) % 7
        if daysToMonday > 0 {
            baseMonday = calendar.date(byAdding: .day, value: -daysToMonday, to: baseMonday)!
        }

        let challenges: [(title: String, description: String, theme: PhotoChallenge.ChallengeTheme)] = [
            ("光影猎人", "捕捉城市中令人惊叹的光影对比，让明暗交错的画面讲述故事", .lightShadow),
            ("极简之美", "少即是多，用最简洁的构图展现最纯粹的美感", .minimalism),
            ("色彩狂欢", "寻找城市中最鲜艳、最富冲击力的色彩组合", .color),
            ("老街故事", "漫步城市老街，用镜头记录市井生活的温度", .street),
            ("自然之美", "走进大自然，发现山川、湖泊、森林的壮丽景色", .nature),
            ("美食日记", "记录令人垂涎的美食瞬间，让味蕾通过视觉绽放", .food),
            ("城市天际线", "仰望城市建筑与天空的交汇，捕捉天际线的韵律", .architecture),
            ("微距世界", "放大微观世界，发现不易察觉的细节之美", .macro),
            ("黑白影像", "舍弃色彩，用纯粹的明暗讲述故事", .blackAndWhite),
            ("倒影之美", "寻找水面、镜面中的对称世界，捕捉倒影的梦幻", .reflection),
            ("剪影艺术", "利用逆光创造剪影，用轮廓诉说故事", .silhouette),
            ("宠物时光", "记录萌宠的可爱瞬间，分享生命中的温暖陪伴", .pet),
        ]

        var result: [PhotoChallenge] = []
        for (index, challenge) in challenges.enumerated() {
            let weekOffset = Double(index) * weekInSeconds
            let startDate = baseMonday.addingTimeInterval(weekOffset)
            let endDate = startDate.addingTimeInterval(weekInSeconds - 1)

            let isActive: Bool
            if index == 0 {
                // 最近一周的是当前挑战
                isActive = (now >= startDate && now <= endDate)
            } else {
                isActive = false
            }

            let photoChallenge = PhotoChallenge(
                id: "challenge_\(index)",
                title: challenge.title,
                description: challenge.description,
                theme: challenge.theme,
                startDate: startDate,
                endDate: endDate,
                userEntries: [],
                isActive: isActive
            )
            result.append(photoChallenge)
        }

        return result
    }

    // MARK: - 加载与轮换

    func loadChallenges() {
        // 尝试从 UserDefaults 加载
        if let saved = loadSavedChallenges() {
            let now = Date()
            let challenges = checkRotation(saved, now: now)
            let (current, upcoming, past) = categorizeChallenges(challenges, now: now)
            self.currentChallenge = current
            self.upcomingChallenges = upcoming
            self.pastChallenges = past
            return
        }

        // 首次加载：使用内置挑战
        let challenges = builtInChallenges()
        let now = Date()
        let (current, upcoming, past) = categorizeChallenges(challenges, now: now)
        self.currentChallenge = current
        self.upcomingChallenges = upcoming
        self.pastChallenges = past
        saveChallenges(challenges)
    }

    func checkAndRotateChallenge() {
        let challenges = loadSavedChallenges() ?? builtInChallenges()
        let now = Date()
        let rotated = checkRotation(challenges, now: now)
        let (current, upcoming, past) = categorizeChallenges(rotated, now: now)
        self.currentChallenge = current
        self.upcomingChallenges = upcoming
        self.pastChallenges = past
        saveChallenges(rotated)
    }

    private func checkRotation(_ challenges: [PhotoChallenge], now: Date) -> [PhotoChallenge] {
        var mutated = challenges
        for i in 0..<mutated.count {
            if now >= mutated[i].startDate && now <= mutated[i].endDate {
                mutated[i].isActive = true
            } else {
                mutated[i].isActive = false
            }
        }
        return mutated
    }

    private func categorizeChallenges(_ challenges: [PhotoChallenge], now: Date) -> (PhotoChallenge?, [PhotoChallenge], [PhotoChallenge]) {
        let current = challenges.first { $0.isActive }
        let upcoming = challenges.filter { $0.startDate > now }.sorted { $0.startDate < $1.startDate }
        let past = challenges.filter { $0.endDate < now }.sorted { $0.endDate > $1.endDate }
        return (current, upcoming, past)
    }

    // MARK: - 参赛

    func submitEntry(to challengeId: String, photoFileName: String, title: String) {
        var challenges = loadSavedChallenges() ?? builtInChallenges()
        guard let index = challenges.firstIndex(where: { $0.id == challengeId }) else { return }

        let entry = ChallengeEntry(
            id: UUID().uuidString,
            photoFileName: photoFileName,
            submittedDate: Date(),
            title: title,
            votes: 0,
            hasVoted: false
        )
        challenges[index].userEntries.append(entry)

        let now = Date()
        let (current, upcoming, past) = categorizeChallenges(challenges, now: now)
        self.currentChallenge = current
        self.upcomingChallenges = upcoming
        self.pastChallenges = past
        saveChallenges(challenges)
    }

    // MARK: - 投票

    func voteForEntry(_ entryId: String, in challengeId: String) {
        var challenges = loadSavedChallenges() ?? builtInChallenges()
        guard let challengeIndex = challenges.firstIndex(where: { $0.id == challengeId }),
              let entryIndex = challenges[challengeIndex].userEntries.firstIndex(where: { $0.id == entryId }),
              !challenges[challengeIndex].userEntries[entryIndex].hasVoted
        else { return }

        challenges[challengeIndex].userEntries[entryIndex].votes += 1
        challenges[challengeIndex].userEntries[entryIndex].hasVoted = true

        let now = Date()
        let (current, upcoming, past) = categorizeChallenges(challenges, now: now)
        self.currentChallenge = current
        self.upcomingChallenges = upcoming
        self.pastChallenges = past
        saveChallenges(challenges)
    }

    // MARK: - 排行榜

    func getLeaderboard(for challengeId: String) -> [ChallengeEntry] {
        let challenges = loadSavedChallenges() ?? builtInChallenges()
        guard let challenge = challenges.first(where: { $0.id == challengeId }) else { return [] }
        return challenge.userEntries.sorted { $0.votes > $1.votes }
    }

    // MARK: - 持久化

    private func saveChallenges(_ challenges: [PhotoChallenge]) {
        guard let data = try? JSONEncoder().encode(challenges) else { return }
        UserDefaults.standard.set(data, forKey: storageKey)
    }

    private func loadSavedChallenges() -> [PhotoChallenge]? {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let challenges = try? JSONDecoder().decode([PhotoChallenge].self, from: data)
        else { return nil }
        return challenges
    }
}

#endif