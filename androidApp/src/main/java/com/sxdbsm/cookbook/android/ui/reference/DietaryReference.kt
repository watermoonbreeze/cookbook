package com.sxdbsm.cookbook.android.ui.reference

/**
 * @File : DietaryReference
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 膳食参考依据静态数据——App 营养阈值/分级引用的权威标准，透明列给用户
 * <p>
 * 数据经联网核准权威一手来源(膳食指南/GB/WS-T/临床指南/WHO)，逐条带口径+出处；
 * 未取得权威一手确切数值的标 pending(App 不以其参与自动判定)。守健康数据免责红线：不编造出处。
 * <p>
 * [AI生成] 让慢病家庭"看得到我们凭什么这么判"。分类见 [categories]，来源清单见 [sources]。
 **/

/** 一个指标下的一个分级/口径值。[AI生成] */
data class DietaryRefLevel(val label: String, val value: String)

/** 一条参考指标。[AI生成] */
data class DietaryRefItem(
    val indicator: String,
    val levels: List<DietaryRefLevel>,
    val caliber: String,
    val appliesTo: List<String>,
    val source: String, // 《全称》 编号 · 机构 · 年
    val pending: Boolean = false, // 无权威一手确切数值(如嘌呤三级分级)：如实标注、不参与自动判定
)

/** 一个分类分组。[AI生成] */
data class DietaryRefCategory(val title: String, val intro: String, val items: List<DietaryRefItem>)

/** 去重后的参考来源。[AI生成] */
data class DietaryRefSource(val title: String, val no: String, val org: String, val year: String)

object DietaryReference {

    const val disclaimer =
        "本页所列阈值与分级均引用自下列公开的国家标准、卫生行业标准与权威指南，用于帮助你理解 App 中营养提示的判断依据。" +
            "仅供日常饮食参考，不构成医疗或营养专业建议，慢病管理请遵医嘱。标「待核」者为尚未取得权威一手确切数值的项，App 不以其参与任何自动判定。"

    private const val SRC_GUIDE = "《中国居民膳食指南(2022)》 · 中国营养学会 · 2022"
    private const val SRC_DRIS = "《中国居民膳食营养素参考摄入量》 WS/T 578 系列(2023版沿用) · 中国营养学会 · 2018/2023"
    private const val SRC_GB = "GB 28050-2011《预包装食品营养标签通则》 · 原卫生部 · 2011"
    private const val SRC_HTN = "《中国高血压防治指南(2018年修订版)》 · 高血压联盟(中国)等 · 2018"
    private const val SRC_DM = "《中国2型糖尿病防治指南(2020年版)》 · 中华医学会糖尿病学分会 · 2020"
    private const val SRC_GI = "WS/T 652-2019《食物血糖生成指数测定方法》(仅测定方法，分级为FAO/WHO口径) · 国家卫健委 · 2019"
    private const val SRC_WHO_SUGAR = "WHO《成人和儿童糖摄入量指南》 WHO/NMH/NHD/15.3 · 世界卫生组织 · 2015"
    private const val SRC_LIPID_DIET = "《成人高脂血症食养指南(2023年版)》 · 国家卫健委办公厅 · 2023"
    private const val SRC_LIPID_GUIDE = "《中国血脂管理指南(2023年)》 · 中华医学会心血管病学分会等 · 2023"
    private const val SRC_GOUT_DIET = "WS/T 560-2017《高尿酸血症与痛风患者膳食指导》 · 原国家卫计委 · 2017"
    private const val SRC_GOUT_GUIDE = "《中国高尿酸血症与痛风诊疗指南(2019)》 · 中华医学会内分泌学分会 · 2019"

    val categories: List<DietaryRefCategory> = listOf(
        DietaryRefCategory(
            title = "钠 · 盐 · 钾（高血压）",
            intro = "限钠增钾、改善钠钾比，是高血压膳食的核心。",
            items = listOf(
                DietaryRefItem(
                    indicator = "食盐 / 钠 每日上限（一般人群）",
                    levels = listOf(
                        DietaryRefLevel("食盐", "≤ 5 g/日"),
                        DietaryRefLevel("对应钠", "≈ 2000 mg/日"),
                    ),
                    caliber = "每日限量（2016版6g→2022版收紧至5g）",
                    appliesTo = listOf("一般人群", "高血压"),
                    source = SRC_GUIDE,
                ),
                DietaryRefItem(
                    indicator = "钠 参考摄入量（成人）",
                    levels = listOf(
                        DietaryRefLevel("适宜量 AI", "1500 mg/日"),
                        DietaryRefLevel("预防慢病建议 PI-NCD", "2000 mg/日"),
                    ),
                    caliber = "每日限量（老年段 AI 逐段下调，精确值以标准原表为准）",
                    appliesTo = listOf("一般人群"),
                    source = SRC_DRIS,
                ),
                DietaryRefItem(
                    indicator = "钠（高血压人群 · 更严）",
                    levels = listOf(DietaryRefLevel("钠盐目标", "钠 < 2000 mg/日（≈5g盐，2024修订版与一般人群一并收紧，更需严格执行）")),
                    caliber = "每日限量（高血压人群）",
                    appliesTo = listOf("高血压"),
                    source = SRC_HTN,
                ),
                DietaryRefItem(
                    indicator = "钾 参考摄入量（成人）",
                    levels = listOf(
                        DietaryRefLevel("适宜量 AI", "2000 mg/日"),
                        DietaryRefLevel("预防慢病建议 PI-NCD", "3600 mg/日"),
                    ),
                    caliber = "每日推荐（增钾利于降压）",
                    appliesTo = listOf("一般人群", "高血压"),
                    source = SRC_DRIS,
                ),
            ),
        ),
        DietaryRefCategory(
            title = "糖 · GI · 碳水（糖尿病 · 控糖）",
            intro = "控添加糖、优选低 GI，是控糖与糖尿病膳食的要点。",
            items = listOf(
                DietaryRefItem(
                    indicator = "添加糖 每日上限（成人）",
                    levels = listOf(
                        DietaryRefLevel("理想", "< 25 g/日"),
                        DietaryRefLevel("上限", "≤ 50 g/日"),
                    ),
                    caliber = "每日限量（添加糖，不含完整水果/奶天然糖）",
                    appliesTo = listOf("糖尿病", "控糖", "肥胖"),
                    source = SRC_GUIDE,
                ),
                DietaryRefItem(
                    indicator = "游离糖 供能比（WHO）",
                    levels = listOf(
                        DietaryRefLevel("强推荐", "< 10% 供能"),
                        DietaryRefLevel("进一步", "< 5% 供能（约25g）"),
                    ),
                    caliber = "占每日总能量百分比",
                    appliesTo = listOf("糖尿病", "肥胖"),
                    source = SRC_WHO_SUGAR,
                ),
                DietaryRefItem(
                    indicator = "GI 血糖生成指数 分级",
                    levels = listOf(
                        DietaryRefLevel("低 GI", "≤ 55"),
                        DietaryRefLevel("中 GI", "56 ~ 69"),
                        DietaryRefLevel("高 GI", "≥ 70"),
                    ),
                    caliber = "食物固有分级（FAO/WHO 口径，国内糖尿病指南采纳；WS/T 652 仅规定测定方法）",
                    appliesTo = listOf("糖尿病"),
                    source = SRC_DM,
                ),
                DietaryRefItem(
                    indicator = "碳水化合物 供能比（成人）",
                    levels = listOf(DietaryRefLevel("适宜范围", "50% ~ 65% 供能")),
                    caliber = "宏量营养素可接受范围 AMDR",
                    appliesTo = listOf("一般人群", "糖尿病"),
                    source = SRC_GUIDE,
                ),
            ),
        ),
        DietaryRefCategory(
            title = "嘌呤（痛风 · 高尿酸）",
            intro = "权威标准以“应避免/限制/可选择食物”定性指导为准；食物嘌呤“低/中/高”三级临界值并无国标条款（见待核项）。",
            items = listOf(
                DietaryRefItem(
                    indicator = "膳食总原则",
                    levels = listOf(
                        DietaryRefLevel("限制", "酒精、高嘌呤、高果糖饮食"),
                        DietaryRefLevel("鼓励", "奶制品、新鲜蔬菜、适量饮水"),
                        DietaryRefLevel("豆制品", "不推荐也不限制（如豆腐）"),
                    ),
                    caliber = "定性膳食指导原则",
                    appliesTo = listOf("痛风", "高尿酸血症"),
                    source = SRC_GOUT_GUIDE,
                ),
                DietaryRefItem(
                    indicator = "应避免 / 限制 / 可选择食物",
                    levels = listOf(
                        DietaryRefLevel("应避免", "动物内脏、贝壳类海产、浓肉汤肉汁"),
                        DietaryRefLevel("应限制", "高嘌呤畜肉/鱼、含果糖蔗糖食品、酒精(男≤2/女≤1单位/日)"),
                        DietaryRefLevel("可选择", "脱脂低脂奶300ml、蛋1个、蔬菜≥500g、低GI谷类、饮水≥2000ml"),
                    ),
                    caliber = "按食物类别的定性建议（标准附录嘌呤含量单位为 mg/kg）",
                    appliesTo = listOf("痛风", "高尿酸血症"),
                    source = SRC_GOUT_DIET,
                ),
                DietaryRefItem(
                    indicator = "营养素供能（痛风膳食）",
                    levels = listOf(
                        DietaryRefLevel("蛋白质", "1 g/kg/日（10%~20%供能，优选奶蛋）"),
                        DietaryRefLevel("碳水", "50%~60% 供能（优选低GI，全谷≥30%）"),
                        DietaryRefLevel("脂肪", "20%~30% 供能（合并肥胖≤25%）"),
                    ),
                    caliber = "每日营养素供能比",
                    appliesTo = listOf("痛风", "高尿酸血症"),
                    source = SRC_GOUT_DIET,
                ),
                DietaryRefItem(
                    indicator = "食物嘌呤「低/中/高」三级临界值",
                    levels = listOf(
                        DietaryRefLevel("低嘌呤", "< 25 mg/100g（部分资料用<50）"),
                        DietaryRefLevel("中嘌呤", "25 ~ 150 mg/100g"),
                        DietaryRefLevel("高嘌呤", "> 150 mg/100g"),
                    ),
                    caliber = "每100g食物嘌呤含量（惯例口径，非国标条款，版本不一）",
                    appliesTo = listOf("痛风"),
                    source = "临床营养学教材/科普惯例口径（WS/T 560-2017 与诊疗指南均未规定该三级临界值）",
                    pending = true,
                ),
                DietaryRefItem(
                    indicator = "每日嘌呤摄入 mg 上限",
                    levels = listOf(DietaryRefLevel("量化上限", "权威一手来源未给出数值")),
                    caliber = "两份权威文件仅作“限制高嘌呤”定性要求，无 mg 量化值",
                    appliesTo = listOf("痛风", "高尿酸血症"),
                    source = "WS/T 560-2017 / 诊疗指南(2019) 均未规定",
                    pending = true,
                ),
            ),
        ),
        DietaryRefCategory(
            title = "脂肪 · 胆固醇（高血脂）",
            intro = "控饱和脂肪与胆固醇、限反式脂肪，是血脂管理膳食要点。",
            items = listOf(
                DietaryRefItem(
                    indicator = "脂肪 供能比（成人）",
                    levels = listOf(DietaryRefLevel("适宜范围", "20% ~ 35% 供能（DRIs一般20%~30%）")),
                    caliber = "占每日总能量百分比",
                    appliesTo = listOf("一般人群", "高血脂"),
                    source = SRC_LIPID_DIET,
                ),
                DietaryRefItem(
                    indicator = "饱和脂肪 供能比上限",
                    levels = listOf(
                        DietaryRefLevel("一般人群", "< 10% 供能"),
                        DietaryRefLevel("高胆固醇/高LDL", "< 7% 供能"),
                    ),
                    caliber = "占每日总能量百分比",
                    appliesTo = listOf("一般人群", "高血脂"),
                    source = SRC_LIPID_GUIDE,
                ),
                DietaryRefItem(
                    indicator = "膳食胆固醇 每日上限",
                    levels = listOf(
                        DietaryRefLevel("高脂血症", "< 300 mg/日"),
                        DietaryRefLevel("高胆固醇血症", "< 200 mg/日"),
                    ),
                    caliber = "每日膳食胆固醇总量",
                    appliesTo = listOf("高血脂"),
                    source = SRC_LIPID_DIET,
                ),
                DietaryRefItem(
                    indicator = "反式脂肪酸 每日上限",
                    levels = listOf(DietaryRefLevel("供能比", "< 1% 供能（约≤2g/日）")),
                    caliber = "占每日总能量百分比",
                    appliesTo = listOf("一般人群", "高血脂"),
                    source = SRC_LIPID_GUIDE,
                ),
            ),
        ),
        DietaryRefCategory(
            title = "能量 · 膳食纤维 · 蛋白质（通用）",
            intro = "宏量与能量的日常参考；部分逐档数值以 DRIs 原表为准。",
            items = listOf(
                DietaryRefItem(
                    indicator = "膳食纤维 每日推荐（成人）",
                    levels = listOf(DietaryRefLevel("推荐", "25 ~ 30 g/日")),
                    caliber = "每日膳食纤维总量",
                    appliesTo = listOf("一般人群", "高血脂"),
                    source = SRC_DRIS,
                ),
                DietaryRefItem(
                    indicator = "蛋白质 每日推荐（成人）",
                    levels = listOf(
                        DietaryRefLevel("供能比", "10% ~ 15% 供能"),
                        DietaryRefLevel("参考量", "男约65g / 女约55g（轻体力，以DRIs原表为准）"),
                    ),
                    caliber = "供能比及每日推荐量 RNI",
                    appliesTo = listOf("一般人群"),
                    source = SRC_DRIS,
                    pending = true,
                ),
                DietaryRefItem(
                    indicator = "每日能量参考（EER，轻体力）",
                    levels = listOf(
                        DietaryRefLevel("成年男性(约)", "≈ 2250 kcal/日"),
                        DietaryRefLevel("成年女性(约)", "≈ 1800 kcal/日"),
                    ),
                    caliber = "每日能量需要量（逐档确切值待核 DRIs 2023 原表）",
                    appliesTo = listOf("一般人群"),
                    source = SRC_DRIS,
                    pending = true,
                ),
            ),
        ),
        DietaryRefCategory(
            title = "营养标签：NRV 与「低/高」声称（GB 28050）",
            intro = "看包装营养成分表时的参考基准；NRV% = 含量 ÷ NRV × 100%。",
            items = listOf(
                DietaryRefItem(
                    indicator = "营养素参考值 NRV（每日）",
                    levels = listOf(
                        DietaryRefLevel("能量 / 蛋白质", "8400 kJ(≈2000kcal) / 60 g"),
                        DietaryRefLevel("脂肪 / 碳水", "≤ 60 g / 300 g"),
                        DietaryRefLevel("钠 / 钾", "2000 mg / 2000 mg"),
                        DietaryRefLevel("膳食纤维 / 胆固醇", "25 g / ≤ 300 mg"),
                    ),
                    caliber = "标签用每日基准值（4岁以上一般人群）",
                    appliesTo = listOf("标签识别"),
                    source = SRC_GB,
                ),
                DietaryRefItem(
                    indicator = "含量声称「低 / 无」阈值（每100g固体）",
                    levels = listOf(
                        DietaryRefLevel("低钠 / 无钠", "≤120 mg / ≤5 mg"),
                        DietaryRefLevel("低糖 / 无糖", "≤5 g / ≤0.5 g"),
                        DietaryRefLevel("低脂 / 无脂", "≤3 g / ≤0.5 g"),
                        DietaryRefLevel("低胆固醇 / 无", "≤20 mg / ≤5 mg"),
                    ),
                    caliber = "每100g固体（液体阈值另计）的含量声称条件",
                    appliesTo = listOf("标签识别"),
                    source = SRC_GB,
                ),
                DietaryRefItem(
                    indicator = "含量声称「来源 / 高」阈值（每100g固体）",
                    levels = listOf(
                        DietaryRefLevel("膳食纤维 来源/高", "≥3 g / ≥6 g"),
                        DietaryRefLevel("蛋白质 来源/高", "≥6 g / ≥12 g"),
                    ),
                    caliber = "每100g固体的“来源/含有”与“高/富含”条件",
                    appliesTo = listOf("标签识别"),
                    source = SRC_GB,
                ),
            ),
        ),
    )

    val sources: List<DietaryRefSource> = listOf(
        DietaryRefSource("《中国居民膳食指南(2022)》", "", "中国营养学会", "2022"),
        DietaryRefSource("《中国居民膳食营养素参考摄入量(DRIs)》", "WS/T 578 系列", "中国营养学会", "2018/2023"),
        DietaryRefSource("《食品安全国家标准 预包装食品营养标签通则》", "GB 28050-2011", "原卫生部（现国家卫健委）", "2011"),
        DietaryRefSource("《中国高血压防治指南(2018年修订版)》", "", "高血压联盟(中国)等", "2018"),
        DietaryRefSource("《中国2型糖尿病防治指南(2020年版)》", "", "中华医学会糖尿病学分会", "2020"),
        DietaryRefSource("《食物血糖生成指数测定方法》(仅测定方法)", "WS/T 652-2019", "国家卫生健康委员会", "2019"),
        DietaryRefSource("《成人和儿童糖摄入量指南》", "WHO/NMH/NHD/15.3", "世界卫生组织(WHO)", "2015"),
        DietaryRefSource("《成人高脂血症食养指南(2023年版)》", "", "国家卫健委办公厅", "2023"),
        DietaryRefSource("《中国血脂管理指南(2023年)》", "", "中华医学会心血管病学分会等", "2023"),
        DietaryRefSource("《高尿酸血症与痛风患者膳食指导》", "WS/T 560-2017", "原国家卫计委", "2017"),
        DietaryRefSource("《中国高尿酸血症与痛风诊疗指南(2019)》", "", "中华医学会内分泌学分会", "2019"),
        DietaryRefSource("《成人和儿童钠/钾摄入量指南》", "", "世界卫生组织(WHO)", "2013"),
    )
}
