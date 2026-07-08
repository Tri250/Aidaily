//
//  CommunityModels.swift
//  LiveCapture
//
//  社区与社交生态系统数据模型
//

import Foundation
import UIKit
import CoreLocation

#if os(iOS)

// MARK: - Codable CLLocationCoordinate2D 包装

struct CodableCoordinate: Codable, Equatable {
    let latitude: Double
    let longitude: Double

    init(_ coordinate: CLLocationCoordinate2D) {
        self.latitude = coordinate.latitude
        self.longitude = coordinate.longitude
    }

    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }

    var clLocation: CLLocation {
        CLLocation(latitude: latitude, longitude: longitude)
    }
}

// MARK: - 照片挑战

struct PhotoChallenge: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let description: String
    let theme: ChallengeTheme
    let startDate: Date
    let endDate: Date
    var userEntries: [ChallengeEntry]
    var isActive: Bool

    static func == (lhs: PhotoChallenge, rhs: PhotoChallenge) -> Bool {
        lhs.id == rhs.id
    }

    enum ChallengeTheme: String, CaseIterable, Codable {
        case portrait
        case landscape
        case food
        case street
        case night
        case nature
        case architecture
        case macro
        case blackAndWhite
        case minimalism
        case reflection
        case silhouette
        case pet
        case color
        case lightShadow
        case urban
        case vintage
        case water
        case sky

        var displayName: String {
            switch self {
            case .portrait: return "人像"
            case .landscape: return "风光"
            case .food: return "美食"
            case .street: return "街拍"
            case .night: return "夜景"
            case .nature: return "自然"
            case .architecture: return "建筑"
            case .macro: return "微距"
            case .blackAndWhite: return "黑白"
            case .minimalism: return "极简"
            case .reflection: return "倒影"
            case .silhouette: return "剪影"
            case .pet: return "宠物"
            case .color: return "色彩"
            case .lightShadow: return "光影"
            case .urban: return "城市"
            case .vintage: return "复古"
            case .water: return "水景"
            case .sky: return "天空"
            }
        }

        var iconName: String {
            switch self {
            case .portrait: return "person.crop.square"
            case .landscape: return "mountain.2"
            case .food: return "fork.knife"
            case .street: return "building.2"
            case .night: return "moon.stars"
            case .nature: return "leaf"
            case .architecture: return "building.columns"
            case .macro: return "ant"
            case .blackAndWhite: return "circle.lefthalf.filled"
            case .minimalism: return "circle.dotted"
            case .reflection: return "water.waves"
            case .silhouette: return "person.fill.viewfinder"
            case .pet: return "pawprint"
            case .color: return "paintpalette"
            case .lightShadow: return "sun.max"
            case .urban: return "building.2.crop.circle"
            case .vintage: return "clock.arrow.circlepath"
            case .water: return "drop"
            case .sky: return "cloud.sun"
            }
        }
    }
}

// MARK: - 挑战参赛作品

struct ChallengeEntry: Identifiable, Codable, Equatable {
    let id: String
    let photoFileName: String
    let submittedDate: Date
    let title: String
    var votes: Int
    var hasVoted: Bool

    static func == (lhs: ChallengeEntry, rhs: ChallengeEntry) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - 用户滤镜

struct UserFilter: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let creatorName: String
    let parameters: FilterParameters
    let previewImageName: String?
    let createdDate: Date
    let downloads: Int
    let category: FilterCategory
    let filterDescription: String

    static func == (lhs: UserFilter, rhs: UserFilter) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - 拍摄地点推荐

struct PhotoLocation: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let description: String
    let coordinate: CodableCoordinate
    let bestTime: String
    let tags: [String]
    let samplePhotoName: String?
    let difficulty: Difficulty

    static func == (lhs: PhotoLocation, rhs: PhotoLocation) -> Bool {
        lhs.id == rhs.id
    }

    enum Difficulty: String, Codable, CaseIterable {
        case easy
        case medium
        case hard

        var displayName: String {
            switch self {
            case .easy: return "轻松"
            case .medium: return "中等"
            case .hard: return "挑战"
            }
        }

        var color: String {
            switch self {
            case .easy: return "green"
            case .medium: return "orange"
            case .hard: return "red"
            }
        }
    }
}

#endif