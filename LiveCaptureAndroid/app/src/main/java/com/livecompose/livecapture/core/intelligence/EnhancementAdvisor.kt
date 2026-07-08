package com.livecompose.livecapture.core.intelligence

class EnhancementAdvisor {

    // =========================================================================
    // 主方法：基于质量、场景、光线生成增强建议
    // =========================================================================

    fun generateSuggestions(
        quality: QualityAssessment,
        scene: SceneType,
        light: LightAnalysis
    ): List<EnhancementSuggestion> {
        val suggestions = mutableListOf<EnhancementSuggestion>()

        addQualityBasedSuggestions(suggestions, quality)
        addSceneSpecificSuggestions(suggestions, scene)
        addLightBasedSuggestions(suggestions, light)

        return suggestions
    }

    // =========================================================================
    // 重载方法：仅基于质量生成建议
    // =========================================================================

    fun generateSuggestions(quality: QualityAssessment): List<EnhancementSuggestion> {
        val suggestions = mutableListOf<EnhancementSuggestion>()
        addQualityBasedSuggestions(suggestions, quality)
        return suggestions
    }

    // =========================================================================
    // 质量维度建议
    // =========================================================================

    private fun addQualityBasedSuggestions(
        suggestions: MutableList<EnhancementSuggestion>,
        quality: QualityAssessment
    ) {
        // 锐度不足
        if (quality.sharpnessScore < 50f) {
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.SHARPNESS,
                    title = "锐度增强",
                    description = "当前画面锐度不足（得分: ${quality.sharpnessScore.toInt()}），细节表现力较弱，建议进行锐化处理以提升画面清晰度。",
                    parameters = mapOf(
                        "sharpen" to (20f + (50f - quality.sharpnessScore) * 0.4f).coerceIn(20f, 30f)
                    ),
                    priority = if (quality.sharpnessScore < 30f) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        // 噪点过高
        if (quality.noiseLevel > 30f) {
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.NOISE_REDUCTION,
                    title = "降噪处理",
                    description = "检测到较高噪点水平（${quality.noiseLevel.toInt()}%），建议进行降噪处理以提升画面纯净度。",
                    parameters = mapOf(
                        "noise_reduction" to (15f + (quality.noiseLevel - 30f) * 0.5f).coerceIn(15f, 25f)
                    ),
                    priority = if (quality.noiseLevel > 50f) Priority.HIGH else Priority.MEDIUM
                )
            )
        }

        // 曝光问题
        if (quality.exposureScore < 60f) {
            val exposureParams = if (quality.exposureScore < 30f) {
                // 严重欠曝
                mapOf("brightness" to 25f, "contrast" to 15f, "shadows" to 20f)
            } else if (quality.exposureScore < 45f) {
                // 中度欠曝
                mapOf("brightness" to 18f, "contrast" to 10f, "shadows" to 12f)
            } else {
                // 轻微欠曝
                mapOf("brightness" to 10f, "contrast" to 5f)
            }
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.EXPOSURE,
                    title = "曝光调整",
                    description = "曝光不足（得分: ${quality.exposureScore.toInt()}），建议提升亮度和对比度以改善画面曝光。",
                    parameters = exposureParams,
                    priority = if (quality.exposureScore < 40f) Priority.HIGH else Priority.MEDIUM
                )
            )
        } else if (quality.exposureScore > 90f) {
            // 过曝
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.EXPOSURE,
                    title = "曝光修正",
                    description = "画面略微过曝，建议降低高光区域并适当压暗画面。",
                    parameters = mapOf("highlights" to -15f, "brightness" to -8f),
                    priority = Priority.LOW
                )
            )
        }

        // 色彩和谐度
        if (quality.colorHarmonyScore < 40f) {
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.COLOR_HARMONY,
                    title = "色彩和谐度调整",
                    description = "色彩和谐度偏低（得分: ${quality.colorHarmonyScore.toInt()}），建议调整饱和度和色温以改善色彩表现。",
                    parameters = mapOf(
                        "saturation" to 12f,
                        "warmth" to 5f,
                        "vibrance" to 8f
                    ),
                    priority = if (quality.colorHarmonyScore < 25f) Priority.HIGH else Priority.MEDIUM
                )
            )
        }
    }

    // =========================================================================
    // 场景特定建议
    // =========================================================================

    private fun addSceneSpecificSuggestions(
        suggestions: MutableList<EnhancementSuggestion>,
        scene: SceneType
    ) {
        when (scene) {
            // ---- 人像组 ----
            SceneType.PORTRAIT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "人像增强",
                        description = "人像场景检测：建议柔化肤色、增强眼部细节、添加轻微暗角以突出主体。",
                        parameters = mapOf("skin_soften" to 15f, "eye_clarity" to 10f, "vignette" to 8f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.PORTRAIT_STANDING -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "站姿人像增强",
                        description = "站姿人像场景：建议优化全身比例、增强背景虚化、提升主体锐度。",
                        parameters = mapOf("skin_soften" to 12f, "background_blur" to 10f, "subject_sharpness" to 15f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.PORTRAIT_SITTING -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "坐姿人像增强",
                        description = "坐姿人像场景：建议柔化面部光线、增强上半身细节、暖化肤色。",
                        parameters = mapOf("skin_soften" to 18f, "face_light" to 10f, "warmth" to 8f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.SELFIE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "自拍增强",
                        description = "自拍场景：建议面部光线优化、瘦脸微调、皮肤柔化。",
                        parameters = mapOf("face_brightness" to 12f, "skin_soften" to 20f, "eye_enhance" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.BEAUTY -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "美颜增强",
                        description = "美颜场景：建议肤色均匀化、细节柔化、光泽感提升。",
                        parameters = mapOf("skin_soften" to 25f, "glow" to 10f, "smooth" to 15f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.FASHION -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "时尚人像增强",
                        description = "时尚场景：建议高对比度、饱和度提升、服装细节锐化。",
                        parameters = mapOf("contrast" to 15f, "saturation" to 10f, "sharpness" to 12f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 风景组 ----
            SceneType.LANDSCAPE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "风景增强",
                        description = "风景场景：建议提升清晰度、增强天空层次、增加画面纵深感。",
                        parameters = mapOf("clarity" to 20f, "dehaze" to 15f, "saturation" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.NATURE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "自然风景增强",
                        description = "自然风景场景：建议增强绿色饱和度、提升阴影细节、增加通透感。",
                        parameters = mapOf("greens_saturation" to 15f, "shadows" to 12f, "clarity" to 18f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.SUNSET -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "日落/日出优化",
                        description = "日落/日出场景：建议增强暖色调、提升天空渐变层次、优化云层细节。",
                        parameters = mapOf("warmth" to 20f, "highlights" to -10f, "dehaze" to 12f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.OUTDOOR -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "户外增强",
                        description = "户外场景：建议提升整体亮度、增强色彩饱和度、优化天空曝光。",
                        parameters = mapOf("brightness" to 8f, "saturation" to 12f, "sky_protection" to 10f),
                        priority = Priority.MEDIUM
                    )
                )
            }
            SceneType.TRAVEL -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "旅行风光增强",
                        description = "旅行场景：建议提升色彩生动度、增强画面层次、添加旅行氛围感。",
                        parameters = mapOf("vibrance" to 15f, "clarity" to 12f, "saturation" to 8f),
                        priority = Priority.MEDIUM
                    )
                )
            }

            // ---- 夜景组 ----
            SceneType.NIGHT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.NIGHT_OPTIMIZATION,
                        title = "夜景优化",
                        description = "夜景场景：建议降噪处理、提升暗部细节、控制高光溢出、增强夜景氛围。",
                        parameters = mapOf("noise_reduction" to 25f, "shadows" to 20f, "highlights" to -15f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 食物组 ----
            SceneType.FOOD -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.FOOD_ENHANCEMENT,
                        title = "食物增强",
                        description = "美食场景：建议提升饱和度、增强暖色调、增加微距锐度、优化食物光泽感。",
                        parameters = mapOf("saturation" to 18f, "warmth" to 15f, "sharpness" to 10f, "highlights" to 8f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.STILL_LIFE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.FOOD_ENHANCEMENT,
                        title = "静物/纹理增强",
                        description = "静物/纹理场景：建议提升清晰度、增强纹理细节、优化光影对比。",
                        parameters = mapOf("clarity" to 22f, "texture" to 18f, "contrast" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 婚礼组 ----
            SceneType.WEDDING -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "婚礼优化",
                        description = "婚礼场景：建议暖色调、柔化高光、增强白色细节、提升浪漫氛围。",
                        parameters = mapOf("warmth" to 12f, "highlights" to -10f, "soft_glow" to 15f, "contrast" to -5f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.EVENT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "舞台/活动增强",
                        description = "舞台/活动场景：建议提升暗部细节、控制舞台灯光溢出、增强色彩表现力。",
                        parameters = mapOf("shadows" to 18f, "highlights" to -20f, "saturation" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 儿童组 ----
            SceneType.CHILDREN -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "儿童增强",
                        description = "儿童场景：建议柔化皮肤、提升画面明亮度、增强眼神光、活泼色彩。",
                        parameters = mapOf("skin_soften" to 10f, "brightness" to 12f, "eye_sparkle" to 15f, "saturation" to 8f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 产品组 ----
            SceneType.PRODUCT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.SHARPNESS,
                        title = "产品增强",
                        description = "产品场景：建议白底提亮、增强细节锐度、提升对比度、优化产品质感。",
                        parameters = mapOf("brightness" to 15f, "sharpness" to 20f, "contrast" to 12f, "whites" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 宠物组 ----
            SceneType.PET -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "宠物增强",
                        description = "宠物场景：建议增强毛发细节、提升眼神锐度、优化背景虚化。",
                        parameters = mapOf("fur_detail" to 20f, "eye_sharpness" to 15f, "background_blur" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 建筑组 ----
            SceneType.ARCHITECTURE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "建筑增强",
                        description = "建筑场景：建议提升几何线条锐度、增强天空对比、优化透视校正。",
                        parameters = mapOf("sharpness" to 18f, "contrast" to 15f, "clarity" to 20f, "dehaze" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.URBAN -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "城市风光增强",
                        description = "城市场景：建议提升建筑细节、增强夜景灯光、优化天空层次。",
                        parameters = mapOf("clarity" to 15f, "dehaze" to 12f, "sharpness" to 10f),
                        priority = Priority.MEDIUM
                    )
                )
            }

            // ---- 文档/纪实组 ----
            SceneType.DOCUMENTARY -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.SHARPNESS,
                        title = "文档/纪实增强",
                        description = "文档/纪实场景：建议提升文字清晰度、增强对比度、优化黑白层次。",
                        parameters = mapOf("sharpness" to 25f, "contrast" to 20f, "clarity" to 15f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 微距组 ----
            SceneType.MACRO -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.SHARPNESS,
                        title = "微距/花卉增强",
                        description = "微距场景：建议极限锐化、增强色彩饱和度、提升纹理细节、优化背景虚化。",
                        parameters = mapOf("sharpness" to 25f, "saturation" to 15f, "texture" to 20f, "background_blur" to 12f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 街拍组 ----
            SceneType.STREET -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.LANDSCAPE_ENHANCEMENT,
                        title = "街拍增强",
                        description = "街拍场景：建议增加画面质感、提升对比度、增强故事感氛围。",
                        parameters = mapOf("contrast" to 12f, "clarity" to 15f, "grain" to 5f, "vignette" to 10f),
                        priority = Priority.MEDIUM
                    )
                )
            }

            // ---- 室内组 ----
            SceneType.INDOOR -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.EXPOSURE,
                        title = "室内增强",
                        description = "室内场景：建议提升亮度、优化白平衡、减少阴影噪点。",
                        parameters = mapOf("brightness" to 15f, "shadows" to 12f, "noise_reduction" to 10f),
                        priority = Priority.MEDIUM
                    )
                )
            }

            // ---- 合影组 ----
            SceneType.GROUP -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "合影增强",
                        description = "合影场景：建议多人面部优化、均衡曝光、提升整体清晰度。",
                        parameters = mapOf("face_brightness" to 10f, "sharpness" to 12f, "exposure_balance" to 15f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.COUPLE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.PORTRAIT_ENHANCEMENT,
                        title = "情侣增强",
                        description = "情侣场景：建议柔光处理、暖色调、浪漫氛围渲染。",
                        parameters = mapOf("soft_glow" to 12f, "warmth" to 10f, "skin_soften" to 8f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 剪影组 ----
            SceneType.SILHOUETTE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.EXPOSURE,
                        title = "剪影增强",
                        description = "剪影场景：建议加深暗部、增强轮廓对比、优化天空色彩渐变。",
                        parameters = mapOf("shadows" to -15f, "contrast" to 20f, "highlights" to 10f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 运动/动作组 ----
            SceneType.ACTION, SceneType.SPORTS -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.SHARPNESS,
                        title = "运动/动作增强",
                        description = "运动场景：建议提升快门清晰度、增强动态对比、优化运动模糊。",
                        parameters = mapOf("sharpness" to 20f, "contrast" to 15f, "clarity" to 18f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 逆光组 ----
            SceneType.BACKLIT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.EXPOSURE,
                        title = "逆光优化",
                        description = "逆光场景：建议提升暗部亮度、控制高光溢出、增强主体曝光。",
                        parameters = mapOf("shadows" to 30f, "brightness" to 10f, "highlights" to -20f),
                        priority = Priority.HIGH
                    )
                )
            }

            // ---- 极简/复古/电影组 ----
            SceneType.MINIMAL -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.EXPOSURE,
                        title = "极简/雪景增强",
                        description = "极简/雪景场景：建议提亮画面、提升白色纯净度、降低对比度。",
                        parameters = mapOf("brightness" to 20f, "whites" to 15f, "contrast" to -10f),
                        priority = Priority.HIGH
                    )
                )
            }
            SceneType.VINTAGE -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "复古增强",
                        description = "复古场景：建议降低饱和度、添加暖黄调、增加颗粒感、提升暗角效果。",
                        parameters = mapOf("saturation" to -15f, "warmth" to 15f, "grain" to 12f, "vignette" to 20f),
                        priority = Priority.MEDIUM
                    )
                )
            }
            SceneType.CINEMATIC -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "电影感增强",
                        description = "电影感场景：建议宽幅裁切、青橙色调、增加暗角、降低中间调对比度。",
                        parameters = mapOf("teal_orange" to 15f, "vignette" to 18f, "contrast" to -5f, "saturation" to -5f),
                        priority = Priority.MEDIUM
                    )
                )
            }

            // ---- 未知/默认 ----
            SceneType.UNKNOWN -> {
                // 无特定场景建议
            }
        }
    }

    // =========================================================================
    // 光线条件建议
    // =========================================================================

    private fun addLightBasedSuggestions(
        suggestions: MutableList<EnhancementSuggestion>,
        light: LightAnalysis
    ) {
        if (light.isBacklit) {
            suggestions.add(
                EnhancementSuggestion(
                    type = EnhancementType.EXPOSURE,
                    title = "逆光补偿",
                    description = "检测到逆光环境：建议提升暗部阴影、适当增加曝光补偿以平衡主体与背景亮度。",
                    parameters = mapOf("shadows" to 30f, "exposure_compensation" to 15f, "highlights" to -20f),
                    priority = Priority.HIGH
                )
            )
        }

        when (light.lightType) {
            LightType.NATURAL -> {
                // 自然光，无需额外调整
                if (light.brightness < 0.3f && !light.isBacklit) {
                    suggestions.add(
                        EnhancementSuggestion(
                            type = EnhancementType.EXPOSURE,
                            title = "低光增强",
                            description = "自然光环境下亮度偏低：建议提升曝光并适度降噪。",
                            parameters = mapOf("brightness" to 15f, "noise_reduction" to 10f, "shadows" to 12f),
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }
            LightType.WARM -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "暖光色温调整",
                        description = "检测到暖色光源：建议适当降低色温以平衡画面色调，避免过度偏黄。",
                        parameters = mapOf("warmth" to -10f, "temperature" to -500f),
                        priority = Priority.LOW
                    )
                )
            }
            LightType.COOL -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "冷光色温调整",
                        description = "检测到冷色光源：建议适当增加色温以平衡画面色调，避免过度偏蓝。",
                        parameters = mapOf("warmth" to 12f, "temperature" to 500f),
                        priority = Priority.LOW
                    )
                )
            }
            LightType.FLUORESCENT -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "荧光灯色调校正",
                        description = "检测到荧光灯光源：建议进行色调校正，消除绿色偏色，恢复自然肤色。",
                        parameters = mapOf("tint" to 15f, "green_correction" to 20f),
                        priority = Priority.MEDIUM
                    )
                )
                if (light.brightness < 0.4f) {
                    suggestions.add(
                        EnhancementSuggestion(
                            type = EnhancementType.EXPOSURE,
                            title = "荧光灯环境亮度补偿",
                            description = "荧光灯环境下亮度不足：建议提升曝光以保证画面清晰度。",
                            parameters = mapOf("brightness" to 12f, "noise_reduction" to 8f),
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }
            LightType.MIXED -> {
                suggestions.add(
                    EnhancementSuggestion(
                        type = EnhancementType.COLOR_HARMONY,
                        title = "混合光源综合校正",
                        description = "检测到混合光源环境：建议进行综合白平衡校正，平衡多种色温影响。",
                        parameters = mapOf("white_balance" to 10f, "tint" to 5f, "temperature" to 200f),
                        priority = Priority.MEDIUM
                    )
                )
                if (light.contrast > 0.7f) {
                    suggestions.add(
                        EnhancementSuggestion(
                            type = EnhancementType.EXPOSURE,
                            title = "混合光高对比度调整",
                            description = "混合光源下对比度偏高：建议降低对比度以均衡画面。",
                            parameters = mapOf("contrast" to -10f, "shadows" to 8f),
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // 获取场景预设参数 — 每个场景类型有独特的10参数预设
    // =========================================================================

    fun getPresetParams(scene: SceneType): ScenePresetParams {
        return when (scene) {
            // ---- 人像系列 ----
            SceneType.PORTRAIT -> ScenePresetParams(
                exposure = 0.15f, contrast = -0.05f, saturation = 0.05f,
                highlights = -0.10f, shadows = 0.10f, clarity = 0.05f,
                warmth = 0.08f, sharpness = 0.12f, noiseReduction = 0.10f, vignette = 0.15f
            )
            SceneType.PORTRAIT_STANDING -> ScenePresetParams(
                exposure = 0.10f, contrast = 0.0f, saturation = 0.08f,
                highlights = -0.08f, shadows = 0.12f, clarity = 0.08f,
                warmth = 0.06f, sharpness = 0.15f, noiseReduction = 0.08f, vignette = 0.12f
            )
            SceneType.PORTRAIT_SITTING -> ScenePresetParams(
                exposure = 0.12f, contrast = -0.03f, saturation = 0.06f,
                highlights = -0.12f, shadows = 0.15f, clarity = 0.04f,
                warmth = 0.10f, sharpness = 0.10f, noiseReduction = 0.12f, vignette = 0.18f
            )
            SceneType.SELFIE -> ScenePresetParams(
                exposure = 0.20f, contrast = -0.08f, saturation = 0.04f,
                highlights = -0.15f, shadows = 0.08f, clarity = 0.02f,
                warmth = 0.05f, sharpness = 0.08f, noiseReduction = 0.15f, vignette = 0.10f
            )
            SceneType.BEAUTY -> ScenePresetParams(
                exposure = 0.18f, contrast = -0.10f, saturation = 0.03f,
                highlights = -0.18f, shadows = 0.05f, clarity = 0.0f,
                warmth = 0.07f, sharpness = 0.05f, noiseReduction = 0.20f, vignette = 0.08f
            )
            SceneType.FASHION -> ScenePresetParams(
                exposure = 0.10f, contrast = 0.15f, saturation = 0.20f,
                highlights = -0.05f, shadows = 0.05f, clarity = 0.15f,
                warmth = 0.0f, sharpness = 0.20f, noiseReduction = 0.05f, vignette = 0.10f
            )

            // ---- 风景系列 ----
            SceneType.LANDSCAPE -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.10f, saturation = 0.15f,
                highlights = -0.10f, shadows = 0.10f, clarity = 0.20f,
                warmth = 0.05f, sharpness = 0.18f, noiseReduction = 0.05f, vignette = 0.05f
            )
            SceneType.NATURE -> ScenePresetParams(
                exposure = 0.08f, contrast = 0.08f, saturation = 0.18f,
                highlights = -0.08f, shadows = 0.15f, clarity = 0.22f,
                warmth = 0.03f, sharpness = 0.20f, noiseReduction = 0.04f, vignette = 0.02f
            )
            SceneType.SUNSET -> ScenePresetParams(
                exposure = -0.05f, contrast = 0.05f, saturation = 0.25f,
                highlights = -0.20f, shadows = 0.05f, clarity = 0.15f,
                warmth = 0.30f, sharpness = 0.12f, noiseReduction = 0.08f, vignette = 0.10f
            )
            SceneType.OUTDOOR -> ScenePresetParams(
                exposure = 0.10f, contrast = 0.05f, saturation = 0.12f,
                highlights = -0.12f, shadows = 0.08f, clarity = 0.12f,
                warmth = 0.05f, sharpness = 0.15f, noiseReduction = 0.05f, vignette = 0.0f
            )
            SceneType.TRAVEL -> ScenePresetParams(
                exposure = 0.08f, contrast = 0.08f, saturation = 0.20f,
                highlights = -0.10f, shadows = 0.10f, clarity = 0.18f,
                warmth = 0.10f, sharpness = 0.16f, noiseReduction = 0.06f, vignette = 0.08f
            )

            // ---- 夜景系列 ----
            SceneType.NIGHT -> ScenePresetParams(
                exposure = 0.10f, contrast = 0.15f, saturation = 0.05f,
                highlights = -0.25f, shadows = 0.25f, clarity = 0.10f,
                warmth = -0.05f, sharpness = 0.10f, noiseReduction = 0.28f, vignette = 0.20f
            )

            // ---- 食物系列 ----
            SceneType.FOOD -> ScenePresetParams(
                exposure = 0.15f, contrast = 0.08f, saturation = 0.25f,
                highlights = 0.05f, shadows = 0.10f, clarity = 0.15f,
                warmth = 0.25f, sharpness = 0.18f, noiseReduction = 0.10f, vignette = 0.15f
            )
            SceneType.STILL_LIFE -> ScenePresetParams(
                exposure = 0.10f, contrast = 0.12f, saturation = 0.10f,
                highlights = -0.05f, shadows = 0.08f, clarity = 0.25f,
                warmth = 0.0f, sharpness = 0.22f, noiseReduction = 0.08f, vignette = 0.12f
            )

            // ---- 婚礼/活动系列 ----
            SceneType.WEDDING -> ScenePresetParams(
                exposure = 0.12f, contrast = -0.05f, saturation = 0.08f,
                highlights = -0.15f, shadows = 0.12f, clarity = 0.05f,
                warmth = 0.20f, sharpness = 0.10f, noiseReduction = 0.12f, vignette = 0.20f
            )
            SceneType.EVENT -> ScenePresetParams(
                exposure = 0.08f, contrast = 0.10f, saturation = 0.12f,
                highlights = -0.20f, shadows = 0.18f, clarity = 0.10f,
                warmth = 0.05f, sharpness = 0.12f, noiseReduction = 0.15f, vignette = 0.10f
            )

            // ---- 儿童系列 ----
            SceneType.CHILDREN -> ScenePresetParams(
                exposure = 0.20f, contrast = -0.05f, saturation = 0.15f,
                highlights = -0.10f, shadows = 0.10f, clarity = 0.05f,
                warmth = 0.15f, sharpness = 0.08f, noiseReduction = 0.10f, vignette = 0.12f
            )

            // ---- 产品系列 ----
            SceneType.PRODUCT -> ScenePresetParams(
                exposure = 0.20f, contrast = 0.15f, saturation = 0.05f,
                highlights = -0.05f, shadows = 0.05f, clarity = 0.28f,
                warmth = 0.0f, sharpness = 0.25f, noiseReduction = 0.15f, vignette = 0.05f
            )

            // ---- 宠物系列 ----
            SceneType.PET -> ScenePresetParams(
                exposure = 0.12f, contrast = 0.05f, saturation = 0.12f,
                highlights = -0.08f, shadows = 0.10f, clarity = 0.18f,
                warmth = 0.10f, sharpness = 0.22f, noiseReduction = 0.08f, vignette = 0.10f
            )

            // ---- 建筑系列 ----
            SceneType.ARCHITECTURE -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.20f, saturation = 0.05f,
                highlights = -0.15f, shadows = 0.05f, clarity = 0.30f,
                warmth = 0.0f, sharpness = 0.25f, noiseReduction = 0.05f, vignette = 0.0f
            )
            SceneType.URBAN -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.18f, saturation = 0.08f,
                highlights = -0.12f, shadows = 0.08f, clarity = 0.25f,
                warmth = -0.02f, sharpness = 0.22f, noiseReduction = 0.08f, vignette = 0.05f
            )

            // ---- 纪实/文档系列 ----
            SceneType.DOCUMENTARY -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.25f, saturation = -0.10f,
                highlights = -0.10f, shadows = 0.10f, clarity = 0.20f,
                warmth = 0.0f, sharpness = 0.28f, noiseReduction = 0.10f, vignette = 0.08f
            )

            // ---- 微距系列 ----
            SceneType.MACRO -> ScenePresetParams(
                exposure = 0.08f, contrast = 0.10f, saturation = 0.20f,
                highlights = -0.05f, shadows = 0.08f, clarity = 0.25f,
                warmth = 0.08f, sharpness = 0.30f, noiseReduction = 0.10f, vignette = 0.15f
            )

            // ---- 街拍系列 ----
            SceneType.STREET -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.18f, saturation = 0.05f,
                highlights = -0.12f, shadows = 0.12f, clarity = 0.20f,
                warmth = 0.02f, sharpness = 0.18f, noiseReduction = 0.08f, vignette = 0.15f
            )

            // ---- 室内系列 ----
            SceneType.INDOOR -> ScenePresetParams(
                exposure = 0.15f, contrast = 0.05f, saturation = 0.05f,
                highlights = -0.08f, shadows = 0.15f, clarity = 0.08f,
                warmth = 0.08f, sharpness = 0.12f, noiseReduction = 0.15f, vignette = 0.05f
            )

            // ---- 合影/情侣系列 ----
            SceneType.GROUP -> ScenePresetParams(
                exposure = 0.15f, contrast = 0.0f, saturation = 0.08f,
                highlights = -0.10f, shadows = 0.12f, clarity = 0.10f,
                warmth = 0.08f, sharpness = 0.15f, noiseReduction = 0.10f, vignette = 0.08f
            )
            SceneType.COUPLE -> ScenePresetParams(
                exposure = 0.12f, contrast = -0.05f, saturation = 0.10f,
                highlights = -0.12f, shadows = 0.10f, clarity = 0.05f,
                warmth = 0.15f, sharpness = 0.10f, noiseReduction = 0.10f, vignette = 0.18f
            )

            // ---- 剪影系列 ----
            SceneType.SILHOUETTE -> ScenePresetParams(
                exposure = -0.20f, contrast = 0.30f, saturation = 0.15f,
                highlights = 0.10f, shadows = -0.25f, clarity = 0.05f,
                warmth = 0.10f, sharpness = 0.08f, noiseReduction = 0.05f, vignette = 0.25f
            )

            // ---- 运动/动作系列 ----
            SceneType.ACTION -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.20f, saturation = 0.10f,
                highlights = -0.10f, shadows = 0.05f, clarity = 0.22f,
                warmth = 0.0f, sharpness = 0.25f, noiseReduction = 0.05f, vignette = 0.05f
            )
            SceneType.SPORTS -> ScenePresetParams(
                exposure = 0.05f, contrast = 0.22f, saturation = 0.12f,
                highlights = -0.08f, shadows = 0.05f, clarity = 0.25f,
                warmth = 0.0f, sharpness = 0.28f, noiseReduction = 0.04f, vignette = 0.02f
            )

            // ---- 逆光系列 ----
            SceneType.BACKLIT -> ScenePresetParams(
                exposure = 0.15f, contrast = 0.05f, saturation = 0.05f,
                highlights = -0.30f, shadows = 0.35f, clarity = 0.05f,
                warmth = 0.05f, sharpness = 0.10f, noiseReduction = 0.12f, vignette = 0.08f
            )

            // ---- 极简/复古/电影系列 ----
            SceneType.MINIMAL -> ScenePresetParams(
                exposure = 0.25f, contrast = -0.10f, saturation = -0.15f,
                highlights = 0.10f, shadows = 0.05f, clarity = 0.05f,
                warmth = 0.0f, sharpness = 0.08f, noiseReduction = 0.10f, vignette = 0.0f
            )
            SceneType.VINTAGE -> ScenePresetParams(
                exposure = 0.05f, contrast = -0.05f, saturation = -0.20f,
                highlights = -0.15f, shadows = 0.10f, clarity = -0.05f,
                warmth = 0.25f, sharpness = 0.05f, noiseReduction = 0.05f, vignette = 0.30f
            )
            SceneType.CINEMATIC -> ScenePresetParams(
                exposure = -0.05f, contrast = -0.05f, saturation = -0.10f,
                highlights = -0.20f, shadows = 0.15f, clarity = 0.08f,
                warmth = -0.05f, sharpness = 0.10f, noiseReduction = 0.10f, vignette = 0.25f
            )

            // ---- 未知/默认 ----
            SceneType.UNKNOWN -> ScenePresetParams.DEFAULT
        }
    }

    // =========================================================================
    // 获取最优设置 — 结合场景和光线条件
    // =========================================================================

    fun getOptimalSettings(scene: SceneType, light: LightAnalysis): ScenePresetParams {
        val base = getPresetParams(scene)

        var exposure = base.exposure
        var contrast = base.contrast
        var saturation = base.saturation
        var highlights = base.highlights
        var shadows = base.shadows
        var clarity = base.clarity
        var warmth = base.warmth
        var sharpness = base.sharpness
        var noiseReduction = base.noiseReduction
        var vignette = base.vignette

        // 低亮度：增加曝光、增加阴影
        if (light.brightness < 0.35f) {
            exposure += 0.15f
            shadows += 0.12f
            noiseReduction += 0.10f
        }

        // 高亮度：降低曝光、增加高光保护
        if (light.brightness > 0.75f) {
            exposure -= 0.12f
            highlights -= 0.10f
        }

        // 逆光：大幅增加阴影、略微增加曝光
        if (light.isBacklit) {
            shadows += 0.25f
            exposure += 0.08f
            highlights -= 0.15f
        }

        // 高对比度：降低对比度、增加阴影
        if (light.contrast > 0.7f) {
            contrast -= 0.12f
            shadows += 0.10f
        }

        // 低对比度：增加对比度、降低高光
        if (light.contrast < 0.3f) {
            contrast += 0.12f
            highlights -= 0.08f
        }

        // 光线类型特定调整
        when (light.lightType) {
            LightType.WARM -> {
                warmth -= 0.12f
            }
            LightType.COOL -> {
                warmth += 0.15f
            }
            LightType.FLUORESCENT -> {
                // 荧光灯：调整色调（通过warmth模拟色调偏移）
                warmth += 0.05f
                saturation += 0.05f
            }
            LightType.MIXED -> {
                // 混合光源：温和调整
                contrast += 0.03f
                saturation += 0.03f
                warmth += 0.02f
            }
            LightType.NATURAL -> {
                // 自然光保持基本不变
            }
        }

        // 钳制所有值到 [-1.0, 1.0] 范围
        return ScenePresetParams(
            exposure = exposure.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(-1f, 1f),
            saturation = saturation.coerceIn(-1f, 1f),
            highlights = highlights.coerceIn(-1f, 1f),
            shadows = shadows.coerceIn(-1f, 1f),
            clarity = clarity.coerceIn(-1f, 1f),
            warmth = warmth.coerceIn(-1f, 1f),
            sharpness = sharpness.coerceIn(-1f, 1f),
            noiseReduction = noiseReduction.coerceIn(-1f, 1f),
            vignette = vignette.coerceIn(-1f, 1f)
        )
    }

    // =========================================================================
    // 生成中文摘要报告
    // =========================================================================

    fun generateSummaryReport(
        quality: QualityAssessment,
        suggestions: List<EnhancementSuggestion>
    ): String {
        val sb = StringBuilder()

        // 标题
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("       画质增强建议报告")
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine()

        // 总体评分与等级
        val gradeEmoji = when (quality.qualityGrade) {
            QualityGrade.EXCELLENT -> "⭐"
            QualityGrade.GOOD -> "✓"
            QualityGrade.FAIR -> "△"
            QualityGrade.POOR -> "✗"
        }
        sb.appendLine("【总体评分】")
        sb.appendLine("  综合得分: ${"%.1f".format(quality.overallScore)} / 100")
        sb.appendLine("  质量等级: ${gradeEmoji} ${quality.qualityGrade.displayName}")
        sb.appendLine()

        // 各维度评分
        sb.appendLine("【各维度评分】")
        sb.appendLine(formatDimensionScore("锐度", quality.sharpnessScore, 50f))
        sb.appendLine(formatDimensionScore("噪点控制", 100f - quality.noiseLevel, 70f))
        sb.appendLine(formatDimensionScore("曝光", quality.exposureScore, 60f))
        sb.appendLine(formatDimensionScore("色彩和谐度", quality.colorHarmonyScore, 40f))
        sb.appendLine(formatDimensionScore("分辨率", quality.resolutionScore, 50f))
        sb.appendLine()

        // 图像信息
        quality.imageInfo?.let { info ->
            sb.appendLine("【图像信息】")
            sb.appendLine("  分辨率: ${info.width} × ${info.height}")
            sb.appendLine("  宽高比: ${"%.2f".format(info.aspectRatio)}")
            sb.appendLine("  格式: ${info.format}")
            sb.appendLine("  总像素: ${info.totalPixels / 1_000_000}MP")
            sb.appendLine("  分辨率等级: ${info.resolutionLevel}")
            sb.appendLine()
        }

        // 建议列表
        sb.appendLine("【增强建议】")
        if (suggestions.isEmpty()) {
            sb.appendLine("  ✓ 当前画面质量良好，无需额外增强处理。")
        } else {
            val highPriority = suggestions.filter { it.priority == Priority.HIGH }
            val mediumPriority = suggestions.filter { it.priority == Priority.MEDIUM }
            val lowPriority = suggestions.filter { it.priority == Priority.LOW }

            var index = 1

            if (highPriority.isNotEmpty()) {
                sb.appendLine("  ── 高优先级 ──")
                highPriority.forEach { suggestion ->
                    sb.appendLine(formatSuggestionEntry(index, suggestion))
                    index++
                }
                sb.appendLine()
            }

            if (mediumPriority.isNotEmpty()) {
                sb.appendLine("  ── 中优先级 ──")
                mediumPriority.forEach { suggestion ->
                    sb.appendLine(formatSuggestionEntry(index, suggestion))
                    index++
                }
                sb.appendLine()
            }

            if (lowPriority.isNotEmpty()) {
                sb.appendLine("  ── 低优先级 ──")
                lowPriority.forEach { suggestion ->
                    sb.appendLine(formatSuggestionEntry(index, suggestion))
                    index++
                }
                sb.appendLine()
            }
        }

        // 总结
        sb.appendLine("【总结】")
        val totalSuggestions = suggestions.size
        val highCount = suggestions.count { it.priority == Priority.HIGH }
        val mediumCount = suggestions.count { it.priority == Priority.MEDIUM }
        val lowCount = suggestions.count { it.priority == Priority.LOW }

        sb.append("  共生成 $totalSuggestions 条建议")
        if (totalSuggestions > 0) {
            sb.append("（高: $highCount, 中: $mediumCount, 低: $lowCount）")
        }
        sb.appendLine()

        when {
            quality.overallScore >= 85f -> {
                sb.appendLine("  画面质量优秀，建议关注细节微调以获得最佳效果。")
            }
            quality.overallScore >= 70f -> {
                sb.appendLine("  画面质量良好，建议优先处理高优先级项目以进一步提升。")
            }
            quality.overallScore >= 50f -> {
                sb.appendLine("  画面质量一般，建议按照优先级依次处理各项增强建议。")
            }
            else -> {
                sb.appendLine("  画面质量需改善，建议重点关注高优先级增强项目以显著提升画质。")
            }
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════")
        sb.appendLine("  报告生成时间: ${formatTimestamp(quality.timestamp)}")
        sb.appendLine("═══════════════════════════════════")

        return sb.toString()
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private fun formatDimensionScore(name: String, score: Float, threshold: Float): String {
        val passFail = if (score >= threshold) "✓ 通过" else "✗ 需改善"
        val bar = buildScoreBar(score)
        return "  $name: ${"%.1f".format(score)} $bar $passFail"
    }

    private fun buildScoreBar(score: Float): String {
        val filledBlocks = (score / 10f).toInt().coerceIn(0, 10)
        val emptyBlocks = 10 - filledBlocks
        return "[" + "█".repeat(filledBlocks) + "░".repeat(emptyBlocks) + "]"
    }

    private fun formatSuggestionEntry(index: Int, suggestion: EnhancementSuggestion): String {
        val sb = StringBuilder()
        val priorityTag = when (suggestion.priority) {
            Priority.HIGH -> "🔴"
            Priority.MEDIUM -> "🟡"
            Priority.LOW -> "🟢"
        }
        sb.appendLine("  $index. $priorityTag ${suggestion.title}")
        sb.appendLine("     ${suggestion.description}")
        if (suggestion.parameters.isNotEmpty()) {
            sb.append("     参数: ")
            sb.appendLine(suggestion.parameters.entries.joinToString(", ") { "${it.key}=${"%.1f".format(it.value)}" })
        }
        return sb.toString()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
        return sdf.format(java.util.Date(timestamp))
    }
}