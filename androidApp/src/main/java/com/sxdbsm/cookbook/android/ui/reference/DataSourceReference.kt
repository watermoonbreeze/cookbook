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
                        DietaryRefLevel("交叉核对", "USDA FoodData Central（进口/缺项，如蓝莓、全麦面包）"),
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
        DietaryRefSource("八大菜系 + 家常菜整理", "", "公认餐饮常识", ""),
    )
}
