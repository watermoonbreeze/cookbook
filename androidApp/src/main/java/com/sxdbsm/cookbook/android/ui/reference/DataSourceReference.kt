package com.sxdbsm.cookbook.android.ui.reference

/**
 * @File : DataSourceReference
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 数据来源——App 内食材分类/营养/GI/嘌呤/预设菜品各自的来源，透明列给用户
 * <p>
 * 复用 [DietaryRefCategory]/[DietaryRefItem] 模型与 ReferenceScaffold 渲染，与「膳食参考依据」同一视觉。
 * 记档见 `.ai-context/docs/数据来源.md`。含"用户上报"规划位(联网+账号后家庭菜品/食材可上报充实预设库)。
 * <p>
 * [AI生成] 用户要求：分类/食材/菜品来源都标清楚，并预留用户上报入口(后续联网关联登录)。
 **/
object DataSourceReference {

    const val disclaimer =
        "本 App 的食材营养 / GI / 嘌呤 / 忌口等健康数据，为 AI 依据下列公开权威资料整理核对的参考值，" +
            "非医嘱、非逐项官方逐页核验。因品种 / 产地 / 加工差异，数值本有区间。日常参考即可；" +
            "慢病管理与精确摄入控制，请以《中国食物成分表》原书、专业营养师或医生建议为准。"

    val categories: List<DietaryRefCategory> = listOf(
        DietaryRefCategory(
            title = "食材分类框架",
            intro = "食材归到主食/蔬菜/鱼肉蛋等大类的划分依据。",
            items = listOf(
                DietaryRefItem(
                    indicator = "食材大类划分",
                    levels = listOf(DietaryRefLevel("依据", "《中国居民膳食指南(2022)》食物分类框架")),
                    caliber = "调料/油脂不属九大营养类，单列",
                    appliesTo = emptyList(),
                    source = "《中国居民膳食指南(2022)》 · 中国营养学会 · 2022",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "食材营养数值（每100g）",
            intro = "热量/蛋白/脂肪/碳水/纤维/钠/钾/钙等每100g可食部数值的来源。",
            items = listOf(
                DietaryRefItem(
                    indicator = "食材营养成分",
                    levels = listOf(
                        DietaryRefLevel("主来源", "《中国食物成分表(标准版·第6版)》· 中国疾控中心营养与健康所"),
                        DietaryRefLevel("在线查询", "食物营养成分查询平台 nlc.chinanutri.cn（能量 kJ÷4.184=kcal）"),
                        DietaryRefLevel("交叉核对", "USDA FoodData Central（进口/缺项，如蓝莓、全麦面包、饱和脂肪值）"),
                    ),
                    caliber = "每100g可食部；谷物/干货按生/干、蔬菜鲜菌按鲜品；每条 ref 记实际来源，review：权威成分表=已核、估算=待核",
                    appliesTo = emptyList(),
                    source = "《中国食物成分表(标准版·第6版)》 · 中国疾控中心营养与健康所",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "血糖生成指数（GI）",
            intro = "仅对含碳水食物（主食/薯类/水果/部分蔬菜）标注。",
            items = listOf(
                DietaryRefItem(
                    indicator = "食材 GI 值",
                    levels = listOf(DietaryRefLevel("来源", "悉尼大学 GI 数据库 / 国际血糖指数表(2008)，糖尿病膳食速查交叉参考")),
                    caliber = "纯蛋白/油脂/多数绿叶菜不含 GI，不标",
                    appliesTo = emptyList(),
                    source = "悉尼大学 GI 数据库 / 国际血糖指数表 · 2008",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "嘌呤（痛风/高尿酸）",
            intro = "仅对肉/禽/水产/内脏/豆类/菌菇标注；蔬果/主食/奶通常低嘌呤。",
            items = listOf(
                DietaryRefItem(
                    indicator = "食材嘌呤含量",
                    levels = listOf(DietaryRefLevel("来源", "《成人高尿酸血症与痛风食养指南(2024年版)》· 国家卫健委；常用食物嘌呤汇总表交叉")),
                    caliber = "参考值；分级口径见「膳食参考依据」（三级临界值非国标）",
                    appliesTo = emptyList(),
                    source = "《成人高尿酸血症与痛风食养指南(2024年版)》 · 国家卫生健康委员会 · 2024",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "慢病忌口 · 宜忌建议",
            intro = "食材/酒类对慢病人群（痛风/高血压/高血脂/糖尿病/脂肪肝）的宜忌建议来源。忌口按临床指南定性，不按食物嘌呤 mg 数。",
            items = listOf(
                DietaryRefItem(
                    indicator = "酒类忌口（慢病人群）",
                    levels = listOf(
                        DietaryRefLevel("痛风/高尿酸", "《中国高尿酸血症与痛风诊疗指南(2019/2024)》《成人高尿酸血症与痛风食养指南(2024·卫健委)》——应避免饮酒、啤酒风险最高；限含糖饮料及高果糖食物（果糖促尿酸生成，独立于嘌呤）"),
                        DietaryRefLevel("高血压", "《中国高血压防治指南(2024年修订版)》——限酒 / 不饮酒"),
                        DietaryRefLevel("高血脂", "《中国血脂管理指南(2023年)》——限酒"),
                        DietaryRefLevel("糖尿病", "《中国2型糖尿病防治指南》《成人糖尿病食养指南(2023·卫健委)》——不推荐饮酒（含低血糖风险，甜米酒/酒酿含糖）；限添加糖及高GI食物（含糖饮料应避免）"),
                        DietaryRefLevel("脂肪肝", "《非酒精性 / 酒精性脂肪性肝病防治指南》——应避免饮酒"),
                    ),
                    caliber = "临床指南生活方式建议，仅供参考·非医嘱；酒类忌口按指南定性（应避免/限制），不按食物嘌呤 mg 数（酒精升尿酸是代谢机制）",
                    appliesTo = emptyList(),
                    source = "各慢病临床指南（见下方来源清单）",
                ),
                DietaryRefItem(
                    indicator = "食材忌口 / 宜忌（慢病人群）",
                    levels = listOf(
                        DietaryRefLevel("整理方式", "AI 依据慢病膳食指南与常识整理该食材对各病种的宜 / 忌 / 限"),
                        DietaryRefLevel("阈值口径", "钠 / GI / 嘌呤分级见「膳食参考依据」；嘌呤三级临界非国标·惯例口径"),
                        // [AI生成] F#附2:内脏/腌腊/加工/高GI/高胆固醇 一批食材忌口补齐的指南依据(逐条见下方来源清单)。
                        DietaryRefLevel(
                            "食材类别覆盖",
                            "动物内脏（痛风应避免 / 高血脂限量）、咸腊·加工肉·干海味·腌菜酱（高血压应避免）、精制高 GI 主食（糖尿病限量）、" +
                                "高胆固醇蛋奶与奶油黄油（高血脂限 / 避免）——据 WS/T 560-2017、各《成人食养指南(2023–2024·卫健委)》与《中国血脂管理指南(2023)》定性整理",
                        ),
                    ),
                    caliber = "仅供参考·非医嘱；人工指南建议优先于数值判定（避免因数值低而误判可食，如低嘌呤啤酒对痛风仍应避免、内脏录低嘌呤仍应避免）",
                    appliesTo = emptyList(),
                    source = "各慢病食养指南 / 膳食指导标准（见下方来源清单）",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "预设菜品与菜系",
            intro = "内置菜品的做法/步骤与菜系归类来源。",
            items = listOf(
                DietaryRefItem(
                    indicator = "预设菜品（做法/步骤/配料）",
                    levels = listOf(
                        DietaryRefLevel("整理方式", "AI 依据公开家常菜常识整理，非官方权威菜谱"),
                        DietaryRefLevel("说明", "同名菜各地做法/用量有差异，仅作日常记录与推荐参考，可自行编辑"),
                    ),
                    caliber = "参考整理，非官方权威菜谱",
                    appliesTo = emptyList(),
                    source = "AI 整理（公开家常菜常识）",
                ),
                DietaryRefItem(
                    indicator = "菜系分类",
                    levels = listOf(DietaryRefLevel("依据", "中国传统八大菜系（川鲁粤苏闽浙湘徽）+ 家常菜，公认餐饮常识分类")),
                    caliber = "便于筛选的参考，可能地域交叉，非官方权威认定",
                    appliesTo = emptyList(),
                    source = "八大菜系（公认餐饮常识）",
                ),
            ),
        ),
        DietaryRefCategory(
            title = "用户上报（规划中）",
            intro = "让家庭好菜反哺预设库，越用越丰富。",
            items = listOf(
                DietaryRefItem(
                    indicator = "家庭菜品/食材上报",
                    levels = listOf(
                        DietaryRefLevel("规划", "后续登录后，家庭自建的菜品/食材可自愿上报，经审核并入预设库，不断充实"),
                        DietaryRefLevel("展示", "被采纳的用户贡献将在本页标注来源"),
                    ),
                    caliber = "需联网 + 账号，规划中（详见待办）",
                    appliesTo = emptyList(),
                    source = "待联网功能上线后开放",
                    pending = true,
                ),
            ),
        ),
    )

    val sources: List<DietaryRefSource> = listOf(
        DietaryRefSource("《中国居民膳食指南(2022)》", "", "中国营养学会", "2022"),
        DietaryRefSource("《中国食物成分表(标准版·第6版)》", "", "中国疾控中心营养与健康所", ""),
        DietaryRefSource("食物营养成分查询平台 nlc.chinanutri.cn", "", "中国疾控中心营养与健康所", ""),
        DietaryRefSource("USDA FoodData Central (fdc.nal.usda.gov)", "", "美国农业部", ""),
        DietaryRefSource("悉尼大学 GI 数据库 / 国际血糖指数表", "", "University of Sydney", "2008"),
        DietaryRefSource("《成人高尿酸血症与痛风食养指南(2024年版)》", "", "国家卫生健康委员会", "2024"),
        DietaryRefSource("《中国高尿酸血症与痛风诊疗指南(2019/2024)》", "", "中华医学会内分泌学分会等", ""),
        DietaryRefSource("《中国高血压防治指南(2024年修订版)》", "", "国家心血管病中心 / 中国高血压联盟等", "2024"),
        DietaryRefSource("《中国血脂管理指南(2023年)》", "", "中国血脂管理指南修订联合专家委员会", "2023"),
        DietaryRefSource("《中国2型糖尿病防治指南(2020年版)》", "", "中华医学会糖尿病学分会", "2020"),
        DietaryRefSource("《非酒精性 / 酒精性脂肪性肝病防治指南》", "", "中华医学会肝病学分会", ""),
        // [AI生成] F#附2:食材忌口补漏批(内脏/腌腊/加工/高GI/高胆固醇)所据的食养指南与膳食指导标准。
        DietaryRefSource("WS/T 560-2017《高尿酸血症与痛风患者膳食指导》", "", "国家卫生健康委卫生行业标准", "2017"),
        DietaryRefSource("《成人高血压食养指南(2023年版)》", "", "国家卫生健康委办公厅", "2023"),
        DietaryRefSource("《成人高脂血症食养指南(2023年版)》", "", "国家卫生健康委办公厅", "2023"),
        DietaryRefSource("《成人糖尿病食养指南(2023年版)》", "", "国家卫生健康委办公厅", "2023"),
        DietaryRefSource("DASH 饮食（限钠增钾）", "", "美国 NHLBI", ""),
        DietaryRefSource("八大菜系 + 家常菜整理", "", "公认餐饮常识", ""),
    )
}
