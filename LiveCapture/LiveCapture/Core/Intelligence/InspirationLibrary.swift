//
//  InspirationLibrary.swift
//  LiveCapture
//
//  摄影灵感库 - 基于场景类型的拍摄灵感推荐系统
//
//  ## 文件作用
//  提供精选摄影灵感数据库，覆盖所有场景类型
//  支持按场景、标签、日期等维度检索灵感条目
//  为 AI 场景引擎提供拍摄建议和创意指导
//
//  ## 主要类型
//  ### InspirationLibrary
//  灵感库单例类，包含 50+ 条精心策划的摄影灵感
//
//  ## 主要方法
//
//  ### 场景检索
//  - getInspirations(for:): 获取单个场景的灵感
//  - getInspirations(for:): 获取多个场景的灵感
//  - getAllInspirations(): 获取所有灵感
//
//  ### 特色推荐
//  - getRandomInspiration(): 随机获取一条灵感
//  - getInspirationsByTag(_:): 按标签搜索灵感
//  - getFeaturedInspirations(): 获取 5 条精选特色灵感
//  - getDailyInspiration(): 基于日历日期的每日灵感（确定性）
//
//  ## 线程安全
//  - 所有数据为只读静态数组，天然线程安全
//  - 返回值为值类型，无共享状态
//

import Foundation

#if os(iOS)

/// 摄影灵感库 - 单例模式
final class InspirationLibrary {

	// MARK: - 单例

	/// 共享实例
	static let shared = InspirationLibrary()

	private init() {}

	// MARK: - 灵感数据库

	/// 完整的灵感数据库（50+ 条精选条目）
	private let entries: [InspirationEntry] = [
		// MARK: 人像·站姿（3 条）
		InspirationEntry(
			id: "portrait-standing-001",
			scene: .portraitStanding,
			style: "逆光",
			title: "逆光少女",
			description: "在日落黄金时刻，让模特背对光源站立，阳光从发丝间穿透形成柔美的轮廓光。使用大光圈镜头虚化背景，营造梦幻温暖的氛围。建议使用反光板为面部补光，平衡逆光带来的暗部细节丢失。",
			tags: ["逆光", "少女", "黄金时刻", "轮廓光", "大光圈"],
			photographerNote: "拍摄时注意让模特侧脸转向光源方向，让光线勾勒出下颌线条。使用点测光对面部进行测光，避免面部过暗。后期可适当增加暖色调和柔光效果。"
		),
		InspirationEntry(
			id: "portrait-standing-002",
			scene: .portraitStanding,
			style: "城市",
			title: "都市漫步",
			description: "在繁华都市的街头，让模特自然站立于斑马线或建筑前，利用城市线条作为引导线构图。穿着简约时尚的服装，与城市背景形成对比，记录都市生活的真实瞬间。使用中长焦镜头压缩空间感，突出人物主体。",
			tags: ["城市", "街拍", "引导线", "中长焦", "自然"],
			photographerNote: "选择人流量较少的时间段拍摄，注意背景中的广告牌和灯箱不要过于抢眼。使用连拍模式捕捉模特自然走动的瞬间，快门速度保持 1/250s 以上。"
		),
		InspirationEntry(
			id: "portrait-standing-003",
			scene: .portraitStanding,
			style: "清新",
			title: "窗边人像",
			description: "利用室内窗边的自然光拍摄站姿人像，柔和的散射光从侧面打亮模特面部，形成自然的立体感。模特可以倚靠窗框或自然站立，透过薄纱窗帘的光线更加柔和。适合拍摄清新自然的日常风格人像。",
			tags: ["窗光", "自然光", "清新", "散射光", "室内"],
			photographerNote: "注意窗户朝向，北向窗户提供全天柔和的散射光最为理想。如果光线过强，可以用白色薄纱帘柔化。ISO 保持在 400-800 之间，确保安全快门的同时控制噪点。"
		),

		// MARK: 人像·坐姿（2 条）
		InspirationEntry(
			id: "portrait-sitting-001",
			scene: .portraitSitting,
			style: "日系",
			title: "咖啡馆午后",
			description: "在洒满阳光的咖啡馆角落，模特坐在木质椅子上，双手轻握咖啡杯，眼神自然望向窗外。柔和的侧光在面部形成细腻的光影过渡，营造日系清新的慵懒氛围。使用 50mm 标准镜头，保持自然的透视感。",
			tags: ["日系", "咖啡", "午后", "侧光", "标准镜头"],
			photographerNote: "注意桌面上的道具布置，咖啡杯、书本等元素可以增加画面故事感。构图时让模特视线方向留出空间，避免画面显得局促。白平衡设为 5500K 左右，保留暖调氛围。"
		),
		InspirationEntry(
			id: "portrait-sitting-002",
			scene: .portraitSitting,
			style: "胶片",
			title: "长椅时光",
			description: "在公园或校园的长椅上，模特自然端坐，双腿交叉或并拢，双手自然搭在膝上。利用树荫下的斑驳光影，在人物身上形成有趣的光斑效果。使用 85mm 定焦镜头，f/1.8 大光圈虚化背景，突出人物情绪。",
			tags: ["胶片", "斑驳光", "公园", "长焦", "情绪"],
			photographerNote: "注意斑驳光不要落在面部造成不均匀曝光，可以让模特略微调整位置。后期可以增加胶片色调，略微降低对比度，提升阴影部分的灰度感。"
		),

		// MARK: 婚礼·户外（2 条）
		InspirationEntry(
			id: "wedding-outdoor-001",
			scene: .weddingOutdoor,
			style: "浪漫",
			title: "草坪婚礼",
			description: "在绿色草坪上举行的户外婚礼，新娘穿着白色婚纱在阳光下自然站立。使用广角镜头拍摄全景，将蓝天、绿草和白色婚纱形成色彩对比。逆光拍摄时，婚纱的薄纱部分会呈现出梦幻的透光效果。",
			tags: ["婚礼", "草坪", "广角", "逆光", "浪漫"],
			photographerNote: "提前踩点确定光线方向，上午 9-11 点或下午 3-5 点是最佳拍摄时间。使用 RAW 格式拍摄，保留婚纱高光细节。带一个助手帮忙整理裙摆。"
		),
		InspirationEntry(
			id: "wedding-outdoor-002",
			scene: .weddingOutdoor,
			style: "唯美",
			title: "花海誓言",
			description: "在花海或花园中拍摄户外婚礼，新人站在花丛中，四周被鲜花环绕。使用 70-200mm 长焦镜头压缩空间，让花海与新人的距离感减弱，营造被花海包围的唯美效果。柔和的自然光下，色彩饱和而不过度。",
			tags: ["花海", "长焦", "唯美", "色彩", "婚礼"],
			photographerNote: "使用 f/2.8 光圈制造浅景深效果，让前景和背景的花卉虚化形成色彩光斑。注意不要使用过低的拍摄角度，避免新人面部被花朵遮挡。"
		),

		// MARK: 婚礼·室内（2 条）
		InspirationEntry(
			id: "wedding-indoor-001",
			scene: .weddingIndoor,
			style: "典雅",
			title: "殿堂仪式",
			description: "在教堂或婚礼殿堂内拍摄仪式环节，利用建筑本身的对称结构和穹顶光线营造庄严神圣的氛围。使用广角镜头拍摄全景，将新人与建筑空间融为一体。注意控制混合光源的白平衡，保留暖色调的氛围灯效。",
			tags: ["教堂", "对称", "庄严", "广角", "暖调"],
			photographerNote: "提前与灯光师沟通，了解仪式环节的灯光变化。使用三脚架拍摄重要环节，ISO 控制在 1600 以内。准备一支 24-70mm 变焦镜头应对不同景别需求。"
		),
		InspirationEntry(
			id: "wedding-indoor-002",
			scene: .weddingIndoor,
			style: "温馨",
			title: "烛光晚宴",
			description: "在室内婚礼晚宴中，利用烛光和暖色灯光营造温馨浪漫的氛围。拍摄新人切蛋糕或交换戒指的细节时，使用大光圈镜头捕捉烛光在戒指和酒杯上的反光。降低快门速度保留环境光，让画面充满温暖的金色调。",
			tags: ["烛光", "晚宴", "暖调", "细节", "大光圈"],
			photographerNote: "使用 35mm 或 50mm 大光圈定焦镜头，f/1.4 可以在极暗环境下安全拍摄。白平衡设为 3200K 左右保留烛光暖调，不对着烛光点测光。"
		),

		// MARK: 儿童·户外（2 条）
		InspirationEntry(
			id: "children-outdoor-001",
			scene: .childrenOutdoor,
			style: "活泼",
			title: "草地奔跑",
			description: "在阳光明媚的草地上，让孩子自由奔跑和玩耍，使用高速连拍捕捉最自然的笑容和动作。低角度拍摄可以更好地展现孩子的视角，让画面充满童趣。使用 70-200mm 长焦镜头在远处抓拍，不打扰孩子的自然状态。",
			tags: ["草地", "奔跑", "连拍", "低角度", "自然"],
			photographerNote: "快门速度设为 1/500s 以上确保冻结动作，使用 AF-C 连续对焦追踪移动中的孩子。拍摄时蹲下或趴下，与孩子视线平齐。"
		),
		InspirationEntry(
			id: "children-outdoor-002",
			scene: .childrenOutdoor,
			style: "童趣",
			title: "泡泡乐园",
			description: "在户外公园或游乐场，让孩子玩吹泡泡的游戏，阳光下的泡泡反射出七彩光芒成为天然的梦幻元素。拍摄孩子追逐泡泡的瞬间，表情生动自然。使用逆光或侧逆光增强泡泡的透明感和色彩。",
			tags: ["泡泡", "逆光", "梦幻", "童趣", "公园"],
			photographerNote: "使用 f/2.8 光圈虚化背景，让泡泡成为画面中的光斑元素。注意泡泡在面部造成的反光，可以适当调整拍摄角度。带上泡泡机作为道具，控制泡泡的密度和方向。"
		),

		// MARK: 儿童·室内（2 条）
		InspirationEntry(
			id: "children-indoor-001",
			scene: .childrenIndoor,
			style: "温馨",
			title: "阅读时光",
			description: "在温暖的室内光线中，孩子坐在窗边或地毯上专注地翻看绘本。柔和的侧光打亮孩子的侧脸和书页，画面充满宁静温馨的氛围。使用大光圈镜头捕捉孩子专注的眼神和翻书的小手。",
			tags: ["阅读", "窗光", "温馨", "专注", "室内"],
			photographerNote: "选择自然光充足的时段拍摄，ISO 控制在 800 以内。使用静音快门避免打扰孩子的专注状态。在画面中加入毛绒玩具或靠垫增加温馨感。"
		),
		InspirationEntry(
			id: "children-indoor-002",
			scene: .childrenIndoor,
			style: "可爱",
			title: "玩具总动员",
			description: "在孩子的游戏区拍摄他们与心爱玩具互动的瞬间。将玩具作为前景元素，营造丰富的画面层次。使用自然光与柔和的补光结合，让画面明亮而柔和。俯拍角度可以展现孩子和玩具之间的亲密关系。",
			tags: ["玩具", "俯拍", "可爱", "互动", "层次"],
			photographerNote: "拍摄前清理游戏区杂乱元素，保持画面整洁。使用 35mm 镜头在较近距离拍摄，既能展现环境又能捕捉表情。光圈 f/2.0 左右，保持玩具和孩子的景深平衡。"
		),

		// MARK: 产品·白底（2 条）
		InspirationEntry(
			id: "product-white-001",
			scene: .productWhite,
			style: "极简",
			title: "纯白产品照",
			description: "在纯白无缝背景前拍摄产品，使用双灯布光（左右各一柔光箱）消除阴影，打造干净利落的电商级别产品照。产品居中构图，画面简洁有力，适合化妆品、电子产品等高质感产品。使用微距镜头拍摄产品细节和质感。",
			tags: ["白底", "极简", "柔光箱", "居中", "微距"],
			photographerNote: "背景需要过曝 0.5-1 档以确保纯白，使用入射式测光表测量产品主体光线。确保产品表面没有灰尘和指纹，准备清洁工具。使用小光圈 f/8-f/11 保证产品全貌清晰。"
		),
		InspirationEntry(
			id: "product-white-002",
			scene: .productWhite,
			style: "创意",
			title: "悬浮产品",
			description: "通过细线悬挂或后期合成的方式，让产品呈现悬浮在空中的视觉效果。利用白底背景的简洁性，突出产品的形态和设计感。使用高速快门冻结产品，结合侧光突出产品的立体感和材质纹理。",
			tags: ["悬浮", "创意", "立体感", "侧光", "设计"],
			photographerNote: "使用细鱼线悬挂产品，后期用 Photoshop 消除鱼线痕迹。拍摄时注意鱼线不要产生反光。快门速度 1/200s 以上，使用三脚架确保构图稳定便于后期合成。"
		),

		// MARK: 风景·日落（3 条）
		InspirationEntry(
			id: "landscape-sunset-001",
			scene: .landscapeSunset,
			style: "壮丽",
			title: "金色地平线",
			description: "在海边或开阔的平原拍摄日落时分的金色地平线。太阳位于画面三分之一处，天空从橙红渐变到深蓝。使用小光圈 f/11-f/16 获得星芒效果，让太阳呈现放射状光芒。前景加入剪影元素增加画面层次。",
			tags: ["日落", "地平线", "星芒", "渐变", "剪影"],
			photographerNote: "使用三脚架和快门线避免抖动，开启反光板预升功能。使用渐变中灰镜平衡天空和地面的光比。在太阳刚落下的 15-20 分钟内拍摄，天空色彩最为丰富。"
		),
		InspirationEntry(
			id: "landscape-sunset-002",
			scene: .landscapeSunset,
			style: "梦幻",
			title: "云端落日",
			description: "在高山或高层建筑上拍摄云层之上的日落。夕阳将云海染成金色和粉色，云层的纹理在侧光下格外立体。使用长焦镜头捕捉远处的日落细节，将云海的层次压缩成油画般的画面。",
			tags: ["云海", "高山", "长焦", "层次", "金粉"],
			photographerNote: "使用 70-200mm 镜头，焦距在 135mm 以上拍摄。使用点测光对天空中间亮度区域测光。携带保暖衣物，高山日落前后温差大。"
		),
		InspirationEntry(
			id: "landscape-sunset-003",
			scene: .landscapeSunset,
			style: "诗意",
			title: "湖面倒影",
			description: "在平静的湖面拍摄日落倒影，天空的色彩完美映射在水面上，形成上下对称的构图。选择无风的傍晚，水面如镜面般平静。使用广角镜头将天空和水面各占画面一半，营造宁静诗意的氛围。",
			tags: ["倒影", "湖面", "对称", "广角", "宁静"],
			photographerNote: "构图时让地平线严格居中，使用相机内置水平仪辅助。偏振镜可以消除水面反光，让倒影更清晰。使用包围曝光拍摄，后期合成 HDR 保留天空和倒影的细节。"
		),

		// MARK: 风景·自然（3 条）
		InspirationEntry(
			id: "landscape-nature-001",
			scene: .landscapeNature,
			style: "壮阔",
			title: "山峦叠嶂",
			description: "在清晨或黄昏拍摄连绵的山脉，利用层叠的山峦形成由近到远、由深到浅的色彩渐变。使用长焦镜头压缩空间，让远近山峦仿佛叠加在一起。薄雾或晨霭在山间弥漫时效果最佳，营造水墨画般的意境。",
			tags: ["山脉", "渐变", "长焦", "晨雾", "水墨"],
			photographerNote: "使用 f/8-f/11 光圈确保景深，使用点测光对中间调的山体测光。清晨 5-7 点是拍摄山间薄雾的最佳时间。使用三脚架保证长焦拍摄的稳定性。"
		),
		InspirationEntry(
			id: "landscape-nature-002",
			scene: .landscapeNature,
			style: "幽静",
			title: "森林秘境",
			description: "在茂密的森林中，利用穿透树叶的光束（丁达尔效应）拍摄森林的神秘氛围。阳光从树冠缝隙中洒下，形成一道道光柱。使用广角镜头仰拍，将高大的树木和光束一同纳入画面。适当降低曝光突出光束的亮度。",
			tags: ["森林", "光束", "丁达尔", "广角", "神秘"],
			photographerNote: "选择湿度较高的清晨或雨后，空气中水汽充足时光束效果最明显。镜头使用遮光罩防止眩光。曝光补偿 -0.7EV 左右，让光束更突出。使用小光圈 f/11-f/16。"
		),
		InspirationEntry(
			id: "landscape-nature-003",
			scene: .landscapeNature,
			style: "辽阔",
			title: "草原天际",
			description: "在广袤的草原上，利用低角度拍摄风吹草低的景象。天空占画面三分之二，云朵的形态成为画面的主角。使用超广角镜头 16-24mm，将草原的辽阔和天空的壮美完美融合。在草原上点缀一棵孤树或一匹马，增加画面焦点。",
			tags: ["草原", "超广角", "天空", "辽阔", "低角度"],
			photographerNote: "使用光圈 f/8-f/11，对焦在画面三分之一处确保全景深。偏振镜可以加深蓝天饱和度。注意地平线保持水平，使用网格线辅助构图。"
		),

		// MARK: 美食·摆盘（3 条）
		InspirationEntry(
			id: "food-styling-001",
			scene: .foodStyling,
			style: "精致",
			title: "俯拍餐桌",
			description: "采用 90 度俯拍角度拍摄精心布置的餐桌，将菜品、餐具、桌布和装饰元素以平面构成的方式排列。利用对角线构图或黄金分割点放置主菜，其余元素围绕主菜布置。使用自然光或柔光箱均匀打亮整个画面。",
			tags: ["俯拍", "餐桌", "平面构成", "自然光", "精致"],
			photographerNote: "使用 50mm 或 35mm 镜头，站在椅子上或使用俯拍架。光圈 f/4-f/5.6 保证所有菜品清晰。注意桌布纹理和餐具的反光，调整角度避免强反光。"
		),
		InspirationEntry(
			id: "food-styling-002",
			scene: .foodStyling,
			style: "食欲",
			title: "特写美味",
			description: "使用微距镜头拍摄食物的细节特写——流淌的芝士、冒泡的酱汁、酥脆的表皮。利用 45 度侧光从后方打亮食物，突出食物的纹理和光泽。浅景深让焦点集中在食物最诱人的部分，背景完全虚化。",
			tags: ["微距", "特写", "侧逆光", "纹理", "食欲"],
			photographerNote: "使用 100mm 微距镜头，f/2.8-f/4 光圈。拍摄前用刷子在食物表面刷一层薄油增加光泽感。使用镊子和棉签调整食物细节，确保画面完美。"
		),
		InspirationEntry(
			id: "food-styling-003",
			scene: .foodStyling,
			style: "生活",
			title: "厨房日记",
			description: "在厨房自然光下拍摄烹饪过程的纪实风格美食照。切菜的手部动作、锅中翻腾的食材、砧板上的新鲜食材——这些半成品状态同样充满魅力。使用自然光营造真实的生活气息，不需要过度摆盘。",
			tags: ["厨房", "纪实", "烹饪", "生活", "自然光"],
			photographerNote: "使用 35mm 或 24-70mm 变焦镜头，方便在厨房狭小空间内灵活构图。光圈 f/2.8-f/4，快门速度 1/125s 以上冻结手部动作。ISO 800-1600 在室内光线中保持画质。"
		),

		// MARK: 合影（2 条）
		InspirationEntry(
			id: "group-photo-001",
			scene: .groupPhoto,
			style: "正式",
			title: "经典全家福",
			description: "在户外自然光下拍摄家庭合影，采用阶梯式排列（前排坐、后排站），让每个人的面部都能被看到。使用 f/5.6-f/8 光圈确保所有人都在景深范围内。选择柔和的阴天或日落前光线，避免在面部产生强烈阴影。",
			tags: ["全家福", "阶梯式", "小光圈", "自然光", "正式"],
			photographerNote: "使用三脚架和遥控快门，确保自己也能够在合影中。对焦在最前排人物的眼睛上。拍摄 5-10 张，确保每个人都有睁眼和微笑的照片。"
		),
		InspirationEntry(
			id: "group-photo-002",
			scene: .groupPhoto,
			style: "活泼",
			title: "跳跃瞬间",
			description: "拍摄一群朋友同时跳跃的欢乐瞬间，使用高速快门冻结空中的动作。低角度仰拍让跳跃的高度看起来更夸张，蓝天作为简洁的背景。统一喊口号让所有人同步跳跃，使用连拍模式捕捉最佳瞬间。",
			tags: ["跳跃", "仰拍", "高速快门", "连拍", "欢乐"],
			photographerNote: "快门速度 1/1000s 以上，使用广角镜头 16-24mm 低角度拍摄。先喊 '1、2、3' 让大家准备，在 '3' 的时候按下快门。使用 AF-C 对焦模式，光圈 f/8 确保多人在景深内。"
		),

		// MARK: 水景（2 条）
		InspirationEntry(
			id: "water-scene-001",
			scene: .waterScene,
			style: "丝滑",
			title: "瀑布丝绸",
			description: "使用慢速快门拍摄瀑布或溪流，将流动的水拍成丝滑的白色绸缎。使用 ND 减光镜延长曝光时间至 1-5 秒，三脚架是必须的。在画面中加入青苔覆盖的岩石或绿色植被作为色彩对比，增加画面的生机感。",
			tags: ["瀑布", "慢门", "ND镜", "丝滑", "三脚架"],
			photographerNote: "使用 ND1000 或 ND64 减光镜，根据光线条件选择。光圈 f/11-f/16 帮助延长曝光时间。使用快门线或 2 秒延时自拍避免按下快门时的抖动。"
		),
		InspirationEntry(
			id: "water-scene-002",
			scene: .waterScene,
			style: "灵动",
			title: "水花飞溅",
			description: "使用高速快门捕捉水花飞溅的瞬间——水滴落入水面激起皇冠形的水花、游泳者跃入水中的瞬间。使用 1/2000s 以上的快门速度冻结水滴，配合闪光灯在高功率下提供极短的闪光持续时间。黑色背景让透明的水花更加突出。",
			tags: ["水花", "高速快门", "闪光灯", "冻结", "微距"],
			photographerNote: "在浅色容器中滴入彩色水滴，使用离机闪光灯从侧面打光。手动对焦预设在落水点。使用遥控快门，另一只手控制水滴。需要多次尝试，成功率约 10-20%。"
		),

		// MARK: 夜景人像（2 条）
		InspirationEntry(
			id: "night-portrait-001",
			scene: .nightPortrait,
			style: "都市",
			title: "霓虹人像",
			description: "在夜晚的城市街头，利用霓虹灯、路灯和店铺橱窗的灯光作为背景光源，拍摄带有都市夜生活气息的人像。使用大光圈定焦镜头（f/1.4-f/1.8），将背景的灯光虚化成色彩斑斓的光斑。模特面部使用便携 LED 灯补光，平衡环境光与主体光线。",
			tags: ["霓虹", "光斑", "大光圈", "LED补光", "夜色"],
			photographerNote: "白平衡设为 3200K-4000K 保留暖色氛围，面部补光使用 5500K 日光型 LED。ISO 800-1600，快门速度不低于 1/60s。在繁华商业区寻找色彩丰富的招牌作为背景。"
		),
		InspirationEntry(
			id: "night-portrait-002",
			scene: .nightPortrait,
			style: "浪漫",
			title: "烟火人像",
			description: "使用仙女棒或手持烟花作为光源和道具，在夜晚拍摄浪漫的人像。烟花的暖光在模特面部形成柔和的补光，火花轨迹在长时间曝光下形成优美的光绘效果。模特手持烟花自然舞动，摄影师使用慢速快门记录光的轨迹。",
			tags: ["烟花", "光绘", "慢门", "暖光", "浪漫"],
			photographerNote: "快门速度 1/4s-1s 记录烟花轨迹，同时使用后帘同步闪光灯定格模特面部。ISO 400-800，光圈 f/4-f/5.6。注意安全，准备灭火用水。"
		),

		// MARK: 剪影（2 条）
		InspirationEntry(
			id: "silhouette-001",
			scene: .silhouette,
			style: "艺术",
			title: "日落剪影",
			description: "在日落时分，让模特站在太阳正前方，对天空最亮处测光，将人物完全拍成黑色剪影。利用人物轮廓（尤其是侧面）展现优美的身体线条，太阳的光晕围绕在人物周围形成神圣的光环效果。",
			tags: ["日落", "轮廓", "光环", "测光", "侧面"],
			photographerNote: "使用点测光对太阳旁边的天空测光，确保人物完全变黑。使用 f/8-f/11 光圈让太阳呈现星芒效果。注意模特的轮廓清晰度，避免手臂和身体重叠。"
		),
		InspirationEntry(
			id: "silhouette-002",
			scene: .silhouette,
			style: "极简",
			title: "极简剪影",
			description: "在纯色背景（如明亮的天空或白色墙面）前，拍摄人物或物体的极简剪影。通过强烈的明暗对比，去掉所有不必要的信息，只保留最纯粹的轮廓和形态。构图力求简洁，让观者关注形状和线条本身。",
			tags: ["极简", "明暗对比", "纯色", "线条", "形态"],
			photographerNote: "对着背景最亮处测光并锁定曝光，然后重新构图。使用高对比度后期处理，将阴影部分压至纯黑。选择轮廓特征鲜明的主体，如独树、飞鸟、舞者。"
		),

		// MARK: 微距（2 条）
		InspirationEntry(
			id: "macro-detail-001",
			scene: .macroDetail,
			style: "精细",
			title: "昆虫世界",
			description: "使用微距镜头探索昆虫的微观世界——蝴蝶翅膀上的鳞片、蜜蜂身上的绒毛、蜻蜓的复眼。清晨是拍摄昆虫的最佳时间，此时昆虫体温较低、活动迟缓。使用自然光或环形闪光灯均匀照亮微小主体。",
			tags: ["昆虫", "清晨", "环形灯", "细节", "微观"],
			photographerNote: "使用 100mm 或 180mm 微距镜头，1:1 放大倍率。光圈 f/8-f/16 确保足够的景深。手动对焦，使用相机 LV 放大功能精细对焦。使用三脚架和快门线消除抖动。"
		),
		InspirationEntry(
			id: "macro-detail-002",
			scene: .macroDetail,
			style: "诗意",
			title: "水滴世界",
			description: "拍摄清晨露珠或雨后水滴中的微观世界——水滴如同天然透镜，折射出背后的景物。使用微距镜头靠近水滴，将焦点对准水滴内部折射的影像。背景虚化成柔和的光斑，营造梦幻诗意的氛围。",
			tags: ["水滴", "露珠", "折射", "光斑", "清晨"],
			photographerNote: "清晨日出后 1 小时内是拍摄露珠的最佳时间。使用 f/4-f/5.6 光圈，让水滴清晰而背景虚化。寻找水滴中折射出花朵或树叶的特别角度。"
		),

		// MARK: 纹理（2 条）
		InspirationEntry(
			id: "texture-001",
			scene: .texture,
			style: "抽象",
			title: "岁月纹理",
			description: "拍摄老建筑墙面、树皮、锈蚀金属等自然形成的纹理。利用侧光或斜射光强调纹理的凹凸感，让平面的照片产生触觉般的质感。使用黑白模式去除色彩干扰，让观者专注于纹理本身的韵律和节奏。",
			tags: ["墙面", "树皮", "侧光", "黑白", "质感"],
			photographerNote: "使用小光圈 f/8-f/11 确保整个纹理面清晰。光线角度约 30-45 度时纹理最明显。使用黑白模式直出或后期转换为黑白，适当增加对比度。"
		),
		InspirationEntry(
			id: "texture-002",
			scene: .texture,
			style: "自然",
			title: "织物之美",
			description: "拍摄丝绸、棉麻、针织等不同织物的纹理细节。利用柔和的自然光展现织物的纤维结构和编织纹理。在画面中加入褶皱或折叠，让织物产生丰富的明暗变化。近距离拍摄，让织物充满整个画面，形成抽象而富有韵律的图案。",
			tags: ["织物", "纤维", "折叠", "柔和光", "韵律"],
			photographerNote: "使用 50mm 或 100mm 微距镜头。窗户自然光是拍摄织物的最佳光源，柔和的散射光能展现纤维细节。使用反光板填充阴影区域。手动对焦确保纹理清晰。"
		),

		// MARK: 人像（1 条）
		InspirationEntry(
			id: "portrait-001",
			scene: .portrait,
			style: "经典",
			title: "柔和光人像",
			description: "在阴天或阴影下拍摄经典人像，柔和均匀的光线在面部形成细腻的渐变过渡，没有强烈的阴影。使用 85mm f/1.4 定焦镜头，将背景完全虚化，突出人物神态和表情。模特眼神看向镜头，与观者建立直接的情感连接。",
			tags: ["阴天", "柔和光", "85mm", "眼神", "经典"],
			photographerNote: "让模特略微低头，下巴微收，可以显得脸部更瘦。使用单点对焦对在靠近镜头的眼睛上。阴天拍摄时色温偏冷，后期适当增加暖色调。"
		),

		// MARK: 美食（1 条）
		InspirationEntry(
			id: "food-001",
			scene: .food,
			style: "诱人",
			title: "热气腾腾",
			description: "拍摄刚出锅的热菜，利用上升的蒸汽营造温暖的食欲感。使用侧逆光从后方打亮蒸汽，让蒸汽在深色背景下清晰可见。餐具和配菜环绕主菜布置，形成丰富的画面层次。使用暖色调增强食物的温度和美味感。",
			tags: ["蒸汽", "侧逆光", "暖调", "刚出锅", "层次"],
			photographerNote: "在食物旁边放置一杯热水产生额外蒸汽。使用深色背景让蒸汽更明显。快门速度 1/250s 以上冻结蒸汽形态。使用 100mm 微距镜头拍摄特写。"
		),

		// MARK: 风景（1 条）
		InspirationEntry(
			id: "landscape-001",
			scene: .landscape,
			style: "壮丽",
			title: "经典三分法",
			description: "使用经典的三分法构图拍摄风景——前景（草地、岩石或花朵）、中景（湖泊或田野）、远景（山脉或天空）。每层占据画面三分之一，形成由近及远的深度感。使用广角镜头 16-24mm，光圈 f/8-f/11 确保全景深。",
			tags: ["三分法", "前景", "广角", "全景深", "层次"],
			photographerNote: "使用超焦距对焦技术确保从近到远都清晰。将相机放在低角度，让前景元素更突出。利用黄金时刻（日出后/日落前 1 小时）的暖光拍摄。"
		),

		// MARK: 宠物（1 条）
		InspirationEntry(
			id: "pet-001",
			scene: .pet,
			style: "可爱",
			title: "萌宠凝视",
			description: "在宠物放松的状态下，用它们最喜欢的玩具或零食吸引注意力，在它们看向镜头（或镜头方向）的瞬间按下快门。使用低角度拍摄，与宠物视线平齐，展现它们眼中的世界。使用大光圈虚化背景，让焦点集中在宠物的眼睛上。",
			tags: ["低角度", "眼睛", "玩具", "互动", "大光圈"],
			photographerNote: "使用 AF-C 连续对焦和动物眼部识别（如果相机支持）。快门速度 1/500s 以上应对宠物突然的动作。用零食或玩具吸引注意力但不要入镜。"
		),

		// MARK: 建筑（1 条）
		InspirationEntry(
			id: "architecture-001",
			scene: .architecture,
			style: "几何",
			title: "线条交响",
			description: "拍摄现代建筑的几何线条和结构——玻璃幕墙的反射、楼梯的螺旋曲线、立面的重复图案。使用广角镜头或移轴镜头拍摄，强调建筑的透视感和空间感。在画面中寻找引导线，将观者的视线引向建筑的核心结构。",
			tags: ["几何", "透视", "广角", "线条", "反射"],
			photographerNote: "使用移轴镜头校正透视变形，或后期在 Lightroom 中进行透视校正。拍摄时保持相机水平，使用网格线辅助。选择清晨或傍晚拍摄，金黄色的侧光让建筑立体感更强。"
		),

		// MARK: 夜景（1 条）
		InspirationEntry(
			id: "night-scene-001",
			scene: .nightScene,
			style: "璀璨",
			title: "城市星轨",
			description: "在城市的制高点使用长时间曝光拍摄城市夜景。车流变成红白色的光轨，建筑的灯光在夜色中璀璨闪耀。使用三脚架和快门线，曝光时间 15-30 秒。使用小光圈 f/11-f/16 让灯光呈现星芒效果。",
			tags: ["长曝光", "光轨", "三脚架", "星芒", "制高点"],
			photographerNote: "使用 ISO 100 和 f/11 光圈，曝光时间根据车流密度调整 10-30 秒。使用快门线或 2 秒延时自拍。在天全黑前 30 分钟开始拍摄，天空会呈现深蓝色调而非纯黑。"
		),

		// MARK: 文档（1 条）
		InspirationEntry(
			id: "document-001",
			scene: .document,
			style: "清晰",
			title: "文档扫描",
			description: "在均匀光照下拍摄纸质文档，确保文字清晰可读，纸张平整无褶皱。使用俯拍角度，相机与文档平面保持平行。使用均匀的漫反射光源（如两个柔光箱从左右 45 度打光），避免阴影和反光。后期使用透视校正裁剪为标准比例。",
			tags: ["俯拍", "平行", "均匀光", "无反光", "后期校正"],
			photographerNote: "使用三脚架确保相机稳定且与文档平面平行。光圈 f/5.6-f/8 获得最佳锐度。使用网格线和对角线辅助对齐。如果文档有反光，使用偏振镜消除。"
		),

		// MARK: 日出日落（1 条）
		InspirationEntry(
			id: "sunrise-001",
			scene: .sunrise,
			style: "希望",
			title: "晨光初现",
			description: "在日出前 30 分钟到达拍摄地点，捕捉从深蓝到橙红的天空渐变。太阳从地平线升起的那一刻，第一缕阳光穿透晨雾洒向大地。使用渐变中灰镜平衡天空和地面的光比。在画面中加入前景剪影（如树木、建筑）增加层次感。",
			tags: ["晨光", "渐变", "晨雾", "剪影", "中灰镜"],
			photographerNote: "提前使用手机 App 查询日出时间和方位。使用三脚架和包围曝光，后期合成获得最佳动态范围。日出前后的光线变化极快，提前设置好相机参数。"
		),

		// MARK: 雪景（1 条）
		InspirationEntry(
			id: "snow-001",
			scene: .snow,
			style: "纯净",
			title: "银装素裹",
			description: "在大雪过后的清晨拍摄雪景，阳光照射在新雪上反射出耀眼的光芒。使用曝光补偿 +1 到 +1.7EV，确保雪地呈现纯白色而非灰白色。在画面中加入红色或暖色元素（如红色小屋、穿着亮色衣服的人）作为视觉焦点，打破纯白画面的单调。",
			tags: ["曝光补偿", "新雪", "色彩对比", "纯净", "视觉焦点"],
			photographerNote: "相机测光表会把雪地拍成灰色，务必手动增加曝光补偿。使用偏振镜减少雪地反光。电池在低温下消耗快，携带备用电池并放在贴身口袋保暖。"
		),

		// MARK: 海滩（1 条）
		InspirationEntry(
			id: "beach-001",
			scene: .beach,
			style: "度假",
			title: "碧海蓝天",
			description: "在阳光明媚的海滩上，利用蓝天、碧海和白沙的天然色彩搭配拍摄。使用偏振镜加深天空的蓝色并消除水面反光。在画面中加入椰树、沙滩椅或冲浪者等元素，增加度假氛围。使用广角镜头展现海滩的辽阔感。",
			tags: ["偏振镜", "蓝天", "白沙", "广角", "度假"],
			photographerNote: "使用偏振镜时旋转镜片找到最佳消反角度。保护相机免受海水和沙粒侵蚀，使用防水相机包。在日出或日落时分拍摄，光线更柔和，沙滩纹理更立体。"
		),

		// MARK: 花卉（1 条）
		InspirationEntry(
			id: "flower-001",
			scene: .flower,
			style: "柔美",
			title: "花间私语",
			description: "在花园中拍摄盛开的花朵，使用微距镜头或长焦镜头将花朵从背景中分离出来。选择柔和的散射光（阴天或清晨），避免强烈阳光在花瓣上产生过曝。利用前景虚化的花瓣作为画框，增加画面的层次感和梦幻感。",
			tags: ["微距", "散射光", "虚化", "层次", "花园"],
			photographerNote: "使用 100mm 微距或 70-200mm 镜头，f/2.8-f/4 光圈。带一个小喷壶在花瓣上喷水珠增加生机感。选择没有风的时间段拍摄，避免花朵晃动导致模糊。"
		),

		// MARK: 舞台（1 条）
		InspirationEntry(
			id: "stage-001",
			scene: .stage,
			style: "动感",
			title: "舞台光影",
			description: "在演唱会或舞台表演中，利用舞台灯光的变化拍摄充满戏剧性的画面。使用较高的 ISO（3200-6400）应对快速变化的灯光，使用大光圈定焦镜头（如 70-200mm f/2.8）捕捉舞台上的精彩瞬间。注意捕捉灯光照亮表演者面部的那一刻。",
			tags: ["舞台灯光", "高ISO", "大光圈", "长焦", "戏剧性"],
			photographerNote: "使用点测光对表演者面部测光，舞台灯光变化大，使用光圈优先模式。快门速度不低于 1/250s 冻结动作。使用静音快门避免影响演出。"
		),

		// MARK: 街拍（1 条）
		InspirationEntry(
			id: "street-001",
			scene: .street,
			style: "纪实",
			title: "街头故事",
			description: "在城市街头捕捉真实的生活瞬间——匆匆走过的行人、街角的小贩、老建筑前晒太阳的老人。使用 35mm 或 28mm 定焦镜头，融入街头环境，以不打扰的方式记录。利用自然光和城市环境本身的色彩，呈现真实而有温度的街头画面。",
			tags: ["纪实", "35mm", "自然光", "真实", "温度"],
			photographerNote: "使用区域对焦法（f/8，预设对焦距离 3 米），看到画面直接按下快门无需对焦。穿低调的服装融入环境。拍摄前先观察，预判有趣瞬间的发生。"
		),

		// MARK: 室内（1 条）
		InspirationEntry(
			id: "indoor-001",
			scene: .indoor,
			style: "生活",
			title: "室内光影",
			description: "在室内利用窗户透入的自然光拍摄，捕捉光线在墙面、家具和人物身上形成的明暗对比。利用百叶窗或窗帘的条纹投影，在画面中形成有趣的几何光影。使用大光圈镜头在暗光环境中拍摄，保留室内的温暖氛围。",
			tags: ["窗光", "几何光影", "明暗对比", "大光圈", "温暖"],
			photographerNote: "在画面中加入一盏台灯或蜡烛作为辅助光源，增加层次感。使用 ISO 800-1600，光圈 f/1.8-f/2.8。利用反光板（白墙或白纸）为人物的暗部补光。"
		),
	]

	// MARK: - 公开方法

	/// 获取指定场景的灵感
	/// - Parameter scene: 场景类型
	/// - Returns: 匹配的灵感条目数组
	func getInspirations(for scene: SceneType) -> [InspirationEntry] {
		entries.filter { $0.scene == scene }
	}

	/// 获取多个场景的灵感
	/// - Parameter scenes: 场景类型数组
	/// - Returns: 匹配的灵感条目数组
	func getInspirations(for scenes: [SceneType]) -> [InspirationEntry] {
		let sceneSet = Set(scenes)
		return entries.filter { sceneSet.contains($0.scene) }
	}

	/// 获取所有灵感
	/// - Returns: 全部灵感条目数组
	func getAllInspirations() -> [InspirationEntry] {
		entries
	}

	/// 获取随机一条灵感
	/// - Returns: 随机灵感条目
	func getRandomInspiration() -> InspirationEntry {
		guard !entries.isEmpty else {
			// 理论上不会进入此分支，数据库始终有数据
			return InspirationEntry(
				id: "fallback",
				scene: .unknown,
				style: "默认",
				title: "即兴创作",
				description: "没有预设的灵感时，相信自己的直觉。观察周围的光线、色彩和构图，用心去感受和记录。",
				tags: ["即兴"],
				photographerNote: "最好的灵感往往来自于当下的瞬间。"
			)
		}
		return entries[Int.random(in: 0..<entries.count)]
	}

	/// 按标签搜索灵感
	/// - Parameter tag: 搜索标签
	/// - Returns: 包含该标签的灵感条目数组
	func getInspirationsByTag(_ tag: String) -> [InspirationEntry] {
		let lowercasedTag = tag.lowercased()
		return entries.filter { entry in
			entry.tags.contains { $0.lowercased().contains(lowercasedTag) }
		}
	}

	/// 获取精选特色灵感（5 条手选条目）
	/// - Returns: 5 条精选灵感条目
	func getFeaturedInspirations() -> [InspirationEntry] {
		let featuredIDs = [
			"portrait-standing-001",  // 逆光少女
			"landscape-sunset-001",   // 金色地平线
			"food-styling-002",       // 特写美味
			"night-portrait-001",     // 霓虹人像
			"silhouette-001",         // 日落剪影
		]
		let featuredSet = Set(featuredIDs)
		let featured = entries.filter { featuredSet.contains($0.id) }
		// 按 featuredIDs 顺序排列
		return featured.sorted { a, b in
			let indexA = featuredIDs.firstIndex(of: a.id) ?? Int.max
			let indexB = featuredIDs.firstIndex(of: b.id) ?? Int.max
			return indexA < indexB
		}
	}

	/// 获取每日灵感（基于日历日期的确定性随机）
	/// - Returns: 当天对应的灵感条目
	func getDailyInspiration() -> InspirationEntry {
		let calendar = Calendar.current
		let today = Date()
		let dayOfYear = calendar.ordinality(of: .day, in: .year, for: today) ?? 1
		let year = calendar.component(.year, from: today)
		// 使用年份和年内天数组合生成确定性索引
		let seed = year * 1000 + dayOfYear
		let index = seed % entries.count
		return entries[index]
	}
}

#endif