//
//  LocationRecommender.swift
//  LiveCapture
//
//  拍摄地点推荐系统 - 基于当前定位的拍照地点推荐
//

import Foundation
import CoreLocation
import Combine

#if os(iOS)

final class LocationRecommender: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var nearbyLocations: [PhotoLocation] = []
    @Published var allLocations: [PhotoLocation] = []
    @Published var currentLocation: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var isSearching = false

    private let locationManager = CLLocationManager()
    private var allLocationsCache: [PhotoLocation] = []

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        allLocations = loadLocationDatabase()
        allLocationsCache = allLocations
    }

    // MARK: - 定位控制

    func requestLocation() {
        authorizationStatus = locationManager.authorizationStatus
        switch authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.requestLocation()
        case .denied, .restricted:
            break
        @unknown default:
            break
        }
    }

    // MARK: - 查找附近拍摄点

    func findNearbySpots(within radius: CLLocationDistance = 5000) {
        guard let userLocation = currentLocation else {
            // 没有定位时，展示所有地点按名称排序
            nearbyLocations = allLocationsCache.sorted { $0.name < $1.name }
            return
        }

        isSearching = true
        defer { isSearching = false }

        let nearby = allLocationsCache.compactMap { location -> (PhotoLocation, CLLocationDistance)? in
            let loc = location.coordinate.clLocation
            let distance = userLocation.distance(from: loc)
            guard distance <= radius else { return nil }
            return (location, distance)
        }
        .sorted { $0.1 < $1.1 }
        .map { $0.0 }

        nearbyLocations = nearby
    }

    // MARK: - 内置拍摄地点数据库（30+ 中国摄影地点）

    private func loadLocationDatabase() -> [PhotoLocation] {
        return [
            // 北京
            PhotoLocation(id: "loc_01", name: "故宫角楼", description: "故宫西北角楼是北京最经典的摄影机位之一，护城河倒影与角楼交相辉映，四季皆宜", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 39.9163, longitude: 116.3860)), bestTime: "清晨或黄昏，光线柔和时拍摄最佳", tags: ["建筑", "历史", "倒影", "北京"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_02", name: "颐和园十七孔桥", description: "金光穿洞奇观闻名遐迩，冬至前后落日余晖穿过桥洞，美不胜收", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 39.9999, longitude: 116.2754)), bestTime: "冬至前后傍晚，金光穿洞奇观", tags: ["园林", "桥", "日落", "北京"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_03", name: "八达岭长城", description: "万里长城最壮丽的段落之一，蜿蜒于群山之巅，日出日落尤为壮观", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 40.3597, longitude: 116.0185)), bestTime: "秋季清晨日出，游客较少光影好", tags: ["长城", "山脉", "历史", "北京"], samplePhotoName: nil, difficulty: .medium),
            PhotoLocation(id: "loc_04", name: "天坛祈年殿", description: "天坛标志性建筑，三重檐攒尖顶，对称构图极为出片", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 39.8822, longitude: 116.4066)), bestTime: "晴天上午，蓝天下鎏金顶格外耀眼", tags: ["建筑", "历史", "对称", "北京"], samplePhotoName: nil, difficulty: .easy),

            // 上海
            PhotoLocation(id: "loc_05", name: "外滩", description: "黄浦江畔百年建筑群，与陆家嘴摩天大楼隔江相望，新旧对比强烈", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 31.2400, longitude: 121.4905)), bestTime: "傍晚蓝调时刻，灯光初上", tags: ["城市", "建筑", "夜景", "上海"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_06", name: "豫园", description: "明代江南园林，亭台楼阁错落有致，九曲桥与湖心亭是经典机位", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 31.2272, longitude: 121.4925)), bestTime: "清晨开园时游客较少", tags: ["园林", "古建筑", "倒影", "上海"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_07", name: "陆家嘴环形天桥", description: "仰望三件套的最佳位置，超广角仰拍极具视觉冲击力", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 31.2355, longitude: 121.5010)), bestTime: "夜晚灯光璀璨时", tags: ["城市", "建筑", "夜景", "上海"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_08", name: "朱家角古镇", description: "上海近郊水乡古镇，小桥流水，明清建筑保存完好", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 31.1080, longitude: 121.0580)), bestTime: "清晨或傍晚，游客较少的时段", tags: ["古镇", "水乡", "人文", "上海"], samplePhotoName: nil, difficulty: .easy),

            // 杭州
            PhotoLocation(id: "loc_09", name: "西湖断桥", description: "西湖十景之一，白堤起点，雷峰塔与保俶塔遥相呼应", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.2590, longitude: 120.1490)), bestTime: "清晨日出或雪后初晴", tags: ["湖泊", "桥", "园林", "杭州"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_10", name: "灵隐寺", description: "千年古刹，深山藏古寺，黄墙红柱与竹林相映成趣", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.2425, longitude: 120.0970)), bestTime: "清晨香火缭绕时分", tags: ["寺庙", "古建筑", "山林", "杭州"], samplePhotoName: nil, difficulty: .easy),

            // 重庆
            PhotoLocation(id: "loc_11", name: "洪崖洞", description: "依山而建的吊脚楼群，夜晚灯火辉煌，宛如千与千寻场景", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 29.5647, longitude: 106.5830)), bestTime: "日落后蓝调时刻，灯光全开", tags: ["建筑", "夜景", "山城", "重庆"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_12", name: "长江索道", description: "横跨长江的空中索道，拍摄山城立体交通的独特视角", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 29.5610, longitude: 106.5850)), bestTime: "傍晚日落时分", tags: ["城市", "交通", "长江", "重庆"], samplePhotoName: nil, difficulty: .medium),

            // 成都
            PhotoLocation(id: "loc_13", name: "宽窄巷子", description: "成都三大历史文化保护区之一，青砖黛瓦，川西民居风格", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.6680, longitude: 104.0560)), bestTime: "清晨游客较少时", tags: ["古街", "人文", "建筑", "成都"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_14", name: "锦里古街", description: "武侯祠旁的古街，红灯笼挂满街巷，三国文化浓郁", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.6480, longitude: 104.0470)), bestTime: "傍晚灯笼亮起时", tags: ["古街", "夜景", "人文", "成都"], samplePhotoName: nil, difficulty: .easy),

            // 西安
            PhotoLocation(id: "loc_15", name: "大雁塔", description: "唐代古塔，大慈恩寺内，广场喷泉与古塔形成现代与古典的碰撞", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 34.2195, longitude: 108.9640)), bestTime: "傍晚喷泉表演时", tags: ["古塔", "历史", "喷泉", "西安"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_16", name: "钟楼", description: "西安市中心地标，东西南北四条大街交汇处，夜景灯光璀璨", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 34.2610, longitude: 108.9425)), bestTime: "夜晚灯光亮起时", tags: ["建筑", "夜景", "历史", "西安"], samplePhotoName: nil, difficulty: .easy),

            // 厦门
            PhotoLocation(id: "loc_17", name: "鼓浪屿", description: "万国建筑博览，琴声悠扬，海风轻拂，文艺小清新的天堂", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 24.4479, longitude: 118.0680)), bestTime: "清晨或傍晚，避开正午烈日", tags: ["海岛", "建筑", "文艺", "厦门"], samplePhotoName: nil, difficulty: .easy),

            // 桂林
            PhotoLocation(id: "loc_18", name: "漓江兴坪", description: "20元人民币背景取景地，漓江山水甲天下，喀斯特地貌奇观", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 24.9250, longitude: 110.5350)), bestTime: "清晨薄雾或傍晚日落", tags: ["山水", "河流", "喀斯特", "桂林"], samplePhotoName: nil, difficulty: .medium),
            PhotoLocation(id: "loc_19", name: "龙脊梯田", description: "壮族梯田奇观，层层叠叠如登天阶梯，春秋两季尤为壮观", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 25.7600, longitude: 110.1250)), bestTime: "春季灌水或秋季收割时", tags: ["梯田", "农耕", "少数民族", "桂林"], samplePhotoName: nil, difficulty: .hard),

            // 云南
            PhotoLocation(id: "loc_20", name: "丽江古城", description: "世界文化遗产，纳西族建筑风格，小桥流水，四方街热闹非凡", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 26.8720, longitude: 100.2330)), bestTime: "清晨古城苏醒时，游客较少", tags: ["古城", "少数民族", "人文", "丽江"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_21", name: "大理洱海", description: "苍山洱海，风花雪月，环海公路处处是风景", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 25.6050, longitude: 100.2350)), bestTime: "日出或日落时分，湖面如镜", tags: ["湖泊", "山脉", "公路", "大理"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_22", name: "元阳梯田", description: "哈尼族梯田，被誉为大地的雕塑，日出时分云雾缭绕如仙境", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 23.0930, longitude: 102.7800)), bestTime: "冬季清晨，梯田灌水反射天光", tags: ["梯田", "日出", "云雾", "云南"], samplePhotoName: nil, difficulty: .hard),

            // 四川
            PhotoLocation(id: "loc_23", name: "稻城亚丁", description: "蓝色星球上最后一片净土，三座神山巍峨矗立，牛奶海五色海如宝石", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 28.4680, longitude: 100.2940)), bestTime: "秋季10月，色彩最丰富", tags: ["雪山", "湖泊", "高原", "四川"], samplePhotoName: nil, difficulty: .hard),
            PhotoLocation(id: "loc_24", name: "九寨沟", description: "人间仙境，五彩池、诺日朗瀑布，水色斑斓令人叹为观止", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 33.2630, longitude: 103.9030)), bestTime: "秋季10月中下旬，彩林与碧水", tags: ["湖泊", "瀑布", "彩林", "四川"], samplePhotoName: nil, difficulty: .medium),

            // 张家界
            PhotoLocation(id: "loc_25", name: "张家界天子山", description: "石英砂岩峰林地貌，阿凡达取景地，云雾缭绕如悬浮山", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 29.3450, longitude: 110.4350)), bestTime: "雨后初晴，云雾缭绕时", tags: ["山峰", "云雾", "自然", "张家界"], samplePhotoName: nil, difficulty: .medium),

            // 黄山
            PhotoLocation(id: "loc_26", name: "黄山迎客松", description: "天下第一奇山，奇松、怪石、云海、温泉四绝，日出壮观", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.1340, longitude: 118.1670)), bestTime: "冬季雪后或日出时分", tags: ["山峰", "云海", "日出", "黄山"], samplePhotoName: nil, difficulty: .hard),

            // 苏州
            PhotoLocation(id: "loc_27", name: "拙政园", description: "中国四大名园之一，江南园林代表作，移步换景处处是画", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 31.3260, longitude: 120.6290)), bestTime: "春季花开或秋季红叶时", tags: ["园林", "古建筑", "苏州"], samplePhotoName: nil, difficulty: .easy),

            // 南京
            PhotoLocation(id: "loc_28", name: "中山陵", description: "孙中山先生陵寝，392级台阶气势恢宏，俯瞰整个南京城", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 32.0645, longitude: 118.8480)), bestTime: "秋季梧桐金黄时", tags: ["陵墓", "建筑", "历史", "南京"], samplePhotoName: nil, difficulty: .easy),

            // 青岛
            PhotoLocation(id: "loc_29", name: "八大关", description: "万国建筑博览区，红瓦绿树碧海蓝天，德式建筑与海岸线交织", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 36.0560, longitude: 120.3460)), bestTime: "秋季银杏金黄时", tags: ["建筑", "海岸", "秋叶", "青岛"], samplePhotoName: nil, difficulty: .easy),

            // 深圳
            PhotoLocation(id: "loc_30", name: "深圳湾公园", description: "城市与自然交融，15公里滨海长廊，日出方向正对香港", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 22.5170, longitude: 113.9480)), bestTime: "清晨日出时分", tags: ["海岸", "城市", "日出", "深圳"], samplePhotoName: nil, difficulty: .easy),

            // 广州
            PhotoLocation(id: "loc_31", name: "广州塔", description: "小蛮腰，600米高空俯瞰珠江新城，城市夜景迷人", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 23.1065, longitude: 113.3240)), bestTime: "傍晚蓝调时刻，灯光亮起", tags: ["建筑", "夜景", "城市", "广州"], samplePhotoName: nil, difficulty: .easy),
            PhotoLocation(id: "loc_32", name: "沙面", description: "欧陆风情建筑群，百年榕树遮天蔽日，文艺街拍圣地", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 23.1100, longitude: 113.2440)), bestTime: "下午阳光透过榕树叶洒落", tags: ["建筑", "历史", "街拍", "广州"], samplePhotoName: nil, difficulty: .easy),

            // 武汉
            PhotoLocation(id: "loc_33", name: "黄鹤楼", description: "天下江山第一楼，长江与汉江交汇处，登楼远眺气势磅礴", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 30.5470, longitude: 114.3030)), bestTime: "傍晚日落时分", tags: ["古楼", "长江", "历史", "武汉"], samplePhotoName: nil, difficulty: .easy),

            // 拉萨
            PhotoLocation(id: "loc_34", name: "布达拉宫", description: "世界屋脊上的宫殿，红白相间的宫殿群，信仰的力量", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 29.6578, longitude: 91.1169)), bestTime: "清晨日出或傍晚日落", tags: ["宫殿", "高原", "宗教", "拉萨"], samplePhotoName: nil, difficulty: .hard),

            // 哈尔滨
            PhotoLocation(id: "loc_35", name: "圣索菲亚教堂", description: "远东最大东正教堂，拜占庭风格建筑，雪后更显庄严", coordinate: CodableCoordinate(CLLocationCoordinate2D(latitude: 45.7680, longitude: 126.6240)), bestTime: "冬季雪后或夜晚灯光", tags: ["教堂", "建筑", "雪景", "哈尔滨"], samplePhotoName: nil, difficulty: .easy),
        ]
    }

    // MARK: - 按标签筛选

    func getLocationsByTag(_ tag: String) -> [PhotoLocation] {
        allLocationsCache.filter { $0.tags.contains(tag) }
    }

    func getAllTags() -> [String] {
        var tagSet = Set<String>()
        for location in allLocationsCache {
            for tag in location.tags {
                tagSet.insert(tag)
            }
        }
        return tagSet.sorted()
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        currentLocation = location
        findNearbySpots()
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("[LocationRecommender] 定位失败: \(error.localizedDescription)")
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        switch authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.requestLocation()
        case .denied, .restricted:
            // 无权限时展示所有地点
            findNearbySpots(within: .infinity)
        default:
            break
        }
    }
}

#endif