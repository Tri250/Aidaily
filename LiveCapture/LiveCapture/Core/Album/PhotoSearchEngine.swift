//
//  PhotoSearchEngine.swift
//  LiveCapture
//
//  自然语言照片搜索引擎 - 基于 NaturalLanguage 框架的关键词提取和中文日期解析
//
//  ## 主要功能
//  - search: 自然语言搜索照片（支持中文日期、场景、地点等）
//  - extractDate: 从自然语言中提取日期范围
//  - extractSceneKeywords: 从查询中提取场景关键词
//

import Foundation
import NaturalLanguage

#if os(iOS)

/// 日期范围结构体
struct DateRange {
    let start: Date?
    let end: Date?

    /// 判断给定日期是否在范围内
    func contains(_ date: Date) -> Bool {
        if let start = start, date < start { return false }
        if let end = end, date > end { return false }
        return true
    }
}

/// 自然语言照片搜索引擎
final class PhotoSearchEngine {

    // MARK: - 场景关键词映射

    /// 场景关键词 → SceneType 映射
    private let sceneKeywords: [String: SceneType] = [
        // 美食
        "美食": .food, "食物": .food, "吃饭": .food, "餐厅": .food, "料理": .food,
        "火锅": .food, "烧烤": .food, "甜品": .food, "咖啡": .food, "奶茶": .food,
        // 风景
        "风景": .landscape, "山水": .landscape, "自然": .landscape, "森林": .landscape,
        "草原": .landscape, "湖泊": .landscape, "河流": .landscape, "山": .landscape,
        "天空": .landscape, "云": .landscape,
        // 人像
        "人像": .portrait, "人物": .portrait, "自拍": .portrait, "合照": .portrait,
        "合影": .portrait, "人": .portrait, "朋友": .portrait, "家人": .portrait,
        // 夜景
        "夜景": .nightScene, "晚上": .nightScene, "夜晚": .nightScene, "灯光": .nightScene,
        "霓虹": .nightScene, "星空": .nightScene,
        // 建筑
        "建筑": .architecture, "城市": .architecture, "高楼": .architecture, "大厦": .architecture,
        "街道": .architecture,
        // 街拍
        "街拍": .street, "街头": .street, "马路": .street,
        // 宠物
        "宠物": .pet, "猫": .pet, "狗": .pet, "动物": .pet, "猫咪": .pet, "狗狗": .pet,
        // 花卉
        "花": .flower, "花卉": .flower, "花朵": .flower, "樱花": .flower, "梅花": .flower,
        "荷花": .flower, "玫瑰": .flower,
        // 海滩
        "海滩": .beach, "海边": .beach, "大海": .beach, "沙滩": .beach, "海": .beach,
        "西湖": .beach, "湖": .beach,
        // 雪景
        "雪景": .snow, "雪": .snow, "下雪": .snow, "冬天": .snow,
        // 日出日落
        "日出": .sunrise, "日落": .sunrise, "夕阳": .sunrise, "黄昏": .sunrise, "晚霞": .sunrise,
        "朝霞": .sunrise, "黎明": .sunrise,
        // 室内
        "室内": .indoor, "家里": .indoor, "房间": .indoor,
        // 文档
        "文档": .document, "文字": .document, "书本": .document, "书籍": .document,
        // 舞台
        "舞台": .stage, "演出": .stage, "演唱会": .stage, "表演": .stage
    ]

    /// 地点关键词
    private let locationKeywords: Set<String> = [
        "北京", "上海", "广州", "深圳", "杭州", "成都", "南京", "武汉", "西安", "重庆",
        "苏州", "厦门", "青岛", "大连", "三亚", "香港", "澳门", "台北", "哈尔滨", "昆明",
        "拉萨", "乌鲁木齐", "海口", "桂林", "丽江", "张家界", "长沙", "郑州", "合肥",
        "济南", "天津", "福州", "南宁", "贵阳", "兰州", "银川", "西宁", "呼和浩特",
        "西湖", "故宫", "长城", "外滩", "东方明珠", "迪士尼", "环球影城", "故宫博物院",
        "颐和园", "天坛", "鸟巢", "水立方", "深圳湾", "广州塔", "中山陵", "夫子庙",
        "武大", "武大樱花", "大雁塔", "兵马俑", "解放碑", "洪崖洞", "鼓浪屿", "栈桥",
        "星海", "亚龙湾", "太平山", "大三巴", "日月潭", "阿里山", "冰雪大世界",
        "滇池", "石林", "布达拉宫", "大昭寺", "天山", "吐鲁番", "漓江", "阳朔",
        "黄山", "泰山", "华山", "峨眉山", "庐山", "武夷山", "张家界", "九寨沟"
    ]

    // MARK: - 搜索

    /// 自然语言搜索照片
    /// - Parameters:
    ///   - query: 自然语言查询字符串
    ///   - records: 照片记录列表
    /// - Returns: 匹配的照片记录列表（按相关性排序）
    func search(_ query: String, in records: [PhotoRecord]) -> [PhotoRecord] {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return records
        }

        let lowerQuery = query.lowercased()

        // 1. 提取日期范围
        let dateRange = extractDate(from: query)

        // 2. 提取场景关键词
        let sceneKeywords = extractSceneKeywords(from: query)

        // 3. 提取地点关键词
        let locations = extractLocationKeywords(from: query)

        // 4. 提取通用关键词
        let tokens = tokenize(query)
        let generalKeywords = tokens.filter { token in
            !sceneKeywords.contains(token) && !locations.contains(token)
        }

        var scoredRecords: [(record: PhotoRecord, score: Float)] = []

        for record in records {
            var score: Float = 0

            // 日期匹配
            if let dateRange = dateRange {
                if dateRange.contains(record.creationDate) {
                    score += 3.0  // 日期匹配权重最高
                }
            }

            // 场景关键词匹配
            for keyword in sceneKeywords {
                if let sceneType = self.sceneKeywords[keyword] {
                    let matchedScene = matchSceneType(record, sceneType: sceneType)
                    if matchedScene {
                        score += 1.5
                    }
                }
            }

            // 地点关键词匹配
            for location in locations {
                if matchLocation(record, location: location) {
                    score += 2.0
                }
            }

            // 通用关键词：匹配 detectionMethod
            if let method = record.detectionMethod {
                for keyword in generalKeywords {
                    if method.lowercased().contains(keyword.lowercased()) {
                        score += 0.5
                    }
                }
            }

            // 通用关键词：匹配 EXIF 数据
            if let iso = record.iso {
                if generalKeywords.contains("高iso") || generalKeywords.contains("暗光") {
                    if iso >= 1600 { score += 1.0 }
                }
            }

            if let shutter = record.shutterSpeed {
                if generalKeywords.contains("长曝光") || generalKeywords.contains("慢门") {
                    if shutter > 0.5 { score += 1.0 }
                }
                if generalKeywords.contains("高速") || generalKeywords.contains("抓拍") {
                    if shutter < 1.0 / 1000.0 { score += 1.0 }
                }
            }

            if let aperture = record.aperture {
                if generalKeywords.contains("大光圈") || generalKeywords.contains("虚化") {
                    if aperture < 2.8 { score += 1.0 }
                }
            }

            if score > 0 {
                scoredRecords.append((record: record, score: score))
            }
        }

        // 按分数降序排列
        scoredRecords.sort { $0.score > $1.score }

        return scoredRecords.map { $0.record }
    }

    // MARK: - 日期提取

    /// 从自然语言中提取日期范围
    /// - Parameter query: 查询字符串
    /// - Returns: 日期范围，如果无法解析则返回 nil
    private func extractDate(from query: String) -> DateRange? {
        let calendar = Calendar.current
        let now = Date()

        // 检测中文日期关键词
        if query.contains("今天") {
            let start = calendar.startOfDay(for: now)
            let end = calendar.date(byAdding: .day, value: 1, to: start)?.addingTimeInterval(-1)
            return DateRange(start: start, end: end)
        }

        if query.contains("昨天") {
            let yesterday = calendar.date(byAdding: .day, value: -1, to: now)!
            let start = calendar.startOfDay(for: yesterday)
            let end = calendar.date(byAdding: .day, value: 1, to: start)?.addingTimeInterval(-1)
            return DateRange(start: start, end: end)
        }

        if query.contains("前天") {
            let dayBefore = calendar.date(byAdding: .day, value: -2, to: now)!
            let start = calendar.startOfDay(for: dayBefore)
            let end = calendar.date(byAdding: .day, value: 1, to: start)?.addingTimeInterval(-1)
            return DateRange(start: start, end: end)
        }

        if query.contains("本周") || query.contains("这周") {
            guard let startOfWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)) else {
                return nil
            }
            let endOfWeek = calendar.date(byAdding: .day, value: 7, to: startOfWeek)?.addingTimeInterval(-1)
            return DateRange(start: startOfWeek, end: endOfWeek)
        }

        if query.contains("上周") {
            guard let startOfThisWeek = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)),
                  let startOfLastWeek = calendar.date(byAdding: .day, value: -7, to: startOfThisWeek) else {
                return nil
            }
            let endOfLastWeek = calendar.date(byAdding: .day, value: 7, to: startOfLastWeek)?.addingTimeInterval(-1)
            return DateRange(start: startOfLastWeek, end: endOfLastWeek)
        }

        if query.contains("本月") || query.contains("这个月") {
            guard let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) else {
                return nil
            }
            let endOfMonth = calendar.date(byAdding: .month, value: 1, to: startOfMonth)?.addingTimeInterval(-1)
            return DateRange(start: startOfMonth, end: endOfMonth)
        }

        if query.contains("上个月") || query.contains("上月") {
            guard let startOfThisMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)),
                  let startOfLastMonth = calendar.date(byAdding: .month, value: -1, to: startOfThisMonth) else {
                return nil
            }
            let endOfLastMonth = calendar.date(byAdding: .month, value: 1, to: startOfLastMonth)?.addingTimeInterval(-1)
            return DateRange(start: startOfLastMonth, end: endOfLastMonth)
        }

        if query.contains("去年") {
            let thisYear = calendar.component(.year, from: now)
            var components = DateComponents()
            components.year = thisYear - 1
            components.month = 1
            components.day = 1
            guard let startOfLastYear = calendar.date(from: components) else { return nil }
            components.year = thisYear
            guard let endOfLastYear = calendar.date(byAdding: .second, value: -1, to: startOfLastYear) else { return nil }
            return DateRange(start: startOfLastYear, end: endOfLastYear)
        }

        if query.contains("今年") {
            let thisYear = calendar.component(.year, from: now)
            var components = DateComponents()
            components.year = thisYear
            components.month = 1
            components.day = 1
            guard let startOfYear = calendar.date(from: components) else { return nil }
            let endOfYear = calendar.date(byAdding: .year, value: 1, to: startOfYear)?.addingTimeInterval(-1)
            return DateRange(start: startOfYear, end: endOfYear)
        }

        // 匹配 "2025年" 格式
        if let yearMatch = query.range(of: #"(\d{4})年"#, options: .regularExpression) {
            let yearStr = String(query[yearMatch]).replacingOccurrences(of: "年", with: "")
            if let year = Int(yearStr), year >= 2000 && year <= 2100 {
                var components = DateComponents()
                components.year = year
                components.month = 1
                components.day = 1
                guard let start = calendar.date(from: components) else { return nil }
                let end = calendar.date(byAdding: .year, value: 1, to: start)?.addingTimeInterval(-1)
                return DateRange(start: start, end: end)
            }
        }

        // 匹配 "2025年3月" 格式
        if let match = query.range(of: #"(\d{4})年(\d{1,2})月"#, options: .regularExpression) {
            let matchStr = String(query[match])
            if let yearRange = matchStr.range(of: #"\d{4}"#, options: .regularExpression),
               let monthRange = matchStr.range(of: #"(?<=年)\d{1,2}"#, options: .regularExpression) {
                if let year = Int(matchStr[yearRange]),
                   let month = Int(matchStr[monthRange]),
                   month >= 1 && month <= 12 {
                    var components = DateComponents()
                    components.year = year
                    components.month = month
                    components.day = 1
                    guard let start = calendar.date(from: components) else { return nil }
                    let end = calendar.date(byAdding: .month, value: 1, to: start)?.addingTimeInterval(-1)
                    return DateRange(start: start, end: end)
                }
            }
        }

        return nil
    }

    // MARK: - 场景关键词提取

    /// 从查询中提取场景关键词
    /// - Parameter query: 查询字符串
    /// - Returns: 匹配到的场景关键词数组
    private func extractSceneKeywords(from query: String) -> [String] {
        var found: [String] = []
        let lowerQuery = query.lowercased()

        for keyword in sceneKeywords.keys {
            if query.contains(keyword) || lowerQuery.contains(keyword.lowercased()) {
                found.append(keyword)
            }
        }
        return found
    }

    // MARK: - 地点关键词提取

    /// 从查询中提取地点关键词
    private func extractLocationKeywords(from query: String) -> [String] {
        var found: [String] = []
        for location in locationKeywords {
            if query.contains(location) {
                found.append(location)
            }
        }
        return found
    }

    // MARK: - 分词

    /// 使用 NLTokenizer 进行中文分词
    private func tokenize(_ text: String) -> [String] {
        let tokenizer = NLTokenizer(unit: .word)
        tokenizer.string = text
        tokenizer.setLanguage(.chinese)

        var tokens: [String] = []
        tokenizer.enumerateTokens(in: text.startIndex..<text.endIndex) { range, _ in
            let token = String(text[range])
            // 过滤掉单个字符的标点符号
            if token.count > 1 || Character(token).isLetter {
                tokens.append(token)
            }
            return true
        }
        return tokens
    }

    // MARK: - 匹配逻辑

    /// 匹配场景类型
    private func matchSceneType(_ record: PhotoRecord, sceneType: SceneType) -> Bool {
        // 基于 EXIF 数据进行场景匹配
        switch sceneType {
        case .nightScene:
            if let iso = record.iso, iso >= 1600 { return true }
            if let shutter = record.shutterSpeed, shutter > 0.5 { return true }
            return false
        case .landscape:
            if let iso = record.iso, iso < 400 { return true }
            if let shutter = record.shutterSpeed, shutter < 1.0 / 2000.0 { return true }
            return false
        case .portrait:
            if let aperture = record.aperture, aperture < 3.0 { return true }
            return false
        default:
            return true  // 对于无法精确匹配的场景，默认返回匹配
        }
    }

    /// 匹配地点
    private func matchLocation(_ record: PhotoRecord, location: String) -> Bool {
        // 尝试从照片中提取 GPS 数据匹配
        let storage = PhotoStorageService.shared
        guard let url = storage.photoURL(for: record.id),
              let data = try? Data(contentsOf: url),
              let source = CGImageSourceCreateWithData(data as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any],
              let gps = props[kCGImagePropertyGPSDictionary as String] as? [String: Any] else {
            return false
        }

        // 简单的城市关键词匹配：如果照片包含 GPS 数据，则认为可能匹配
        // 实际项目可通过 CLGeocoder 进行反向地理编码
        if let lat = gps[kCGImagePropertyGPSLatitude as String] as? Double,
           let lon = gps[kCGImagePropertyGPSLongitude as String] as? Double {
            // 判断是否在中国范围内（粗略）
            if lat >= 18 && lat <= 54 && lon >= 73 && lon <= 135 {
                return true
            }
        }

        return false
    }
}

#endif