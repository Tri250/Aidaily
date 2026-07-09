package com.livecompose.livecapture.core.intelligence

/**
 * 姿势推荐引擎
 *
 * 基于场景类型、置信度和被摄主体检测结果，智能推荐最佳拍摄姿势。
 * 包含 35 个完整定义的姿势模板，覆盖 8 个类别。
 */
class PoseRecommendationEngine {

    // =========================================================================
    // 姿势数据库 — 35 个完整姿势 × 8 个类别
    // =========================================================================

    private val poseDatabase: Map<PoseCategory, Map<String, List<PoseTemplate>>> = mapOf(
        // ── PORTRAIT_STANDING: 6 个站姿 ──
        PoseCategory.PORTRAIT_STANDING to mapOf(
            "standing_poses" to listOf(
                PoseTemplate(
                    id = "portrait_standing_natural_standing",
                    name = "自然站立",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩放松自然下沉，略向后展开",
                        head = "头部微微倾斜 3-5 度，避免僵硬正直",
                        arms = "双臂自然垂于身体两侧，肘部微弯",
                        legs = "双腿与肩同宽，一腿微曲承重",
                        hands = "手指自然并拢微曲，拇指轻贴大腿外侧",
                        back = "背部挺直但不僵硬，保持自然弧度",
                        eyeContact = "直视镜头，保持温和眼神",
                        bodyAngle = "身体与镜头呈 15-30 度角",
                        hips = "髋部自然水平，重心落于后腿",
                        feet = "前脚尖指向镜头，后脚略外八",
                        chin = "下巴微收，避免双下巴",
                        gaze = "目光柔和聚焦于镜头中心"
                    ),
                    tips = listOf(
                        "深呼吸放松肩膀，想象肩胛骨向下滑动",
                        "将重心放在后腿上，前腿膝盖微曲",
                        "想象头顶有一根线轻轻向上提拉",
                        "拍摄前做几次肩部绕圈放松"
                    ),
                    variations = listOf(
                        "双手插兜自然站立",
                        "单手扶墙侧身站立",
                        "回头看向镜头"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "portrait_standing_confident_pose",
                    name = "自信姿态",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩后展下沉，展现自信胸廓",
                        head = "头部微微上扬 5-8 度，展露下颌线",
                        arms = "一手自然下垂，另一手叉腰或插入口袋",
                        legs = "双腿分开略宽于肩，前腿伸直后腿微曲",
                        hands = "叉腰手大拇指朝前，四指并拢",
                        back = "背部挺直，核心微收",
                        eyeContact = "坚定直视镜头，略带自信微笑",
                        bodyAngle = "身体与镜头呈 30-45 度角",
                        hips = "髋部向前腿方向微倾",
                        feet = "前脚正对镜头，后脚呈 45 度",
                        chin = "下巴微抬，拉长颈部线条",
                        gaze = "目光坚定有力，展现掌控感"
                    ),
                    tips = listOf(
                        "想象自己是全场最自信的人",
                        "叉腰时手腕放松，不要用力过猛",
                        "保持核心收紧，让姿态更挺拔",
                        "眼神要坚定但不咄咄逼人",
                        "嘴角保持若有若无的微笑"
                    ),
                    variations = listOf(
                        "双手抱胸自信站立",
                        "单手搭肩自信回眸",
                        "靠墙单手插兜"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "portrait_standing_casual_lean",
                    name = "随性倚靠",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "靠墙侧肩部轻贴墙面，对侧肩自然下垂",
                        head = "头部转向镜头，微倾 5-10 度",
                        arms = "靠墙侧手臂自然弯曲撑墙，另一手插兜",
                        legs = "双腿交叉站立，重心在靠墙侧腿",
                        hands = "撑墙手肘微弯，手指自然展开",
                        back = "背部略离墙面，保持自然曲线",
                        eyeContact = "轻松看向镜头或侧目而视",
                        bodyAngle = "身体与墙面呈 10-20 度角",
                        hips = "髋部靠墙侧微顶",
                        feet = "前脚交叉于后脚前，脚尖点地",
                        chin = "下巴微收，脖颈放松",
                        gaze = "慵懒随性的目光"
                    ),
                    tips = listOf(
                        "不要整个人靠在墙上，保持身体张力",
                        "交叉腿时确保重心稳定",
                        "选择有质感的墙面作为背景",
                        "可以尝试单手插兜增加随性感"
                    ),
                    variations = listOf(
                        "背靠墙壁抬头看远方",
                        "侧身倚靠栏杆",
                        "单手扶墙低头微笑"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "portrait_standing_dynamic_movement",
                    name = "动态行走",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩略微前后错开，展现动感",
                        head = "头部自然朝前或转向镜头",
                        arms = "双臂自然摆动，前后交错",
                        legs = "一腿前迈步态，后腿蹬地发力",
                        hands = "手指自然微曲，随步伐摆动",
                        back = "背部微前倾，展现前进动势",
                        eyeContact = "偶尔看向镜头，大多自然前视",
                        bodyAngle = "身体与镜头呈 45-60 度角",
                        hips = "髋部随步伐自然扭动",
                        feet = "前脚跟着地，后脚尖蹬地",
                        chin = "下巴自然水平，不过度抬起",
                        gaze = "自然前视，偶与镜头交流"
                    ),
                    tips = listOf(
                        "让模特自然行走，摄影师抓拍",
                        "使用连拍模式捕捉最佳瞬间",
                        "步伐不要太快，放慢到平时的 70%",
                        "手臂摆动幅度比平时略大 20%",
                        "选择有纵深感的路段拍摄"
                    ),
                    variations = listOf(
                        "快步向前头发飘动",
                        "转身回眸动态抓拍",
                        "跳跃瞬间定格"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                ),
                PoseTemplate(
                    id = "portrait_standing_over_shoulder",
                    name = "回眸一瞥",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "前肩自然下垂，后肩微耸",
                        head = "头部转向镜头超过 90 度回眸",
                        arms = "前臂自然垂放，后臂可微抬",
                        legs = "双腿前后站立，重心均匀分布",
                        hands = "手指自然放松，可轻触衣领或头发",
                        back = "背部朝向镜头，展现背部线条",
                        eyeContact = "回眸与镜头进行眼神交流",
                        bodyAngle = "身体背对镜头，仅头部回转",
                        hips = "髋部保持与背部同向",
                        feet = "双脚指向远离镜头方向",
                        chin = "下巴微收，侧脸线条清晰",
                        gaze = "深邃回眸，带有一丝神秘感"
                    ),
                    tips = listOf(
                        "回眸时先转身再转头，避免颈部扭曲",
                        "眼神可以略带惊讶或温柔",
                        "利用头发甩动增加动感",
                        "选择有景深的背景增加层次感",
                        "拍摄时注意颈部不要出现过多褶皱"
                    ),
                    variations = listOf(
                        "单手撩发回眸",
                        "微笑回眸看镜头",
                        "侧身45度回眸"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "portrait_standing_walking_pose",
                    name = "街拍步态",
                    category = PoseCategory.PORTRAIT_STANDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩自然平展，随步伐微晃",
                        head = "头部保持水平，可微侧向镜头",
                        arms = "一臂前摆一臂后摆，幅度自然",
                        legs = "步伐均匀，步幅比平时小 30%",
                        hands = "手可持包、咖啡杯等道具",
                        back = "背部挺直，核心微收保持姿态",
                        eyeContact = "自然前视，营造抓拍感",
                        bodyAngle = "身体与镜头呈 60-80 度角",
                        hips = "髋部随步伐节奏自然摆动",
                        feet = "脚掌滚动着地，步态轻盈",
                        chin = "下巴微收，保持面部轮廓清晰",
                        gaze = "目视前方或远方，营造故事感"
                    ),
                    tips = listOf(
                        "步幅比正常走路小三分之一",
                        "手持道具可以缓解手部尴尬",
                        "选择有特色的街道背景",
                        "保持匀速行走，不要突然变速",
                        "可以配合风吹动衣角和发丝"
                    ),
                    variations = listOf(
                        "手拿咖啡杯街拍",
                        "回头招手街拍",
                        "过马路动态抓拍"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                )
            )
        ),

        // ── PORTRAIT_SITTING: 4 个坐姿 ──
        PoseCategory.PORTRAIT_SITTING to mapOf(
            "sitting_poses" to listOf(
                PoseTemplate(
                    id = "portrait_sitting_formal_sitting",
                    name = "正式坐姿",
                    category = PoseCategory.PORTRAIT_SITTING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩后展下沉，保持端正对称",
                        head = "头部正直，微倾 2-3 度",
                        arms = "双臂自然搭在扶手或膝盖上",
                        legs = "双腿并拢或微错，膝盖朝前",
                        hands = "双手交叠放于膝盖或大腿上",
                        back = "背部挺直，不靠椅背，坐前三分之一",
                        eyeContact = "直视镜头，保持专业感",
                        bodyAngle = "身体正对镜头或微侧 10 度",
                        hips = "髋部正坐，重心均匀分布",
                        feet = "双脚平放地面，脚尖朝前",
                        chin = "下巴水平，保持颈部修长",
                        gaze = "自信沉稳的目光接触"
                    ),
                    tips = listOf(
                        "只坐椅面前三分之一，保持挺拔",
                        "膝盖并拢显得优雅端庄",
                        "双手交叠时手腕放松",
                        "选择简约背景突出人物",
                        "保持微笑但不过度"
                    ),
                    variations = listOf(
                        "双腿侧放正式坐姿",
                        "单手扶椅扶手",
                        "微微前倾交谈姿态"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "portrait_sitting_relaxed_sitting",
                    name = "放松坐姿",
                    category = PoseCategory.PORTRAIT_SITTING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩放松，一侧可略低",
                        head = "头部微侧，营造轻松氛围",
                        arms = "一臂搭在椅背，另一臂自然垂放",
                        legs = "一腿翘起搭在另一腿上",
                        hands = "上方手自然搭在膝盖，下方手放松",
                        back = "背部靠椅背但保持上背部挺直",
                        eyeContact = "轻松自然看向镜头",
                        bodyAngle = "身体可微侧 15-25 度",
                        hips = "髋部自然放松，重心偏向一侧",
                        feet = "翘起脚自然悬空或脚尖点地",
                        chin = "下巴微收，保持面部线条",
                        gaze = "轻松愉悦的眼神"
                    ),
                    tips = listOf(
                        "翘腿时注意不要翘太高",
                        "搭在椅背的手臂要自然弯曲",
                        "保持肩膀放松但不要塌陷",
                        "可以配合微笑增加亲和力",
                        "选择舒适的座椅和场景"
                    ),
                    variations = listOf(
                        "单手托腮放松坐姿",
                        "双手抱膝放松坐",
                        "侧坐扶手椅"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "portrait_sitting_creative_sitting",
                    name = "创意坐姿",
                    category = PoseCategory.PORTRAIT_SITTING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩一高一低，营造不对称美感",
                        head = "头部可大幅度倾斜 15-20 度",
                        arms = "一臂撑地，另一臂高举或伸展",
                        legs = "双腿可不对称摆放，一屈一伸",
                        hands = "手指有表现力地伸展或弯曲",
                        back = "背部可扭转，展现身体曲线",
                        eyeContact = "可以看向镜头外，增加艺术感",
                        bodyAngle = "身体大幅扭转，角度 30-60 度",
                        hips = "髋部倾斜，重心在支撑侧",
                        feet = "脚尖绷直或自然弯曲",
                        chin = "下巴上扬或侧转，展现不同角度",
                        gaze = "富有表现力的艺术性眼神"
                    ),
                    tips = listOf(
                        "利用不对称构图增加视觉张力",
                        "尝试不同寻常的手部姿势",
                        "背景选择简洁以突出创意造型",
                        "可以配合道具增加趣味性",
                        "注意身体线条的流畅性"
                    ),
                    variations = listOf(
                        "侧坐地面一手撑地",
                        "抱膝而坐低头沉思",
                        "盘腿而坐双手合十"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                ),
                PoseTemplate(
                    id = "portrait_sitting_ground_sitting",
                    name = "地面坐姿",
                    category = PoseCategory.PORTRAIT_SITTING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩放松，可微前倾",
                        head = "头部自然抬起，看向镜头",
                        arms = "双臂环抱膝盖或自然撑地",
                        legs = "双腿弯曲，膝盖并拢或微开",
                        hands = "手指自然搭在膝盖或小腿上",
                        back = "背部微弯，呈现自然弧度",
                        eyeContact = "柔和看向镜头，展现亲近感",
                        bodyAngle = "身体与地面呈 60-80 度角",
                        hips = "髋部贴地，重心均匀分布",
                        feet = "双脚交叉或并拢，脚尖自然",
                        chin = "下巴微收，避免仰头角度过大",
                        gaze = "温暖亲近的眼神"
                    ),
                    tips = listOf(
                        "选择干净的地面或铺上毯子",
                        "采用俯拍角度增加层次感",
                        "手部可以玩树叶或花瓣增加趣味",
                        "保持膝盖弯曲角度自然",
                        "利用自然光从侧面打光"
                    ),
                    variations = listOf(
                        "双腿伸直侧坐地面",
                        "盘腿而坐冥想姿势",
                        "趴在地面双手托腮"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                )
            )
        ),

        // ── COUPLE: 5 个双人姿势 ──
        PoseCategory.COUPLE to mapOf(
            "couple_poses" to listOf(
                PoseTemplate(
                    id = "couple_facing_each_other",
                    name = "面对面",
                    category = PoseCategory.COUPLE,
                    keypoints = PoseKeypoints(
                        shoulders = "两人双肩放松，可微触碰",
                        head = "两人头部靠近，形成亲密三角",
                        arms = "一人手臂搭在对方肩上或腰间",
                        legs = "两人腿部靠近，可微交错",
                        hands = "手牵手或一方手放在对方脸颊",
                        back = "两人背部微弓，前倾靠近对方",
                        eyeContact = "两人深情对视或同时看向镜头",
                        bodyAngle = "两人身体呈 30-45 度面对面",
                        hips = "髋部靠近但保持自然距离",
                        feet = "两人脚部靠近，形成亲密站姿",
                        chin = "两人下巴微收，避免碰撞",
                        gaze = "深情对视或一同看向镜头"
                    ),
                    tips = listOf(
                        "两人之间的距离以一拳为宜",
                        "可以额头相触增加亲密感",
                        "注意身高差，矮的一方可以微仰头",
                        "两人呼吸同步可以让表情更自然",
                        "选择简洁背景突出人物关系"
                    ),
                    variations = listOf(
                        "鼻尖相触的亲密面对",
                        "交谈大笑的自然瞬间",
                        "一人闭眼一人注视"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "couple_side_by_side",
                    name = "并肩而立",
                    category = PoseCategory.COUPLE,
                    keypoints = PoseKeypoints(
                        shoulders = "两人并肩，肩膀可轻触",
                        head = "两人头部微靠向对方",
                        arms = "内侧手臂相拥或牵手",
                        legs = "两人站姿协调，重心一致",
                        hands = "双手紧握或一方手搭在对方背部",
                        back = "两人背部挺直，形成统一画面",
                        eyeContact = "一同看向镜头或远方",
                        bodyAngle = "两人身体正对镜头，并肩站立",
                        hips = "髋部靠近，形成整体轮廓",
                        feet = "两人脚部齐平，站姿稳定",
                        chin = "下巴微收，保持面部对称",
                        gaze = "一致的视线方向"
                    ),
                    tips = listOf(
                        "身高差大时，矮者可靠近镜头",
                        "两人服装颜色协调但不雷同",
                        "可以一起看向同一个方向增加故事感",
                        "保持身体接触点至少有两处",
                        "利用前后站位增加层次感"
                    ),
                    variations = listOf(
                        "一人从背后环抱",
                        "并肩坐着的双人照",
                        "同向行走抓拍"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "couple_back_to_back",
                    name = "背靠背",
                    category = PoseCategory.COUPLE,
                    keypoints = PoseKeypoints(
                        shoulders = "两人肩背相靠，形成支撑",
                        head = "两人头部各自转向镜头方向",
                        arms = "双臂可交叉抱胸或自然下垂",
                        legs = "双腿微分开，形成稳定支撑",
                        hands = "手指可交叉或自然摆放",
                        back = "两人背部紧贴，形成 V 字形",
                        eyeContact = "各自看向镜头或对视",
                        bodyAngle = "两人背对背呈 180 度",
                        hips = "髋部接触形成支撑点",
                        feet = "双脚分开与肩同宽，稳定站立",
                        chin = "下巴微抬，展现自信",
                        gaze = "各自展现不同表情增加趣味"
                    ),
                    tips = listOf(
                        "两人背部接触面积要大，展现信任",
                        "可以一人严肃一人微笑形成对比",
                        "穿着风格对比强烈的服装",
                        "利用对称构图增加视觉冲击力",
                        "可以尝试一人闭眼一人睁眼"
                    ),
                    variations = listOf(
                        "背靠背坐在地上",
                        "背靠背一人看左一人看右",
                        "背靠背手持道具"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "couple_piggyback",
                    name = "背人姿势",
                    category = PoseCategory.COUPLE,
                    keypoints = PoseKeypoints(
                        shoulders = "背负者双肩承载重量，被背者双肩放松",
                        head = "被背者头靠背负者肩部或侧望镜头",
                        arms = "背负者双手托住对方大腿，被背者环抱对方颈部",
                        legs = "背负者双腿微曲承受重量",
                        hands = "背负者手部用力托稳，被背者手部放松",
                        back = "背负者背部微弓承担重量",
                        eyeContact = "两人一同看向镜头，笑容灿烂",
                        bodyAngle = "两人身体重叠，形成一体",
                        hips = "背负者髋部后坐保持平衡",
                        feet = "背负者双脚分开与肩同宽",
                        chin = "两人下巴微收，展现快乐表情",
                        gaze = "开心的笑容和眼神"
                    ),
                    tips = listOf(
                        "确保背负者体力足够，安全第一",
                        "被背者双腿夹紧保持稳定",
                        "两人表情要自然开心",
                        "选择开阔场地拍摄",
                        "可以慢跑增加动感"
                    ),
                    variations = listOf(
                        "正面公主抱",
                        "侧身熊抱",
                        "跳跃背人瞬间"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                ),
                PoseTemplate(
                    id = "couple_forehead_touch",
                    name = "额头相触",
                    category = PoseCategory.COUPLE,
                    keypoints = PoseKeypoints(
                        shoulders = "两人双肩放松，身体靠近",
                        head = "两人额头轻触，形成亲密联系",
                        arms = "一人手臂环抱对方腰部",
                        legs = "两人腿部靠近，可微交错",
                        hands = "一手轻抚对方脸颊或后颈",
                        back = "两人背部微弓，身体前倾靠近",
                        eyeContact = "两人闭眼享受当下或微睁对视",
                        bodyAngle = "两人身体呈 20-30 度角",
                        hips = "髋部靠近，身体接触",
                        feet = "两人脚部靠近，站立稳定",
                        chin = "下巴微收，额头相触",
                        gaze = "闭眼感受或微睁深情对视"
                    ),
                    tips = listOf(
                        "额头接触要轻柔，不要用力",
                        "闭眼可以增加情感深度",
                        "利用侧光突出面部轮廓",
                        "两人呼吸同步，保持画面宁静",
                        "选择温暖色调的背景"
                    ),
                    variations = listOf(
                        "额头相触鼻尖轻碰",
                        "闭眼微笑额头相触",
                        "侧脸额头相触剪影"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                )
            )
        ),

        // ── CHILDREN: 4 个儿童姿势 ──
        PoseCategory.CHILDREN to mapOf(
            "children_poses" to listOf(
                PoseTemplate(
                    id = "children_playful_sitting",
                    name = "顽皮坐姿",
                    category = PoseCategory.CHILDREN,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩自然放松，可一高一低",
                        head = "头部可歪向一侧，展现天真",
                        arms = "双臂可撑地或环抱玩具",
                        legs = "双腿随意摆放，W 坐姿或盘腿",
                        hands = "小手可拿玩具、花瓣或自然摆放",
                        back = "背部可微弯，展现儿童自然体态",
                        eyeContact = "睁大眼睛看向镜头",
                        bodyAngle = "身体可正对或侧对镜头",
                        hips = "髋部自然贴地",
                        feet = "小脚丫自然摆放",
                        chin = "下巴微抬，展现天真表情",
                        gaze = "纯真好奇的眼神"
                    ),
                    tips = listOf(
                        "蹲下与孩子平视拍摄",
                        "用玩具或泡泡吸引孩子注意力",
                        "抓拍比摆拍更适合儿童",
                        "利用自然光营造温暖氛围",
                        "让孩子在熟悉的环境中拍摄"
                    ),
                    variations = listOf(
                        "趴在地上双手托腮",
                        "坐在地上玩玩具",
                        "盘腿大笑"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "children_curious_standing",
                    name = "好奇站立",
                    category = PoseCategory.CHILDREN,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩微耸，展现好奇姿态",
                        head = "头部微仰，看向高处或远方",
                        arms = "一手指向远处，另一手自然下垂",
                        legs = "双腿微分开站立",
                        hands = "手指指向感兴趣的方向",
                        back = "背部挺直微前倾",
                        eyeContact = "睁大好奇的眼睛看向某处",
                        bodyAngle = "身体微侧，面向兴趣点",
                        hips = "髋部自然水平",
                        feet = "双脚分开与肩同宽",
                        chin = "下巴微抬，仰望姿态",
                        gaze = "充满好奇和探索欲的眼神"
                    ),
                    tips = listOf(
                        "利用孩子对新鲜事物的好奇心",
                        "在户外自然环境中拍摄",
                        "使用长焦镜头远距离抓拍",
                        "选择黄金时段的光线",
                        "让孩子探索时保持安全距离"
                    ),
                    variations = listOf(
                        "踮脚伸手够东西",
                        "蹲下观察昆虫",
                        "趴在窗边看外面"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "children_running_free",
                    name = "自由奔跑",
                    category = PoseCategory.CHILDREN,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩自然摆动",
                        head = "头部自然抬起，头发随风飘扬",
                        arms = "双臂张开或自然摆动",
                        legs = "双腿大步奔跑",
                        hands = "手指自然张开",
                        back = "背部微前倾跑步姿态",
                        eyeContact = "看向前方或回头看向镜头",
                        bodyAngle = "身体前倾奔跑角度",
                        hips = "髋部随跑步节奏摆动",
                        feet = "双脚交替着地奔跑",
                        chin = "下巴自然抬起",
                        gaze = "自由快乐的眼神"
                    ),
                    tips = listOf(
                        "使用高速快门冻结动作",
                        "选择开阔安全的场地",
                        "逆光拍摄可以增加梦幻感",
                        "让孩子在草地上自由奔跑",
                        "连拍模式捕捉最佳瞬间"
                    ),
                    variations = listOf(
                        "张开双臂奔跑",
                        "追逐泡泡奔跑",
                        "跳跃瞬间定格"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                ),
                PoseTemplate(
                    id = "children_peekaboo",
                    name = "躲猫猫",
                    category = PoseCategory.CHILDREN,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩微耸，躲在遮挡物后",
                        head = "头部从遮挡物后探出",
                        arms = "双手扶着遮挡物边缘",
                        legs = "双腿站在遮挡物后方",
                        hands = "小手抓着边缘或捂住一只眼睛",
                        back = "背部隐藏在遮挡物后",
                        eyeContact = "一只眼睛或半张脸露出看向镜头",
                        bodyAngle = "身体大部分隐藏在遮挡物后",
                        hips = "髋部在遮挡物后方",
                        feet = "双脚隐藏在遮挡物后",
                        chin = "下巴微收，露出半张脸",
                        gaze = "狡黠调皮的眼神"
                    ),
                    tips = listOf(
                        "利用门框、树干、窗帘等作为遮挡物",
                        "让孩子自然玩耍时抓拍",
                        "只露出一只眼睛更有趣味",
                        "可以配合夸张的表情",
                        "使用大光圈虚化前后景"
                    ),
                    variations = listOf(
                        "从门后探出半个身子",
                        "用手捂住眼睛再打开",
                        "躲在窗帘后面露出脚"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                )
            )
        ),

        // ── FOOD: 4 个美食姿势 ──
        PoseCategory.FOOD to mapOf(
            "food_poses" to listOf(
                PoseTemplate(
                    id = "food_top_down",
                    name = "俯拍平面",
                    category = PoseCategory.FOOD,
                    keypoints = PoseKeypoints(
                        shoulders = "拍摄者双肩保持水平稳定",
                        head = "头部正对下方，保持垂直视角",
                        arms = "双臂稳定持机，肘部夹紧身体",
                        legs = "双腿分开站立保持稳定",
                        hands = "双手稳定握持手机或相机",
                        back = "背部微弯俯身",
                        eyeContact = "注视屏幕构图",
                        bodyAngle = "身体垂直于拍摄平面",
                        hips = "髋部保持稳定",
                        feet = "双脚分开与肩同宽",
                        chin = "下巴微收，不影响视线",
                        gaze = "专注于屏幕构图"
                    ),
                    tips = listOf(
                        "使用手机水平仪确保完全垂直",
                        "选择干净的背景桌面或餐布",
                        "利用自然光从窗户侧面打光",
                        "注意食物摆放的构图和配色",
                        "加入手部动作增加生活感"
                    ),
                    variations = listOf(
                        "手持餐具入镜俯拍",
                        "俯拍撒粉瞬间",
                        "俯拍倒饮品动作"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "food_forty_five_degree",
                    name = "45度角拍摄",
                    category = PoseCategory.FOOD,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩稳定，保持拍摄角度",
                        head = "头部微低，注视屏幕",
                        arms = "双臂微曲，稳定持机",
                        legs = "可坐可站，保持稳定",
                        hands = "双手稳定持机，角度 45 度",
                        back = "背部微前倾或保持正直",
                        eyeContact = "注视屏幕确认构图",
                        bodyAngle = "身体与桌面呈 45 度角",
                        hips = "髋部保持稳定",
                        feet = "双脚稳定支撑",
                        chin = "下巴自然位置",
                        gaze = "专注屏幕确认焦点"
                    ),
                    tips = listOf(
                        "45 度是最接近人眼用餐视角的角度",
                        "利用前景虚化增加层次感",
                        "注意光线从侧面或后方打来",
                        "拍摄时注意背景不要杂乱",
                        "可以加入人手动作增加温度感"
                    ),
                    variations = listOf(
                        "手持筷子夹菜瞬间",
                        "倒酱汁的45度抓拍",
                        "切牛排的45度角"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "food_flat_lay",
                    name = "平面摆拍",
                    category = PoseCategory.FOOD,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩保持水平稳定",
                        head = "头部正对下方平面",
                        arms = "双臂稳定持机，或单手操作",
                        legs = "站立或使用梯子保持稳定",
                        hands = "一手持机一手可摆放道具",
                        back = "背部微弯俯身",
                        eyeContact = "注视屏幕或实物",
                        bodyAngle = "身体垂直于拍摄平面",
                        hips = "髋部保持稳定平衡",
                        feet = "双脚分开保持稳定",
                        chin = "下巴微收不遮挡视线",
                        gaze = "专注构图和细节"
                    ),
                    tips = listOf(
                        "使用三脚架确保画面稳定",
                        "注意色彩搭配和构图平衡",
                        "加入相关道具增加故事感",
                        "利用负空间让画面呼吸",
                        "选择统一的色调和风格"
                    ),
                    variations = listOf(
                        "加入人手操作的flat lay",
                        "食材散落的艺术flat lay",
                        "对称构图的flat lay"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "food_action_shot",
                    name = "动态抓拍",
                    category = PoseCategory.FOOD,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩稳定，随时准备抓拍",
                        head = "头部保持稳定，注视取景器",
                        arms = "双臂稳定持机，随时按下快门",
                        legs = "双腿微曲，机动灵活",
                        hands = "手指放在快门上，随时抓拍",
                        back = "背部微前倾，保持机动",
                        eyeContact = "紧盯拍摄对象",
                        bodyAngle = "身体角度灵活调整",
                        hips = "髋部灵活转动",
                        feet = "双脚一前一后，随时移动",
                        chin = "下巴自然位置",
                        gaze = "高度专注，捕捉瞬间"
                    ),
                    tips = listOf(
                        "使用高速快门冻结动作",
                        "预判动作轨迹提前构图",
                        "使用连拍模式增加成功率",
                        "注意液体飞溅的光线效果",
                        "背景简洁以突出主体动作"
                    ),
                    variations = listOf(
                        "撒糖粉瞬间抓拍",
                        "倒咖啡的漩涡抓拍",
                        "切开流心蛋的瞬间"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                )
            )
        ),

        // ── LANDSCAPE: 4 个风景人像姿势 ──
        PoseCategory.LANDSCAPE to mapOf(
            "landscape_poses" to listOf(
                PoseTemplate(
                    id = "landscape_silhouette_pose",
                    name = "剪影姿态",
                    category = PoseCategory.LANDSCAPE,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩展开，形成清晰轮廓",
                        head = "头部侧转向镜头方向",
                        arms = "双臂可张开或举高",
                        legs = "双腿分开站立，形成稳定三角",
                        hands = "手指自然分开，形成优美剪影",
                        back = "背部挺直，展现身体轮廓",
                        eyeContact = "面向光源方向或远方",
                        bodyAngle = "身体侧对光源，形成剪影",
                        hips = "髋部保持稳定",
                        feet = "双脚分开与肩同宽",
                        chin = "下巴微抬，展现侧脸轮廓",
                        gaze = "面向远方或光源"
                    ),
                    tips = listOf(
                        "选择日出日落时段拍摄",
                        "确保人物在光源和相机之间",
                        "动作要大开大合，轮廓才清晰",
                        "使用点测光对天空测光",
                        "后期可以增加对比度强化剪影"
                    ),
                    variations = listOf(
                        "跳跃剪影定格",
                        "牵手剪影",
                        "瑜伽姿势剪影"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "landscape_pointing_away",
                    name = "指向远方",
                    category = PoseCategory.LANDSCAPE,
                    keypoints = PoseKeypoints(
                        shoulders = "一侧肩膀微抬指向远方",
                        head = "头部转向手指方向",
                        arms = "一臂伸直指向远方，另一臂自然下垂",
                        legs = "双腿前后站立，稳定支撑",
                        hands = "指向远方的手指伸直，四指并拢",
                        back = "背部挺直或微侧",
                        eyeContact = "看向手指指向的方向",
                        bodyAngle = "身体侧对镜头，面朝远方",
                        hips = "髋部转向指向方向",
                        feet = "双脚一前一后稳定站立",
                        chin = "下巴微抬，仰望远方",
                        gaze = "带着向往和探索的眼神"
                    ),
                    tips = listOf(
                        "手指指向有趣的地标或风景",
                        "利用引导线构图将视线引向远方",
                        "人物放在画面三分之一处",
                        "选择有纵深感的场景",
                        "穿着与风景对比的服装颜色"
                    ),
                    variations = listOf(
                        "双手指向不同方向",
                        "坐在地上指向远方",
                        "站在高处俯瞰指向"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "landscape_sitting_scenic",
                    name = "风景坐姿",
                    category = PoseCategory.LANDSCAPE,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩放松，面向风景",
                        head = "头部微侧，看向风景方向",
                        arms = "双臂环抱膝盖或撑在身后",
                        legs = "双腿弯曲或伸直，自然摆放",
                        hands = "手指自然搭在膝盖或地面",
                        back = "背部微弯，放松姿态",
                        eyeContact = "看向远方风景",
                        bodyAngle = "身体侧对或背对镜头",
                        hips = "髋部贴地或座椅",
                        feet = "双脚自然摆放",
                        chin = "下巴微抬，欣赏风景",
                        gaze = "沉浸于风景之中"
                    ),
                    tips = listOf(
                        "选择视野开阔的拍摄位置",
                        "人物放在画面下方三分之一处",
                        "利用黄金时段光线",
                        "穿着与风景协调的服装",
                        "可以加入帽子等配饰"
                    ),
                    variations = listOf(
                        "坐在悬崖边看日落",
                        "野餐垫上欣赏风景",
                        "长椅上坐着看远方"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "landscape_walking_into",
                    name = "走入风景",
                    category = PoseCategory.LANDSCAPE,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩自然摆动，背对或侧对镜头",
                        head = "头部可微侧回望",
                        arms = "双臂自然摆动或一手持物",
                        legs = "双腿自然行走",
                        hands = "手指自然放松",
                        back = "背部朝向镜头或微侧",
                        eyeContact = "可以回眸看向镜头",
                        bodyAngle = "身体远离镜头方向",
                        hips = "髋部随步伐自然摆动",
                        feet = "脚步轻盈走入风景",
                        chin = "下巴自然位置",
                        gaze = "走向远方的故事感"
                    ),
                    tips = listOf(
                        "人物在画面中占比较小，突出风景",
                        "利用道路或小径作为引导线",
                        "拍摄背影增加故事感和想象空间",
                        "选择有延伸感的场景",
                        "人物穿着颜色与风景形成对比"
                    ),
                    variations = listOf(
                        "手牵手走入风景",
                        "骑自行车远去",
                        "奔跑着融入风景"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                )
            )
        ),

        // ── WEDDING: 4 个婚礼姿势 ──
        PoseCategory.WEDDING to mapOf(
            "wedding_poses" to listOf(
                PoseTemplate(
                    id = "wedding_bouquet_hold",
                    name = "手持捧花",
                    category = PoseCategory.WEDDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩后展下沉，展现优雅",
                        head = "头部微低看向捧花或微抬看镜头",
                        arms = "双臂微曲，双手捧花于腰间",
                        legs = "双腿并拢或微错开",
                        hands = "双手轻捧花束，手指自然弯曲",
                        back = "背部挺直，展现婚纱线条",
                        eyeContact = "低头看花或温柔看向镜头",
                        bodyAngle = "身体微侧 15-20 度",
                        hips = "髋部微转展现裙摆",
                        feet = "双脚并拢，脚尖微开",
                        chin = "下巴微收，温柔微笑",
                        gaze = "温柔幸福的眼神"
                    ),
                    tips = listOf(
                        "捧花位置在腰部略下，不要太高压住脸",
                        "手指自然环绕花茎，不要用力抓握",
                        "婚纱裙摆自然展开增加画面感",
                        "选择侧光突出婚纱纹理",
                        "背景简洁以突出新娘"
                    ),
                    variations = listOf(
                        "单手捧花侧身站立",
                        "高举捧花开心大笑",
                        "捧花遮住半张脸"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "wedding_ring_exchange",
                    name = "交换戒指",
                    category = PoseCategory.WEDDING,
                    keypoints = PoseKeypoints(
                        shoulders = "两人双肩靠近，身体微倾",
                        head = "两人头部靠近，注视戒指",
                        arms = "一人伸手，另一人戴戒指",
                        legs = "两人站姿靠近",
                        hands = "一人手心向上伸出，另一人手持戒指",
                        back = "两人背部微弯前倾",
                        eyeContact = "两人注视戒指或深情对视",
                        bodyAngle = "两人面对面呈 30-60 度角",
                        hips = "髋部靠近",
                        feet = "两人脚部靠近",
                        chin = "两人下巴微收",
                        gaze = "专注而深情的眼神"
                    ),
                    tips = listOf(
                        "聚焦手部动作，使用大光圈虚化背景",
                        "从侧面拍摄可以同时拍到两人表情",
                        "注意手部姿势要优雅自然",
                        "使用微距镜头拍摄戒指细节",
                        "温暖的光线增加仪式感"
                    ),
                    variations = listOf(
                        "戒指特写手部交叠",
                        "戴戒指时深情对视",
                        "戴完戒指后牵手展示"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "wedding_first_dance",
                    name = "第一支舞",
                    category = PoseCategory.WEDDING,
                    keypoints = PoseKeypoints(
                        shoulders = "两人双肩靠近，形成舞姿框架",
                        head = "两人头部靠近，可额头相触",
                        arms = "一人手搭对方肩部，另一人手放对方腰间",
                        legs = "两人腿部微错开，舞步姿态",
                        hands = "两人一手相握举起，另一手自然放位",
                        back = "两人背部挺直，展现优雅舞姿",
                        eyeContact = "两人深情对视",
                        bodyAngle = "两人身体贴近，呈舞姿角度",
                        hips = "髋部靠近",
                        feet = "舞步姿态，一前一后",
                        chin = "两人下巴微抬",
                        gaze = "沉浸在二人世界的眼神"
                    ),
                    tips = listOf(
                        "使用慢速快门营造动感",
                        "从多个角度拍摄舞姿",
                        "注意裙摆的飘动效果",
                        "利用环境光营造浪漫氛围",
                        "抓拍旋转或下腰的精彩瞬间"
                    ),
                    variations = listOf(
                        "旋转时裙摆飞扬",
                        "下腰姿势定格",
                        "慢舞拥抱特写"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                ),
                PoseTemplate(
                    id = "wedding_veil_flow",
                    name = "头纱飘扬",
                    category = PoseCategory.WEDDING,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩后展，展现优雅姿态",
                        head = "头部微仰，让头纱自然垂落",
                        arms = "一臂轻抚头纱，另一臂自然下垂",
                        legs = "双腿前后站立，重心稳定",
                        hands = "手指轻捻头纱边缘",
                        back = "背部挺直，展现背部线条",
                        eyeContact = "温柔看向镜头或远方",
                        bodyAngle = "身体微侧 20-30 度",
                        hips = "髋部微转",
                        feet = "双脚一前一后",
                        chin = "下巴微抬，拉长颈部",
                        gaze = "梦幻而温柔的眼神"
                    ),
                    tips = listOf(
                        "利用微风让头纱自然飘动",
                        "逆光拍摄头纱呈现透明质感",
                        "选择开阔的户外场地",
                        "助手可以在旁边抛动头纱",
                        "使用高速快门定格头纱飘动"
                    ),
                    variations = listOf(
                        "头纱盖面朦胧美",
                        "头纱随风飘扬",
                        "新郎掀开头纱的瞬间"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                )
            )
        ),

        // ── PRODUCT: 4 个产品拍摄姿势 ──
        PoseCategory.PRODUCT to mapOf(
            "product_poses" to listOf(
                PoseTemplate(
                    id = "product_flat_lay",
                    name = "产品平铺",
                    category = PoseCategory.PRODUCT,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩保持水平稳定",
                        head = "头部正对下方平面",
                        arms = "双臂稳定持机",
                        legs = "站立或使用梯子",
                        hands = "双手稳定相机或手机",
                        back = "背部微弯俯身",
                        eyeContact = "注视屏幕确认构图",
                        bodyAngle = "身体垂直于拍摄平面",
                        hips = "髋部保持稳定",
                        feet = "双脚分开保持平衡",
                        chin = "下巴微收",
                        gaze = "专注于构图和光线"
                    ),
                    tips = listOf(
                        "使用纯色背景突出产品",
                        "注意产品摆放的构图和间距",
                        "加入相关道具增加场景感",
                        "使用柔光箱消除阴影",
                        "确保画面完全水平"
                    ),
                    variations = listOf(
                        "产品与原材料搭配flat lay",
                        "使用场景flat lay",
                        "极简产品flat lay"
                    ),
                    difficulty = PoseDifficulty.BEGINNER
                ),
                PoseTemplate(
                    id = "product_angled_display",
                    name = "角度展示",
                    category = PoseCategory.PRODUCT,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩稳定，保持拍摄角度",
                        head = "头部微侧，注视屏幕",
                        arms = "双臂稳定持机，灵活调整角度",
                        legs = "可坐可站，稳定支撑",
                        hands = "双手持机或一手调整产品",
                        back = "背部保持稳定",
                        eyeContact = "注视屏幕",
                        bodyAngle = "身体与产品呈 30-60 度",
                        hips = "髋部稳定",
                        feet = "双脚稳定支撑",
                        chin = "下巴自然位置",
                        gaze = "关注焦点和构图"
                    ),
                    tips = listOf(
                        "展示产品最有特色的角度",
                        "利用侧光突出产品质感",
                        "使用大光圈虚化背景",
                        "注意产品表面的反光控制",
                        "加入手持动作增加使用感"
                    ),
                    variations = listOf(
                        "手持产品45度展示",
                        "产品放在展示台上斜拍",
                        "悬空产品创意展示"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "product_in_use",
                    name = "使用场景",
                    category = PoseCategory.PRODUCT,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩放松，自然使用产品",
                        head = "头部自然位置，可微低看产品",
                        arms = "双臂自然使用产品",
                        legs = "自然站立或坐下",
                        hands = "双手自然操作产品",
                        back = "背部自然姿态",
                        eyeContact = "看向产品或自然前视",
                        bodyAngle = "身体自然角度",
                        hips = "髋部自然放松",
                        feet = "自然站立或坐姿",
                        chin = "下巴自然位置",
                        gaze = "专注使用产品"
                    ),
                    tips = listOf(
                        "展示产品真实使用场景",
                        "模特动作要自然不做作",
                        "选择符合产品调性的场景",
                        "注意手部姿势的优雅",
                        "产品logo要清晰可见"
                    ),
                    variations = listOf(
                        "模特手持产品特写",
                        "生活场景中使用产品",
                        "户外使用产品场景"
                    ),
                    difficulty = PoseDifficulty.INTERMEDIATE
                ),
                PoseTemplate(
                    id = "product_detail_closeup",
                    name = "细节特写",
                    category = PoseCategory.PRODUCT,
                    keypoints = PoseKeypoints(
                        shoulders = "双肩稳定，保持相机稳定",
                        head = "头部贴近取景器或屏幕",
                        arms = "双臂夹紧身体稳定相机",
                        legs = "稳定站姿或使用三脚架",
                        hands = "双手稳定相机，微调对焦",
                        back = "背部微弯靠近产品",
                        eyeContact = "紧盯屏幕确认焦点",
                        bodyAngle = "身体靠近产品",
                        hips = "髋部稳定",
                        feet = "双脚稳定支撑",
                        chin = "下巴微收",
                        gaze = "高度专注细节"
                    ),
                    tips = listOf(
                        "使用微距镜头或微距模式",
                        "使用三脚架确保稳定",
                        "注意光线角度避免阴影",
                        "焦点对准产品关键细节",
                        "使用小光圈增加景深"
                    ),
                    variations = listOf(
                        "产品纹理特写",
                        "产品logo特写",
                        "产品材质质感特写"
                    ),
                    difficulty = PoseDifficulty.ADVANCED
                )
            )
        )
    )

    // =========================================================================
    // 推荐规则 — 场景类型 → 推荐姿势类别
    // =========================================================================

    private val recommendationRules: Map<SceneType, List<PoseCategory>> = mapOf(
        SceneType.PORTRAIT to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING
        ),
        SceneType.PORTRAIT_STANDING to listOf(
            PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.PORTRAIT_SITTING to listOf(
            PoseCategory.PORTRAIT_SITTING
        ),
        SceneType.COUPLE to listOf(
            PoseCategory.COUPLE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.CHILDREN to listOf(
            PoseCategory.CHILDREN
        ),
        SceneType.FOOD to listOf(
            PoseCategory.FOOD
        ),
        SceneType.LANDSCAPE to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.WEDDING to listOf(
            PoseCategory.WEDDING, PoseCategory.COUPLE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.PRODUCT to listOf(
            PoseCategory.PRODUCT
        ),
        SceneType.BACKLIT to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.GROUP to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.COUPLE
        ),
        SceneType.SELFIE to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.COUPLE
        ),
        SceneType.NIGHT to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.SUNSET to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.URBAN to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.LANDSCAPE
        ),
        SceneType.NATURE to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.INDOOR to listOf(
            PoseCategory.PORTRAIT_SITTING, PoseCategory.PORTRAIT_STANDING, PoseCategory.PRODUCT
        ),
        SceneType.OUTDOOR to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.LANDSCAPE, PoseCategory.CHILDREN
        ),
        SceneType.SILHOUETTE to listOf(
            PoseCategory.LANDSCAPE
        ),
        SceneType.ACTION to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.CHILDREN
        ),
        SceneType.STILL_LIFE to listOf(
            PoseCategory.PRODUCT, PoseCategory.FOOD
        ),
        SceneType.MACRO to listOf(
            PoseCategory.PRODUCT, PoseCategory.FOOD
        ),
        SceneType.TRAVEL to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.STREET to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.LANDSCAPE
        ),
        SceneType.ARCHITECTURE to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.PET to listOf(
            PoseCategory.CHILDREN
        ),
        SceneType.SPORTS to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.LANDSCAPE
        ),
        SceneType.EVENT to listOf(
            PoseCategory.WEDDING, PoseCategory.COUPLE, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.FASHION to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING
        ),
        SceneType.BEAUTY to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING
        ),
        SceneType.DOCUMENTARY to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.LANDSCAPE
        ),
        SceneType.MINIMAL to listOf(
            PoseCategory.PRODUCT, PoseCategory.PORTRAIT_STANDING
        ),
        SceneType.VINTAGE to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING
        ),
        SceneType.CINEMATIC to listOf(
            PoseCategory.LANDSCAPE, PoseCategory.PORTRAIT_STANDING, PoseCategory.COUPLE
        ),
        SceneType.UNKNOWN to listOf(
            PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING, PoseCategory.LANDSCAPE
        )
    )

    // =========================================================================
    // 分析规则 — 置信度规则 & 人数规则
    // =========================================================================

    private val analysisRules: Map<String, Map<String, Any>> = mapOf(
        "confidence" to mapOf(
            "high" to mapOf(
                "threshold" to 0.8f,
                "maxSuggestions" to 5,
                "includeAdvanced" to true
            ),
            "medium" to mapOf(
                "threshold" to 0.5f,
                "maxSuggestions" to 3,
                "includeAdvanced" to false
            ),
            "low" to mapOf(
                "threshold" to 0.0f,
                "maxSuggestions" to 2,
                "includeAdvanced" to false
            )
        ),
        "people_count" to mapOf(
            "single" to mapOf(
                "minCount" to 1,
                "maxCount" to 1,
                "categories" to listOf(
                    PoseCategory.PORTRAIT_STANDING, PoseCategory.PORTRAIT_SITTING
                )
            ),
            "couple" to mapOf(
                "minCount" to 2,
                "maxCount" to 2,
                "categories" to listOf(
                    PoseCategory.COUPLE, PoseCategory.PORTRAIT_STANDING
                )
            ),
            "group" to mapOf(
                "minCount" to 3,
                "maxCount" to Int.MAX_VALUE,
                "categories" to listOf(
                    PoseCategory.PORTRAIT_STANDING, PoseCategory.COUPLE
                )
            )
        )
    )

    // =========================================================================
    // 引导资源 — 姿势 ID → 资源路径
    // =========================================================================

    private val guideAssets: Map<String, String> = mapOf(
        "portrait_standing_natural_standing" to "poses/portrait/natural_standing.png",
        "portrait_standing_confident_pose" to "poses/portrait/confident_pose.png",
        "portrait_standing_casual_lean" to "poses/portrait/casual_lean.png",
        "portrait_standing_dynamic_movement" to "poses/portrait/dynamic_movement.png",
        "portrait_standing_over_shoulder" to "poses/portrait/over_shoulder.png",
        "portrait_standing_walking_pose" to "poses/portrait/walking_pose.png",
        "portrait_sitting_formal_sitting" to "poses/portrait/formal_sitting.png",
        "portrait_sitting_relaxed_sitting" to "poses/portrait/relaxed_sitting.png",
        "portrait_sitting_creative_sitting" to "poses/portrait/creative_sitting.png",
        "portrait_sitting_ground_sitting" to "poses/portrait/ground_sitting.png",
        "couple_facing_each_other" to "poses/couple/facing_each_other.png",
        "couple_side_by_side" to "poses/couple/side_by_side.png",
        "couple_back_to_back" to "poses/couple/back_to_back.png",
        "couple_piggyback" to "poses/couple/piggyback.png",
        "couple_forehead_touch" to "poses/couple/forehead_touch.png",
        "children_playful_sitting" to "poses/children/playful_sitting.png",
        "children_curious_standing" to "poses/children/curious_standing.png",
        "children_running_free" to "poses/children/running_free.png",
        "children_peekaboo" to "poses/children/peekaboo.png",
        "food_top_down" to "poses/food/top_down.png",
        "food_forty_five_degree" to "poses/food/forty_five_degree.png",
        "food_flat_lay" to "poses/food/flat_lay.png",
        "food_action_shot" to "poses/food/action_shot.png",
        "landscape_silhouette_pose" to "poses/landscape/silhouette_pose.png",
        "landscape_pointing_away" to "poses/landscape/pointing_away.png",
        "landscape_sitting_scenic" to "poses/landscape/sitting_scenic.png",
        "landscape_walking_into" to "poses/landscape/walking_into.png",
        "wedding_bouquet_hold" to "poses/wedding/bouquet_hold.png",
        "wedding_ring_exchange" to "poses/wedding/ring_exchange.png",
        "wedding_first_dance" to "poses/wedding/first_dance.png",
        "wedding_veil_flow" to "poses/wedding/veil_flow.png",
        "product_flat_lay" to "poses/product/flat_lay.png",
        "product_angled_display" to "poses/product/angled_display.png",
        "product_in_use" to "poses/product/in_use.png",
        "product_detail_closeup" to "poses/product/detail_closeup.png"
    )

    // =========================================================================
    // 公开方法
    // =========================================================================

    /**
     * 根据场景类型、置信度和被摄主体检测结果生成姿势推荐
     */
    fun generateRecommendations(
        scene: SceneType,
        confidence: Float,
        subjectDetection: SubjectDetection
    ): PoseRecommendationResult {
        val confidenceLevel = getConfidenceLevel(confidence)
        val confidenceRules = getConfidenceRules(confidenceLevel)
        val maxSuggestions = (confidenceRules["maxSuggestions"] as? Int) ?: 3
        val includeAdvanced = (confidenceRules["includeAdvanced"] as? Boolean) ?: false

        val applicableCategories = recommendationRules[scene]
            ?: recommendationRules[SceneType.UNKNOWN]!!

        val allPoses = applicableCategories.flatMap { category ->
            poseDatabase[category]?.values?.flatten() ?: emptyList()
        }

        val filteredPoses = if (!includeAdvanced) {
            allPoses.filter { it.difficulty != PoseDifficulty.ADVANCED }
        } else {
            allPoses
        }

        val poseSuggestions = generatePoseSuggestions(filteredPoses, scene, confidence)
        val adjustmentSuggestions = generateAdjustmentSuggestions(subjectDetection)
        var compositionSuggestions = generateCompositionSuggestions(scene, subjectDetection)
        val dynamicSuggestions = generateDynamicSuggestions(subjectDetection, scene)

        // Backlit scene special handling
        if (scene == SceneType.BACKLIT) {
            val backlitSuggestions = generateBacklitSuggestions()
            // Add backlit suggestions with high priority
            compositionSuggestions += backlitSuggestions.take(2)
        }

        val prioritizedPoses = prioritizeAndLimit(poseSuggestions, maxSuggestions)

        // If no valid suggestions found, use fallback recommendations
        val finalSuggestions = if (prioritizedPoses.isEmpty()) {
            getFallbackRecommendations()
        } else {
            prioritizedPoses
        }

        val primaryRecommendation = finalSuggestions.firstOrNull()

        return PoseRecommendationResult(
            suggestions = finalSuggestions,
            adjustments = adjustmentSuggestions,
            compositions = compositionSuggestions + dynamicSuggestions,
            primaryRecommendation = primaryRecommendation,
            sceneType = scene,
            confidenceScore = confidence
        )
    }

    /**
     * 生成逆光场景的特别建议
     */
    fun generateBacklitSuggestions(): List<PoseSuggestion> {
        val silhouettePoses = poseDatabase[PoseCategory.LANDSCAPE]
            ?.values?.flatten()
            ?.filter { it.id.contains("silhouette") }
            ?: emptyList()

        val backlitPoses = poseDatabase[PoseCategory.PORTRAIT_STANDING]
            ?.values?.flatten()
            ?.filter { it.id.contains("over_shoulder") || it.id.contains("dynamic") }
            ?: emptyList()

        val allBacklitPoses = silhouettePoses + backlitPoses

        return allBacklitPoses.map { template ->
            createPoseSuggestion(template, SceneType.BACKLIT, 0.9f)
        }.sortedByDescending { it.priority }
    }

    // =========================================================================
    // 私有方法
    // =========================================================================

    /**
     * 根据场景类型过滤并生成姿势建议
     */
    private fun generatePoseSuggestions(
        poses: List<PoseTemplate>,
        scene: SceneType,
        confidence: Float
    ): List<PoseSuggestion> {
        return poses.map { template ->
            val priority = calculatePosePriority(template, scene, confidence)
            createPoseSuggestion(template, scene, priority)
        }.sortedByDescending { it.priority }
    }

    /**
     * 基于被摄主体检测生成调整建议
     * - 面部位置分析 → 头部位置调整
     * - 主体大小分析 → 距离调整
     */
    private fun generateAdjustmentSuggestions(
        subjectDetection: SubjectDetection
    ): List<PoseSuggestion> {
        val adjustments = mutableListOf<PoseSuggestion>()

        val faceRect = subjectDetection.faceRect

        if (faceRect != null) {
            // 面部位置分析 — 面部偏向顶部 → 头部位置调整建议
            if (faceRect.centerY < 0.3f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_head_position",
                        title = "调整头部位置",
                        description = "面部位置偏上，建议将头部稍微下移，使面部位于画面中央偏上位置",
                        instructions = "保持肩膀放松，将下巴微收，头部自然下沉 3-5 厘米",
                        tips = listOf(
                            "想象头顶有一根线轻轻向上拉",
                            "调整手机或相机角度向下倾斜 5-10 度",
                            "保持颈部放松，避免僵硬"
                        ),
                        assetPath = "poses/adjustments/head_position.png",
                        priority = 0.85f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }

            // 面部位置分析 — 面部偏向底部 → 画面构图调整
            if (faceRect.centerY > 0.7f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_frame_lower",
                        title = "调整画面构图",
                        description = "面部位置偏下，建议将相机角度微调向上，使面部位于画面黄金分割位置",
                        instructions = "稍微抬高手机或相机，让面部位于画面上三分之一处",
                        tips = listOf(
                            "开启网格线辅助构图",
                            "将眼睛放在上三分之一线位置",
                            "保持背部挺直，避免俯拍"
                        ),
                        assetPath = "poses/adjustments/frame_lower.png",
                        priority = 0.80f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }

            // 主体大小分析 — 面部过小 → 距离调整
            if (subjectDetection.subjectSizeRatio < 0.15f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_distance_closer",
                        title = "靠近拍摄主体",
                        description = "主体在画面中占比偏小，建议靠近拍摄对象或使用更长焦距",
                        instructions = "向前移动 1-2 步，或使用变焦放大主体",
                        tips = listOf(
                            "保持与主体 1-2 米的距离效果最佳",
                            "使用 2× 或 3× 变焦拉近",
                            "注意不要切到主体边缘"
                        ),
                        assetPath = "poses/adjustments/distance_closer.png",
                        priority = 0.75f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }

            // 主体大小分析 — 面部过大 → 距离调整
            if (subjectDetection.subjectSizeRatio > 0.6f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_distance_farther",
                        title = "适当后退",
                        description = "主体在画面中占比过大，建议后退一步或使用更广的焦距",
                        instructions = "向后退 1-2 步，给画面留出更多呼吸空间",
                        tips = listOf(
                            "保持主体在画面中占比约 30-50%",
                            "后退后检查背景是否有干扰元素",
                            "利用环境元素增加画面层次"
                        ),
                        assetPath = "poses/adjustments/distance_farther.png",
                        priority = 0.70f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }

            // 面部偏向左侧 → 右侧留白调整
            if (faceRect.centerX < 0.35f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_composition_left",
                        title = "调整水平构图",
                        description = "面部偏左，建议将相机向右微调，使主体位于画面中央或三分线位置",
                        instructions = "将相机向右平移，让面部位于画面中央或右侧三分线",
                        tips = listOf(
                            "开启网格线检查构图",
                            "保持主体视线方向有足够空间",
                            "注意背景的水平线对齐"
                        ),
                        assetPath = "poses/adjustments/composition_left.png",
                        priority = 0.65f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }

            // 面部偏向右侧 → 左侧留白调整
            if (faceRect.centerX > 0.65f) {
                adjustments.add(
                    PoseSuggestion(
                        id = "adjustment_composition_right",
                        title = "调整水平构图",
                        description = "面部偏右，建议将相机向左微调，使主体位于画面中央或左侧三分线",
                        instructions = "将相机向左平移，让面部位于画面中央或左侧三分线",
                        tips = listOf(
                            "开启网格线检查构图",
                            "保持主体视线方向有足够空间",
                            "注意背景的水平线对齐"
                        ),
                        assetPath = "poses/adjustments/composition_right.png",
                        priority = 0.65f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isAdjustment = true
                    )
                )
            }
        }

        return adjustments
    }

    /**
     * 基于场景类型和主体信息生成构图建议
     */
    private fun generateCompositionSuggestions(
        scene: SceneType,
        subjectDetection: SubjectDetection
    ): List<PoseSuggestion> {
        val compositions = mutableListOf<PoseSuggestion>()

        when (scene) {
            SceneType.LANDSCAPE, SceneType.NATURE, SceneType.SUNSET -> {
                compositions.add(
                    PoseSuggestion(
                        id = "composition_rule_of_thirds_landscape",
                        title = "三分法构图",
                        description = "将地平线放在上三分之一或下三分之一处，人物放在交叉点上",
                        instructions = "开启网格线，将人物置于左右三分线的交叉点，地平线远离画面中央",
                        tips = listOf(
                            "天空精彩时地平线放下三分之一",
                            "地面精彩时地平线放上三分之一",
                            "人物与风景保持比例平衡"
                        ),
                        assetPath = "poses/composition/rule_of_thirds.png",
                        priority = 0.90f,
                        category = PoseCategory.LANDSCAPE,
                        isComposition = true
                    )
                )
                compositions.add(
                    PoseSuggestion(
                        id = "composition_leading_lines",
                        title = "引导线构图",
                        description = "利用道路、河流、栅栏等自然线条引导视线到主体",
                        instructions = "寻找场景中的自然线条，将主体放在线条的汇聚点或终点",
                        tips = listOf(
                            "道路、小径是最自然的引导线",
                            "对角线构图增加动感",
                            "S 形曲线引导增加优雅感"
                        ),
                        assetPath = "poses/composition/leading_lines.png",
                        priority = 0.85f,
                        category = PoseCategory.LANDSCAPE,
                        isComposition = true
                    )
                )
            }
            SceneType.PORTRAIT, SceneType.PORTRAIT_STANDING, SceneType.PORTRAIT_SITTING -> {
                compositions.add(
                    PoseSuggestion(
                        id = "composition_negative_space",
                        title = "留白构图",
                        description = "在被摄主体视线方向留出空间，增加画面呼吸感",
                        instructions = "将主体放在画面一侧，视线方向留出 60% 以上的空白空间",
                        tips = listOf(
                            "视线方向留白比背后留白更重要",
                            "利用天空或纯色墙面作为留白",
                            "留白面积约为画面的 50-70%"
                        ),
                        assetPath = "poses/composition/negative_space.png",
                        priority = 0.88f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isComposition = true
                    )
                )
                compositions.add(
                    PoseSuggestion(
                        id = "composition_frame_within_frame",
                        title = "框式构图",
                        description = "利用门框、窗户、树枝等自然框架包围主体",
                        instructions = "寻找场景中的自然框架，将主体置于框架内，增加画面深度",
                        tips = listOf(
                            "门框、拱门是最经典的框架",
                            "树枝形成的天然框架增加层次",
                            "框架不要占据超过 30% 的画面"
                        ),
                        assetPath = "poses/composition/frame_within_frame.png",
                        priority = 0.82f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isComposition = true
                    )
                )
            }
            SceneType.FOOD -> {
                compositions.add(
                    PoseSuggestion(
                        id = "composition_diagonal_food",
                        title = "对角线构图",
                        description = "将食物沿对角线摆放，增加画面动感和引导性",
                        instructions = "将主要食材或餐具沿画面对角线排列，辅助元素填补剩余空间",
                        tips = listOf(
                            "主菜放在对角线三分之二处",
                            "餐具沿对角线方向摆放",
                            "利用桌布纹理强化对角线"
                        ),
                        assetPath = "poses/composition/diagonal_food.png",
                        priority = 0.88f,
                        category = PoseCategory.FOOD,
                        isComposition = true
                    )
                )
            }
            SceneType.PRODUCT -> {
                compositions.add(
                    PoseSuggestion(
                        id = "composition_centered_product",
                        title = "中心对称构图",
                        description = "将产品居中放置，利用对称和平衡突出产品",
                        instructions = "将产品放在画面正中央，确保完全水平，周围元素对称排列",
                        tips = listOf(
                            "使用水平仪确保完全居中",
                            "对称的背景元素增强视觉冲击",
                            "适当留白让产品成为绝对焦点"
                        ),
                        assetPath = "poses/composition/centered_product.png",
                        priority = 0.90f,
                        category = PoseCategory.PRODUCT,
                        isComposition = true
                    )
                )
            }
            else -> {
                compositions.add(
                    PoseSuggestion(
                        id = "composition_rule_of_thirds_general",
                        title = "黄金分割构图",
                        description = "将主体放在画面三分之一处，利用黄金分割比例安排元素",
                        instructions = "开启网格线，将主体放在四条三分线的任意交叉点上",
                        tips = listOf(
                            "主体放在交叉点比放在中央更有趣",
                            "视线方向多留空间",
                            "运动方向多留空间"
                        ),
                        assetPath = "poses/composition/rule_of_thirds_general.png",
                        priority = 0.85f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isComposition = true
                    )
                )
            }
        }

        return compositions
    }

    /**
     * 基于被摄主体检测结果生成动态建议
     * - 多人检测 → 双人/群体姿势
     * - 单人检测 → 单人肖像姿势
     * - 面部位置 → 场景相关建议
     */
    private fun generateDynamicSuggestions(
        subjectDetection: SubjectDetection,
        scene: SceneType
    ): List<PoseSuggestion> {
        val dynamicSuggestions = mutableListOf<PoseSuggestion>()

        // 多人检测 → 推荐双人或群体姿势
        if (subjectDetection.hasMultiplePeople || subjectDetection.faceCount >= 2) {
            if (subjectDetection.faceCount == 2) {
                // 两人场景 → 情侣/双人姿势
                val couplePoses = poseDatabase[PoseCategory.COUPLE]
                    ?.values?.flatten()
                    ?.take(3)
                    ?: emptyList()

                couplePoses.forEach { template ->
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_${template.id}",
                            title = "双人推荐: ${template.name}",
                            description = "检测到两人同框，推荐双人互动姿势",
                            instructions = generatePoseInstructions(template),
                            tips = generatePoseTips(template),
                            assetPath = guideAssets[template.id] ?: "poses/couple/default.png",
                            priority = 0.92f,
                            category = PoseCategory.COUPLE,
                            isDynamic = true
                        )
                    )
                }
            } else {
                // 三人及以上 → 群体姿势
                dynamicSuggestions.add(
                    PoseSuggestion(
                        id = "dynamic_group_pose",
                        title = "群体合影建议",
                        description = "检测到 ${subjectDetection.faceCount} 人，建议使用群体合影布局",
                        instructions = "采用三角形或弧形排列，确保每个人面部可见，前后排错开站位",
                        tips = listOf(
                            "前排蹲下或坐下，后排站立",
                            "采用三角形排列，中心人物略高",
                            "确保光线均匀照射每个人",
                            "使用小光圈确保所有人清晰"
                        ),
                        assetPath = "poses/dynamic/group_pose.png",
                        priority = 0.90f,
                        category = PoseCategory.PORTRAIT_STANDING,
                        isDynamic = true
                    )
                )
            }
        } else {
            // 单人场景 → 根据场景推荐单人姿势
            when (scene) {
                SceneType.PORTRAIT, SceneType.PORTRAIT_STANDING -> {
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_single_portrait",
                            title = "单人肖像建议",
                            description = "检测到单人主体，推荐经典单人肖像姿势",
                            instructions = "选择自然站立或随性倚靠姿势，保持与镜头眼神交流",
                            tips = listOf(
                                "找到最适合你的角度",
                                "双手不要闲着，拿道具或自然摆放",
                                "保持自然的微笑或表情"
                            ),
                            assetPath = "poses/dynamic/single_portrait.png",
                            priority = 0.88f,
                            category = PoseCategory.PORTRAIT_STANDING,
                            isDynamic = true
                        )
                    )
                }
                SceneType.LANDSCAPE, SceneType.NATURE, SceneType.TRAVEL -> {
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_travel_portrait",
                            title = "旅拍人像建议",
                            description = "单人旅行场景，推荐将人物融入风景的拍摄方式",
                            instructions = "人物占画面 15-30%，站于风景趣味点，营造人在画中游的效果",
                            tips = listOf(
                                "穿着与风景对比或协调的颜色",
                                "人物放在三分线交叉点",
                                "利用风、光线增加动态感"
                            ),
                            assetPath = "poses/dynamic/travel_portrait.png",
                            priority = 0.86f,
                            category = PoseCategory.LANDSCAPE,
                            isDynamic = true
                        )
                    )
                }
                SceneType.SELFIE -> {
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_selfie_tips",
                            title = "自拍技巧建议",
                            description = "自拍场景，推荐最佳自拍角度和姿势",
                            instructions = "手臂伸直或使用自拍杆，手机略高于眼睛水平线，向下倾斜 10-15 度",
                            tips = listOf(
                                "手机略高于眼睛，可避免双下巴",
                                "利用自然光从正面或侧面打光",
                                "寻找简洁的背景",
                                "尝试不同的面部角度找到最佳面"
                            ),
                            assetPath = "poses/dynamic/selfie_tips.png",
                            priority = 0.90f,
                            category = PoseCategory.PORTRAIT_STANDING,
                            isDynamic = true
                        )
                    )
                }
                SceneType.FOOD -> {
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_food_styling",
                            title = "美食造型建议",
                            description = "美食场景，推荐最佳拍摄角度和光线",
                            instructions = "优先选择 45 度角或俯拍，利用侧光突出食物质感",
                            tips = listOf(
                                "自然光是最好的美食光线",
                                "加入人手动作增加生活感",
                                "注意色彩搭配和摆盘",
                                "趁食物最新鲜时拍摄"
                            ),
                            assetPath = "poses/dynamic/food_styling.png",
                            priority = 0.89f,
                            category = PoseCategory.FOOD,
                            isDynamic = true
                        )
                    )
                }
                else -> {
                    dynamicSuggestions.add(
                        PoseSuggestion(
                            id = "dynamic_general_tips",
                            title = "通用拍摄建议",
                            description = "基于当前场景的通用拍摄建议",
                            instructions = "保持相机稳定，注意光线方向，选择合适的构图方式",
                            tips = listOf(
                                "保持双手稳定或使用三脚架",
                                "顺光拍摄色彩最鲜艳",
                                "侧光拍摄立体感最强"
                            ),
                            assetPath = "poses/dynamic/general_tips.png",
                            priority = 0.75f,
                            category = PoseCategory.PORTRAIT_STANDING,
                            isDynamic = true
                        )
                    )
                }
            }
        }

        return dynamicSuggestions
    }

    /**
     * 按优先级排序并限制数量
     */
    private fun prioritizeAndLimit(
        suggestions: List<PoseSuggestion>,
        maxCount: Int
    ): List<PoseSuggestion> {
        return suggestions
            .sortedByDescending { it.priority }
            .take(maxCount.coerceAtLeast(1))
    }

    /**
     * 从姿势模板创建姿势建议
     */
    private fun createPoseSuggestion(
        template: PoseTemplate,
        scene: SceneType,
        priority: Float
    ): PoseSuggestion {
        return PoseSuggestion(
            id = template.id,
            title = template.name,
            description = "${template.category} · ${template.difficulty} · ${template.name}姿势",
            instructions = generatePoseInstructions(template),
            tips = generatePoseTips(template),
            assetPath = guideAssets[template.id] ?: "poses/default.png",
            priority = priority,
            category = template.category,
            difficulty = template.difficulty
        )
    }

    /**
     * 生成姿势指导文字
     */
    private fun generatePoseInstructions(template: PoseTemplate): String {
        val kp = template.keypoints
        return buildString {
            appendLine("【${template.name}】姿势指导")
            appendLine()
            appendLine("■ 肩部: ${kp.shoulders}")
            appendLine("■ 头部: ${kp.head}")
            appendLine("■ 手臂: ${kp.arms}")
            appendLine("■ 腿部: ${kp.legs}")
            appendLine("■ 手部: ${kp.hands}")
            appendLine("■ 背部: ${kp.back}")
            appendLine("■ 眼神: ${kp.eyeContact}")
            appendLine("■ 身体角度: ${kp.bodyAngle}")
            appendLine("■ 髋部: ${kp.hips}")
            appendLine("■ 脚部: ${kp.feet}")
            appendLine("■ 下巴: ${kp.chin}")
            appendLine("■ 视线: ${kp.gaze}")
        }.trim()
    }

    /**
     * 获取姿势提示列表
     */
    private fun generatePoseTips(template: PoseTemplate): List<String> {
        return template.tips
    }

    /**
     * 计算姿势优先级分数
     */
    private fun calculatePosePriority(
        template: PoseTemplate,
        scene: SceneType,
        confidence: Float
    ): Float {
        var priority = 0.5f

        // 难度加成 — 高置信度优先推荐高级姿势
        when (template.difficulty) {
            PoseDifficulty.BEGINNER -> priority += 0.05f
            PoseDifficulty.INTERMEDIATE -> priority += 0.10f
            PoseDifficulty.ADVANCED -> priority += if (confidence > 0.7f) 0.15f else -0.10f
        }

        // 场景匹配度加成
        val sceneCategories = recommendationRules[scene] ?: emptyList()
        val categoryIndex = sceneCategories.indexOf(template.category)
        if (categoryIndex >= 0) {
            priority += (sceneCategories.size - categoryIndex) * 0.08f
        }

        // 置信度加权
        priority *= (0.5f + confidence * 0.5f)

        // 姿势名称匹配加成
        val sceneName = scene.name.lowercase()
        val templateName = template.name.lowercase()
        when {
            sceneName.contains("standing") && templateName.contains("站立") -> priority += 0.10f
            sceneName.contains("sitting") && templateName.contains("坐") -> priority += 0.10f
            sceneName.contains("couple") && template.category == PoseCategory.COUPLE -> priority += 0.12f
            sceneName.contains("wedding") && template.category == PoseCategory.WEDDING -> priority += 0.12f
            sceneName.contains("food") && template.category == PoseCategory.FOOD -> priority += 0.12f
            sceneName.contains("product") && template.category == PoseCategory.PRODUCT -> priority += 0.12f
            sceneName.contains("landscape") && template.category == PoseCategory.LANDSCAPE -> priority += 0.12f
            sceneName.contains("children") && template.category == PoseCategory.CHILDREN -> priority += 0.12f
        }

        return priority.coerceIn(0.0f, 1.0f)
    }

    /**
     * 获取兜底推荐 — 当没有匹配场景时使用
     */
    internal fun getFallbackRecommendations(): List<PoseSuggestion> {
        val fallbackCategory = PoseCategory.PORTRAIT_STANDING
        val fallbackPoses = poseDatabase[fallbackCategory]
            ?.values?.flatten()
            ?.filter { it.difficulty == PoseDifficulty.BEGINNER }
            ?.take(3)
            ?: emptyList()

        return fallbackPoses.map { template ->
            createPoseSuggestion(template, SceneType.UNKNOWN, 0.5f)
        }
    }

    /**
     * 获取置信度级别
     */
    private fun getConfidenceLevel(confidence: Float): String {
        return when {
            confidence >= 0.8f -> "high"
            confidence >= 0.5f -> "medium"
            else -> "low"
        }
    }

    /**
     * 获取置信度规则配置
     */
    @Suppress("UNCHECKED_CAST")
    private fun getConfidenceRules(level: String): Map<String, Any> {
        val confidenceRules = analysisRules["confidence"] ?: emptyMap()
        return (confidenceRules[level] as? Map<String, Any>) ?: mapOf(
            "threshold" to 0.0f,
            "maxSuggestions" to 2,
            "includeAdvanced" to false
        )
    }

    // =========================================================================
    // 伴生对象
    // =========================================================================

    companion object {
        /**
         * 创建默认实例
         */
        fun create(): PoseRecommendationEngine = PoseRecommendationEngine()

        /**
         * 获取所有可用的姿势类别
         */
        val allCategories: List<PoseCategory> = PoseCategory.entries.toList()

        /**
         * 获取所有可用的场景类型
         */
        val allSceneTypes: List<SceneType> = SceneType.entries.toList()

        /**
         * 获取姿势总数
         */
        const val TOTAL_POSES = 35

        /**
         * 获取类别总数
         */
        const val TOTAL_CATEGORIES = 8
    }
}