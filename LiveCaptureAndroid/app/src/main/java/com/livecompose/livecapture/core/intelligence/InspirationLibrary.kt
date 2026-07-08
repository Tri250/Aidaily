package com.livecompose.livecapture.core.intelligence

import java.util.Calendar

object InspirationLibrary {

    private val entries: List<InspirationEntry> = listOf(
        // =========================================================================
        // PORTRAIT_STANDING — 站立人像 (4 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_ps_001",
            scene = SceneType.PORTRAIT_STANDING,
            style = "清新",
            title = "逆光发丝光",
            description = "利用午后三四点的逆光拍摄站立人像，让阳光从被摄者后方斜射过来，在被摄者头发周围形成柔和的金色轮廓光。建议使用大光圈镜头虚化背景，营造梦幻氛围。",
            tags = listOf("逆光", "发丝光", "清新", "大光圈", "人像"),
            photographerNote = "让被摄者微微侧身，光线从侧后方45度角射入效果最佳。曝光补偿+0.7EV确保面部不欠曝，必要时使用反光板补光。"
        ),
        InspirationEntry(
            id = "insp_ps_002",
            scene = SceneType.PORTRAIT_STANDING,
            style = "城市",
            title = "街头漫步抓拍",
            description = "在城市街道中捕捉站立人像的自然状态，利用斑马线、橱窗、涂鸦墙等城市元素作为背景。让被摄者自然行走，在动态中抓拍最自然的瞬间。",
            tags = listOf("街拍", "城市", "抓拍", "自然", "动态"),
            photographerNote = "使用快门优先模式，快门速度不低于1/250s以冻结动作。选择有纵深感的长街作为背景，利用引导线构图增强画面层次。"
        ),
        InspirationEntry(
            id = "insp_ps_003",
            scene = SceneType.PORTRAIT_STANDING,
            style = "复古",
            title = "胶片质感人像",
            description = "模仿胶片摄影风格的站立人像，通过暖色调、轻微颗粒感和柔和的色彩过渡营造怀旧氛围。选择有年代感的建筑或老街区作为拍摄场景。",
            tags = listOf("胶片", "复古", "暖色调", "怀旧", "颗粒感"),
            photographerNote = "后期降低饱和度10-15%，增加暖色调色温至5800K左右，添加轻微暗角。拍摄时选择有质感的老墙或铁门作为背景。"
        ),
        InspirationEntry(
            id = "insp_ps_004",
            scene = SceneType.PORTRAIT_STANDING,
            style = "极简",
            title = "极简黑白立像",
            description = "以极简主义风格拍摄黑白站立人像，使用纯色背景或简洁的建筑墙面，强调人物的形态、线条和光影对比。",
            tags = listOf("黑白", "极简", "光影", "高对比", "线条"),
            photographerNote = "使用点测光对准人物面部，故意让背景过曝或欠曝以突出主体。注意人物姿态的线条感，手臂与身体之间留出空隙形成负空间。"
        ),

        // =========================================================================
        // PORTRAIT_SITTING — 坐姿人像 (3 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_psi_001",
            scene = SceneType.PORTRAIT_SITTING,
            style = "日系",
            title = "窗边自然光坐姿",
            description = "利用窗户透进来的柔和自然光拍摄坐姿人像，让被摄者坐在窗边的椅子或地板上，光线从侧面打来形成柔和的立体感。日系风格追求清新、通透、低饱和的视觉效果。",
            tags = listOf("日系", "自然光", "窗边", "坐姿", "柔和"),
            photographerNote = "选择朝北的窗户以获得最均匀的光线。让被摄者靠近窗户但不要直射，使用纱帘柔化光线。曝光补偿+0.3EV增加通透感。"
        ),
        InspirationEntry(
            id = "insp_psi_002",
            scene = SceneType.PORTRAIT_SITTING,
            style = "氛围",
            title = "咖啡厅氛围坐姿",
            description = "在咖啡厅的昏暗暖光环境中拍摄坐姿人像，利用桌上的台灯或吊灯作为主光源，营造温馨、慵懒的氛围感。",
            tags = listOf("咖啡厅", "氛围感", "暖光", "慵懒", "室内"),
            photographerNote = "ISO可适当提高至800-1600，使用大光圈镜头。利用桌上的咖啡杯或书本作为前景道具增加画面层次。白平衡设置在4000K左右保留暖调氛围。"
        ),
        InspirationEntry(
            id = "insp_psi_003",
            scene = SceneType.PORTRAIT_SITTING,
            style = "清新",
            title = "草地午后坐姿",
            description = "在户外草地上拍摄坐姿人像，利用斑驳的树影和绿色草地作为天然背景，营造清新自然的氛围。",
            tags = listOf("户外", "草地", "清新", "自然", "树影"),
            photographerNote = "选择下午4点后的柔和光线，利用树冠缝隙形成的点状光斑作为背景。让被摄者侧坐，身体略微后仰，手撑地面形成自然姿态。"
        ),

        // =========================================================================
        // PORTRAIT — 人像特写 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_p_001",
            scene = SceneType.PORTRAIT,
            style = "情绪",
            title = "情绪肖像特写",
            description = "专注于人物面部表情和眼神的情绪肖像特写，使用长焦大光圈镜头压缩背景，突出人物的眼神光和微表情。",
            tags = listOf("特写", "情绪", "眼神", "长焦", "肖像"),
            photographerNote = "使用85mm或135mm镜头，光圈f/2.0或更大。对焦点必须精确落在眼睛上。与被摄者保持沟通，引导其展现真实情绪而非刻意摆拍。"
        ),
        InspirationEntry(
            id = "insp_p_002",
            scene = SceneType.PORTRAIT,
            style = "柔美",
            title = "柔光梦幻人像",
            description = "使用柔光镜或后期柔焦效果拍摄梦幻风格的人像，通过柔化的高光部分营造朦胧、浪漫的视觉效果。",
            tags = listOf("柔光", "梦幻", "朦胧", "浪漫", "高调"),
            photographerNote = "可在镜头前加一层薄纱或使用柔焦滤镜。曝光略微过曝0.5-1档营造高调效果。选择浅色服装和浅色背景增强梦幻感。"
        ),

        // =========================================================================
        // WEDDING — 婚礼 (4 entries: 2 outdoor, 2 indoor)
        // =========================================================================
        InspirationEntry(
            id = "insp_w_001",
            scene = SceneType.WEDDING,
            style = "浪漫",
            title = "户外草地婚礼",
            description = "在阳光明媚的户外草地拍摄婚礼，利用自然光和开阔的绿色背景，捕捉新人甜蜜互动的自然瞬间。",
            tags = listOf("婚礼", "户外", "草地", "浪漫", "自然光"),
            photographerNote = "使用24-70mm变焦镜头灵活构图。注意白色婚纱的曝光控制，避免高光溢出。利用花束、拱门等婚礼元素丰富画面层次。"
        ),
        InspirationEntry(
            id = "insp_w_002",
            scene = SceneType.WEDDING,
            style = "唯美",
            title = "海滩婚礼剪影",
            description = "在海边拍摄婚礼，利用日落时分的金色光线和海水倒影，创造浪漫唯美的海滩婚礼照片。",
            tags = listOf("婚礼", "海滩", "日落", "唯美", "剪影"),
            photographerNote = "日落前30分钟是黄金拍摄时间。使用逆光拍摄新人剪影，曝光以天空为准，创造戏剧性的光影效果。使用广角镜头纳入更多海天景色。"
        ),
        InspirationEntry(
            id = "insp_w_003",
            scene = SceneType.WEDDING,
            style = "典雅",
            title = "教堂婚礼仪式",
            description = "在教堂内拍摄庄严的婚礼仪式，利用教堂的彩色玻璃窗透过的光线、高挑的穹顶和对称的建筑结构创造庄重典雅的画面。",
            tags = listOf("婚礼", "教堂", "室内", "庄严", "对称"),
            photographerNote = "教堂内光线通常较暗，使用大光圈镜头和高ISO。注意不要使用闪光灯以免干扰仪式。利用对称构图突出教堂建筑的庄严感。"
        ),
        InspirationEntry(
            id = "insp_w_004",
            scene = SceneType.WEDDING,
            style = "华丽",
            title = "宴会厅婚礼晚宴",
            description = "在宴会厅拍摄婚礼晚宴，利用华丽的灯光装饰、精致的餐桌布置和温馨的烛光营造浪漫华丽的氛围。",
            tags = listOf("婚礼", "宴会", "室内", "华丽", "灯光"),
            photographerNote = "使用跳闪或离机闪光灯避免直闪的硬光。白平衡设置在3200K左右保留暖色调氛围。抓拍新人切蛋糕、敬酒等关键时刻。"
        ),

        // =========================================================================
        // CHILDREN — 儿童 (4 entries: 2 outdoor, 2 indoor)
        // =========================================================================
        InspirationEntry(
            id = "insp_c_001",
            scene = SceneType.CHILDREN,
            style = "活泼",
            title = "公园嬉戏抓拍",
            description = "在公园户外环境中拍摄儿童自然玩耍的瞬间，利用滑梯、秋千等游乐设施创造有趣的构图，捕捉孩子最真实灿烂的笑容。",
            tags = listOf("儿童", "户外", "公园", "抓拍", "活泼"),
            photographerNote = "降低拍摄角度至儿童视线水平。使用连拍模式和高速快门（1/500s以上）捕捉快速动作。使用70-200mm长焦镜头在不打扰孩子的情况下远距离拍摄。"
        ),
        InspirationEntry(
            id = "insp_c_002",
            scene = SceneType.CHILDREN,
            style = "清新",
            title = "草地奔跑童年",
            description = "在开阔的草地上拍摄儿童奔跑、放风筝、吹泡泡的欢乐场景，利用蓝天绿草作为清新背景，定格无忧无虑的童年时光。",
            tags = listOf("儿童", "草地", "奔跑", "童年", "户外"),
            photographerNote = "使用广角镜头低角度仰拍，将天空作为背景突出孩子的动态。逆光拍摄时注意面部补光。开启连续对焦模式追踪移动中的孩子。"
        ),
        InspirationEntry(
            id = "insp_c_003",
            scene = SceneType.CHILDREN,
            style = "温馨",
            title = "家庭温馨亲子时光",
            description = "在家庭室内环境中拍摄儿童与父母的温馨互动，利用柔和的室内灯光和家居环境营造温暖的亲子氛围。",
            tags = listOf("儿童", "家庭", "室内", "温馨", "亲子"),
            photographerNote = "利用窗户光作为主光源，避免使用闪光灯惊吓孩子。拍摄亲子阅读、玩耍等自然互动场景，不要刻意摆拍。"
        ),
        InspirationEntry(
            id = "insp_c_004",
            scene = SceneType.CHILDREN,
            style = "可爱",
            title = "玩具时光创意拍摄",
            description = "在室内利用玩具和彩色道具创造趣味十足的儿童摄影场景，通过鲜艳的色彩和创意的布置展现孩子的童真世界。",
            tags = listOf("儿童", "室内", "玩具", "创意", "彩色"),
            photographerNote = "使用彩色背景纸或布搭建简易背景。用大光圈虚化背景中的玩具，突出孩子专注的表情。可以让孩子玩积木或画画，拍摄自然投入的状态。"
        ),

        // =========================================================================
        // PRODUCT — 产品 (3 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_pr_001",
            scene = SceneType.PRODUCT,
            style = "商业",
            title = "电商白底产品照",
            description = "使用纯白背景和无影布光拍摄产品标准照，确保产品细节清晰、色彩还原准确，适合电商平台展示使用。",
            tags = listOf("产品", "白底", "电商", "无影", "商业"),
            photographerNote = "使用至少两盏灯从左右45度角打向产品，背景单独用一盏灯打亮至纯白。使用三脚架和小光圈（f/8-f/11）确保整体清晰。"
        ),
        InspirationEntry(
            id = "insp_pr_002",
            scene = SceneType.PRODUCT,
            style = "创意",
            title = "创意布光产品摄影",
            description = "使用创意布光手法拍摄产品，通过硬光、色光、投影等技巧创造独特的视觉效果，适合品牌宣传和社交媒体展示。",
            tags = listOf("产品", "创意布光", "硬光", "色光", "品牌"),
            photographerNote = "使用蜂巢罩或束光筒控制光线范围，制造戏剧性光影。可以加入彩色滤色片创造氛围光。注意保持产品主体受光充足，细节可见。"
        ),
        InspirationEntry(
            id = "insp_pr_003",
            scene = SceneType.PRODUCT,
            style = "生活",
            title = "场景化产品展示",
            description = "将产品置于真实使用场景中拍摄，通过环境氛围烘托产品的质感与用途，让消费者产生代入感。",
            tags = listOf("产品", "场景化", "生活感", "代入感", "氛围"),
            photographerNote = "选择与产品调性相符的场景，如咖啡豆配合木质桌面和咖啡器具。使用大光圈虚化背景，但保留场景的辨识度。注意场景元素不要喧宾夺主。"
        ),

        // =========================================================================
        // LANDSCAPE — 风景 (7 entries: 3 sunset, 3 nature, 1 general)
        // =========================================================================
        InspirationEntry(
            id = "insp_ls_001",
            scene = SceneType.LANDSCAPE,
            style = "震撼",
            title = "海边落日余晖",
            description = "在海边拍摄壮丽的日落景象，利用海面反射的金色光芒、天空的渐变色彩和前景的礁石或沙滩创造层次丰富的画面。",
            tags = listOf("日落", "海边", "余晖", "反射", "壮丽"),
            photographerNote = "使用三脚架，光圈f/8-f/11以保证景深。使用渐变中灰滤镜平衡天空与地面的光比。日落前后15分钟是色彩最丰富的时刻。"
        ),
        InspirationEntry(
            id = "insp_ls_002",
            scene = SceneType.LANDSCAPE,
            style = "城市",
            title = "城市天际线日落",
            description = "从高处拍摄城市天际线在日落时分的壮丽景象，将建筑剪影与橙红色天空结合，展现都市与自然的交融之美。",
            tags = listOf("日落", "城市", "天际线", "建筑", "剪影"),
            photographerNote = "提前踩点确定最佳拍摄机位。使用小光圈营造星芒效果。在城市灯光亮起前15分钟拍摄，同时捕捉天空余晖和初亮的城市灯光。"
        ),
        InspirationEntry(
            id = "insp_ls_003",
            scene = SceneType.LANDSCAPE,
            style = "壮阔",
            title = "山间日落云海",
            description = "在高山之上拍摄日落时分的云海景观，当夕阳将云海染成金色和粉红色时，创造出如仙境般的壮丽画面。",
            tags = listOf("日落", "山景", "云海", "壮阔", "仙境"),
            photographerNote = "需要在日落前1小时到达拍摄点。使用广角镜头纳入更多云海，或使用长焦压缩云层的层次感。注意防风，三脚架要稳固。"
        ),
        InspirationEntry(
            id = "insp_ln_001",
            scene = SceneType.LANDSCAPE,
            style = "神秘",
            title = "森林晨雾光影",
            description = "在清晨的森林中拍摄穿透树冠的光束，利用晨雾增强光线的可见度，创造神秘而梦幻的森林光影效果。",
            tags = listOf("森林", "晨雾", "光束", "神秘", "自然"),
            photographerNote = "需要在日出前到达森林，晨雾通常在日出后30-60分钟内消散。使用小光圈增强光束的星芒感。逆光拍摄，让光束从树冠缝隙中射入。"
        ),
        InspirationEntry(
            id = "insp_ln_002",
            scene = SceneType.LANDSCAPE,
            style = "诗意",
            title = "山水画卷意境",
            description = "拍摄山水相依的自然景观，利用水面倒影、远山层叠和前景元素的组合，营造中国传统山水画般的诗意意境。",
            tags = listOf("山水", "倒影", "意境", "诗意", "中国风"),
            photographerNote = "选择无风的清晨以获得完美的水面倒影。使用对称构图将地平线放在画面中央。加入一叶扁舟或飞鸟作为点睛之笔。"
        ),
        InspirationEntry(
            id = "insp_ln_003",
            scene = SceneType.LANDSCAPE,
            style = "辽阔",
            title = "草原天际线风光",
            description = "在辽阔的草原上拍摄天地相接的壮丽风光，利用草原的线条、起伏的地形和天空的云彩创造纵深感。",
            tags = listOf("草原", "辽阔", "天际线", "云彩", "自然"),
            photographerNote = "使用广角镜头低角度拍摄，纳入前景的野花或草穗增加层次感。等待云彩形成有趣的形状时按下快门。偏振镜可以帮助增强蓝天和云彩的对比。"
        ),
        InspirationEntry(
            id = "insp_l_001",
            scene = SceneType.LANDSCAPE,
            style = "都市",
            title = "城市全景风光",
            description = "从制高点拍摄城市全景，将城市的建筑群、街道网络和自然景观融为一体，展现城市的宏大格局和独特魅力。",
            tags = listOf("城市", "全景", "建筑", "都市", "风光"),
            photographerNote = "使用全景拼接或超广角镜头。选择空气质量好的天气，偏振镜可以帮助减少雾霾感。蓝调时刻（日落后20分钟）拍摄可以同时获得建筑灯光和天空色彩。"
        ),

        // =========================================================================
        // FOOD — 美食 (4 entries: 3 food styling, 1 general)
        // =========================================================================
        InspirationEntry(
            id = "insp_f_001",
            scene = SceneType.FOOD,
            style = "精致",
            title = "精致摆盘美食摄影",
            description = "以精致摆盘为核心的美食摄影，注重食物的色彩搭配、造型设计和餐具的协调，营造高级餐厅的精致感。",
            tags = listOf("美食", "摆盘", "精致", "色彩", "高级"),
            photographerNote = "使用侧逆光突出食物的质感和立体感。光圈f/2.8-f/4，焦点落在食物最精彩的部分。使用柔光板柔化光线，避免硬阴影。"
        ),
        InspirationEntry(
            id = "insp_f_002",
            scene = SceneType.FOOD,
            style = "生活",
            title = "俯拍餐桌盛宴",
            description = "采用俯拍角度拍摄整桌美食，将多道菜品、餐具、装饰物以平面设计的方式排列，创造色彩丰富、构图饱满的画面。",
            tags = listOf("美食", "俯拍", "餐桌", "平面", "丰富"),
            photographerNote = "使用90度俯拍，确保相机与桌面完全平行。将主菜放在画面中心或三分线交点处。使用自然光从窗户方向打来，避免相机投影落入画面。"
        ),
        InspirationEntry(
            id = "insp_f_003",
            scene = SceneType.FOOD,
            style = "清新",
            title = "饮品特写动态抓拍",
            description = "拍摄饮品倒出、冰块落入、气泡升腾等动态瞬间，通过高速快门凝固液体流动的美感，展现饮品的清爽与活力。",
            tags = listOf("饮品", "特写", "动态", "高速", "清爽"),
            photographerNote = "使用高速快门1/1000s以上凝固液体动态。使用离机闪光灯提供充足照明。多次尝试以捕捉最佳瞬间。在玻璃杯上喷水雾增加冰爽感。"
        ),
        InspirationEntry(
            id = "insp_f_004",
            scene = SceneType.FOOD,
            style = "烟火",
            title = "街头烟火美食记录",
            description = "在夜市或街头小吃摊拍摄充满烟火气的美食照片，利用摊位的灯光、蒸腾的热气和忙碌的摊主营造真实、有温度的市井美食氛围。",
            tags = listOf("美食", "街头", "烟火气", "夜市", "市井"),
            photographerNote = "利用环境光，提高ISO至1600-3200。拍摄热气腾腾的瞬间增加画面感染力。可以纳入摊主的手部动作增加人文气息。"
        ),

        // =========================================================================
        // GROUP — 合影 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_g_001",
            scene = SceneType.GROUP,
            style = "温馨",
            title = "全家福幸福合影",
            description = "拍摄温馨的全家福合影，合理安排家庭成员的位置和姿态，通过高低错落的站位和自然的互动展现家庭的温暖与幸福。",
            tags = listOf("合影", "家庭", "温馨", "全家福", "幸福"),
            photographerNote = "使用三脚架和小光圈（f/8）确保所有人都在景深范围内。使用定时拍摄或遥控器。选择户外柔和光线，避免正午强光造成眯眼。"
        ),
        InspirationEntry(
            id = "insp_g_002",
            scene = SceneType.GROUP,
            style = "欢乐",
            title = "好友聚会欢乐合影",
            description = "拍摄朋友聚会的欢乐合影，不拘泥于正式站位，鼓励自然的互动、搞怪的表情和放松的姿态，记录真挚的友谊瞬间。",
            tags = listOf("合影", "朋友", "聚会", "欢乐", "自然"),
            photographerNote = "使用广角镜头拍摄更多人，注意边缘人物不要变形。可以让大家一起跳跃或做动作，使用连拍捕捉最佳瞬间。自拍杆或超广角前置镜头是不错的选择。"
        ),

        // =========================================================================
        // NATURE — 水景 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_wa_001",
            scene = SceneType.NATURE,
            style = "梦幻",
            title = "瀑布长曝光丝绸效果",
            description = "使用长曝光技术拍摄瀑布，将水流拍成丝绸般的柔滑效果，与周围的岩石和植被形成动静对比。",
            tags = listOf("瀑布", "长曝光", "水流", "丝绸", "梦幻"),
            photographerNote = "使用ND1000减光镜，曝光时间2-10秒。必须使用三脚架和快门线。光圈f/8-f/11保证景深。选择阴天或阴影中的瀑布避免光比过大。"
        ),
        InspirationEntry(
            id = "insp_wa_002",
            scene = SceneType.NATURE,
            style = "宁静",
            title = "湖面镜面倒影",
            description = "在无风的清晨或黄昏拍摄平静的湖面倒影，利用完美的对称构图将天空、山峦和湖岸的倒影融为一体，创造宁静致远的画面。",
            tags = listOf("湖面", "倒影", "镜面", "对称", "宁静"),
            photographerNote = "选择日出前或日落后无风的时刻。使用对称构图，也可将地平线放在上三分之一处强调倒影。偏振镜可以控制水面反光程度。"
        ),

        // =========================================================================
        // NIGHT — 夜景 (3 entries: 2 night portrait, 1 night scene)
        // =========================================================================
        InspirationEntry(
            id = "insp_np_001",
            scene = SceneType.NIGHT,
            style = "都市",
            title = "城市灯光夜景人像",
            description = "在城市夜景中拍摄人像，利用城市灯光、车流轨迹和霓虹灯作为背景，创造充满都市感的夜景人像照片。",
            tags = listOf("夜景", "人像", "城市", "灯光", "都市"),
            photographerNote = "使用大光圈镜头（f/1.4-f/2.0），ISO 800-1600。使用离机闪光灯或LED灯为人脸补光。利用背景的灯光光斑营造浪漫氛围。快门速度不低于1/60s。"
        ),
        InspirationEntry(
            id = "insp_np_002",
            scene = SceneType.NIGHT,
            style = "潮流",
            title = "霓虹灯潮流人像",
            description = "利用城市中的霓虹灯招牌和彩色灯光拍摄潮流风格的人像，通过霓虹灯的色彩和光线营造赛博朋克风格的视觉效果。",
            tags = listOf("霓虹灯", "潮流", "夜景", "赛博朋克", "色彩"),
            photographerNote = "让被摄者站在霓虹灯旁边，让彩色光线打在脸上形成独特的色彩。使用大光圈虚化背景灯光。后期可以增强色彩饱和度和对比度。"
        ),
        InspirationEntry(
            id = "insp_ns_001",
            scene = SceneType.NIGHT,
            style = "璀璨",
            title = "城市夜景车流光轨",
            description = "从高处或天桥拍摄城市夜景，利用长时间曝光将车流灯光转化为流动的光轨，展现城市的繁华与动感。",
            tags = listOf("夜景", "车流", "光轨", "城市", "璀璨"),
            photographerNote = "使用三脚架，曝光时间10-30秒，光圈f/8-f/11。使用低ISO（100）保证画质。选择弯道处拍摄可以获得更优美的光轨曲线。"
        ),

        // =========================================================================
        // SILHOUETTE — 剪影 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_s_001",
            scene = SceneType.SILHOUETTE,
            style = "浪漫",
            title = "日落剪影人像",
            description = "在日落时分拍摄人物剪影，利用明亮的天空作为背景，将被摄者的轮廓清晰地勾勒出来，创造浪漫而富有故事感的画面。",
            tags = listOf("剪影", "日落", "轮廓", "浪漫", "故事感"),
            photographerNote = "以天空为基准测光，让人物严重欠曝形成纯黑剪影。注意人物的肢体姿态要清晰可辨，避免手臂与身体重叠。加入牵手或拥抱等动作增加情感表达。"
        ),
        InspirationEntry(
            id = "insp_s_002",
            scene = SceneType.SILHOUETTE,
            style = "艺术",
            title = "城市建筑剪影",
            description = "利用城市建筑的轮廓在黄昏或黎明时分拍摄建筑剪影，将建筑的结构美感与天空的色彩渐变相结合，创造富有几何美感的画面。",
            tags = listOf("剪影", "建筑", "城市", "几何", "轮廓"),
            photographerNote = "选择有独特轮廓的建筑，如教堂尖顶、摩天大楼或桥梁。使用f/8-f/11小光圈。等待天空中有有趣的云彩形状时拍摄。"
        ),

        // =========================================================================
        // MACRO — 微距 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_m_001",
            scene = SceneType.MACRO,
            style = "精致",
            title = "花卉微距细节之美",
            description = "使用微距镜头拍摄花卉的精细细节，如花蕊的结构、花瓣的纹理和露珠的光泽，展现肉眼难以察觉的微观世界之美。",
            tags = listOf("微距", "花卉", "细节", "花蕊", "露珠"),
            photographerNote = "使用微距镜头（100mm左右），光圈f/5.6-f/8以获得足够的景深。使用三脚架和反光镜预升减少震动。清晨拍摄可以获得自然的露珠效果。"
        ),
        InspirationEntry(
            id = "insp_m_002",
            scene = SceneType.MACRO,
            style = "奇妙",
            title = "昆虫微观世界",
            description = "在自然环境中拍摄昆虫的微距照片，捕捉蝴蝶翅膀的鳞片、蜻蜓的复眼或蜜蜂采蜜的瞬间，展现微观世界的奇妙与美丽。",
            tags = listOf("微距", "昆虫", "微观", "自然", "奇妙"),
            photographerNote = "清晨昆虫活动较少时更容易拍摄。使用高速闪光灯凝固动作。靠近昆虫时要缓慢移动，避免惊扰。使用手动对焦进行精确对焦。"
        ),

        // =========================================================================
        // STILL_LIFE — 纹理 (2 entries)
        // =========================================================================
        InspirationEntry(
            id = "insp_tx_001",
            scene = SceneType.STILL_LIFE,
            style = "质感",
            title = "自然纹理抽象之美",
            description = "拍摄自然界中的纹理细节，如树皮的年轮纹路、石头的肌理、树叶的脉络，通过近距离拍摄展现材质的质感与抽象美感。",
            tags = listOf("纹理", "自然", "抽象", "质感", "细节"),
            photographerNote = "使用侧光或斜侧光突出纹理的立体感。使用微距或近摄镜。后期可以增强清晰度和对比度来强化纹理表现。"
        ),
        InspirationEntry(
            id = "insp_tx_002",
            scene = SceneType.STILL_LIFE,
            style = "工业",
            title = "工业金属纹理",
            description = "拍摄工业场景中的金属纹理，如锈迹斑斑的铁板、拉丝不锈钢、铜锈等，通过光影强调金属的质感与岁月的痕迹。",
            tags = listOf("纹理", "工业", "金属", "锈迹", "质感"),
            photographerNote = "使用硬光从侧面打来，突出金属表面的凹凸纹理。近距离拍摄，构图填满画面。后期适当增加清晰度和纹理。"
        ),

        // =========================================================================
        // PET — 宠物 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_pt_001",
            scene = SceneType.PET,
            style = "可爱",
            title = "萌宠自然抓拍",
            description = "在自然光线和环境中拍摄宠物的可爱瞬间，捕捉它们玩耍、休息或与主人互动时的自然表情和动作。",
            tags = listOf("宠物", "萌宠", "抓拍", "可爱", "自然"),
            photographerNote = "降低至宠物视线高度拍摄。使用高速快门（1/500s以上）和连续对焦模式。使用玩具或零食吸引宠物注意力。使用大光圈虚化背景突出宠物。"
        ),

        // =========================================================================
        // ARCHITECTURE — 建筑 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_a_001",
            scene = SceneType.ARCHITECTURE,
            style = "几何",
            title = "建筑几何美学",
            description = "以建筑的结构线条、几何形状和光影关系为拍摄主题，通过精心的构图展现建筑的秩序美感和设计巧思。",
            tags = listOf("建筑", "几何", "线条", "结构", "光影"),
            photographerNote = "使用移轴镜头或后期校正透视变形。寻找重复的图案、对称的结构和有趣的几何形状。利用侧光突出建筑的立体感和纹理。"
        ),

        // =========================================================================
        // STREET — 街拍 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_st_001",
            scene = SceneType.STREET,
            style = "纪实",
            title = "城市街头故事",
            description = "在城市的街头巷尾捕捉日常生活中的真实瞬间，记录路人的神态、街角的场景和城市的人文气息，每张照片都是一个小故事。",
            tags = listOf("街拍", "纪实", "城市", "故事", "人文"),
            photographerNote = "使用35mm或50mm定焦镜头，保持低调。使用f/8光圈配合区域对焦法快速抓拍。预判有趣的场景和人物互动，提前构图等待决定性瞬间。"
        ),

        // =========================================================================
        // OUTDOOR — 海滩 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_b_001",
            scene = SceneType.OUTDOOR,
            style = "夏日",
            title = "夏日海滩活力时光",
            description = "在阳光明媚的海滩拍摄夏日活力场景，利用碧海蓝天、金色沙滩和跳跃的浪花营造充满活力的夏日氛围。",
            tags = listOf("海滩", "夏日", "阳光", "活力", "海浪"),
            photographerNote = "使用偏振镜减少水面反光并增强天空蓝色。保护设备免受沙子和海水侵蚀。利用海浪的线条作为引导线构图。上午或下午光线最佳。"
        ),

        // =========================================================================
        // NATURE — 雪景 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_sn_001",
            scene = SceneType.NATURE,
            style = "纯净",
            title = "冬日雪景童话世界",
            description = "拍摄银装素裹的冬日雪景，利用白雪的纯净与树木、建筑形成鲜明对比，营造如童话世界般的纯净美感。",
            tags = listOf("雪景", "冬日", "纯净", "童话", "白色"),
            photographerNote = "曝光补偿+1至+2EV，避免白雪被拍成灰色。注意保护相机电池，低温下电量消耗很快。选择日出或日落时拍摄，金色光线照在雪上格外美丽。"
        ),

        // =========================================================================
        // MACRO — 花卉 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_fl_001",
            scene = SceneType.MACRO,
            style = "浪漫",
            title = "花海浪漫人像",
            description = "在盛开的鲜花田中拍摄人像，将人物置于花海之中，利用花朵的色彩和层次营造浪漫唯美的画面氛围。",
            tags = listOf("花海", "人像", "浪漫", "色彩", "唯美"),
            photographerNote = "使用大光圈虚化前景和背景中的花朵，突出人物。让被摄者微微俯身闻花或轻触花瓣，创造自然互动。使用85mm以上焦段压缩花海层次。"
        ),

        // =========================================================================
        // SUNSET — 日出 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_sr_001",
            scene = SceneType.SUNSET,
            style = "希望",
            title = "晨曦之光日出拍摄",
            description = "在黎明时分拍摄日出景象，捕捉太阳从地平线升起的第一缕光芒，将天空从深蓝到橙红的渐变色彩完整记录下来。",
            tags = listOf("日出", "晨曦", "朝霞", "希望", "渐变"),
            photographerNote = "提前30分钟到达拍摄点，日出前天空色彩变化最为丰富。使用三脚架，光圈f/8-f/11。使用渐变滤镜或包围曝光合成处理大光比。"
        ),

        // =========================================================================
        // EVENT — 舞台 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_sg_001",
            scene = SceneType.EVENT,
            style = "动感",
            title = "舞台演出精彩瞬间",
            description = "拍摄舞台演出中的精彩瞬间，利用舞台灯光、烟雾效果和演员的动态表演，捕捉充满戏剧性和张力的舞台画面。",
            tags = listOf("舞台", "演出", "灯光", "动感", "戏剧"),
            photographerNote = "使用70-200mm f/2.8镜头，ISO 1600-3200。使用点测光对准演员面部。使用连拍模式捕捉最佳动作瞬间。关闭闪光灯，利用舞台灯光创造氛围。"
        ),

        // =========================================================================
        // INDOOR — 室内 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_id_001",
            scene = SceneType.INDOOR,
            style = "光影",
            title = "室内光影空间艺术",
            description = "在室内空间中利用窗户光、百叶窗投影或灯光装置创造富有光影层次的空间摄影作品，展现室内空间的结构美感与光影魅力。",
            tags = listOf("室内", "光影", "空间", "窗户光", "结构"),
            photographerNote = "寻找有趣的光影投射，如百叶窗条纹光影或彩色玻璃的投影。使用广角镜头纳入更多空间元素。使用三脚架进行长时间曝光以获得最佳画质。"
        ),

        // =========================================================================
        // DOCUMENTARY — 文档 (1 entry)
        // =========================================================================
        InspirationEntry(
            id = "insp_dc_001",
            scene = SceneType.DOCUMENTARY,
            style = "专业",
            title = "文档翻拍与证件照",
            description = "使用均匀光线和标准角度拍摄证件照和文档翻拍，确保画面平整、文字清晰、色彩还原准确，适合正式用途。",
            tags = listOf("文档", "证件照", "翻拍", "专业", "均匀光"),
            photographerNote = "使用两盏灯从左右45度角均匀打光，避免反光和阴影。使用三脚架确保相机与文档完全平行。使用f/5.6-f/8光圈保证画面四角清晰度一致。"
        )
    )

    // =========================================================================
    // Public API
    // =========================================================================

    fun getInspirations(scene: SceneType): List<InspirationEntry> {
        return entries.filter { it.scene == scene }
    }

    fun getInspirations(scenes: List<SceneType>): List<InspirationEntry> {
        val sceneSet = scenes.toSet()
        return entries.filter { it.scene in sceneSet }
    }

    fun getAllInspirations(): List<InspirationEntry> {
        return entries.toList()
    }

    fun getRandomInspiration(): InspirationEntry {
        return entries.random()
    }

    fun getInspirationsByTag(tag: String): List<InspirationEntry> {
        val lowerTag = tag.lowercase()
        return entries.filter { entry ->
            entry.tags.any { it.lowercase().contains(lowerTag) }
        }
    }

    fun getFeaturedInspirations(): List<InspirationEntry> {
        return listOf(
            entries.first { it.id == "insp_ps_001" },
            entries.first { it.id == "insp_ls_001" },
            entries.first { it.id == "insp_w_001" },
            entries.first { it.id == "insp_f_001" },
            entries.first { it.id == "insp_np_001" }
        )
    }

    fun getDailyInspiration(): InspirationEntry {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % entries.size
        return entries[index]
    }
}