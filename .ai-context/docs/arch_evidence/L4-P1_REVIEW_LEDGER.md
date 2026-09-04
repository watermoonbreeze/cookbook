# L4-P1 审核增量台账

> 蓝图：`docs/feature/分享链接解析_实施蓝图.md`（DRAFT·L7）
> 上游：方案 v1.1.3（用户拍板通过）+ 交互规范定稿 + 2026-09-03 事实盘点
> 规则：`experience/12_多模型协作与实施蓝图规范.md` §10.1——逐检查点追加，不等最终结论；已标 PASS 的检查点不重复扫描。
> 状态图：DRAFT →（四审+挑战）→ BLUEPRINT_READY → 编码 → 终审 → ACCEPTED

## 台账头

| 项 | 值 |
|---|---|
| 审核启动 | 2026-09-03（会话内） |
| 审核范围 | 蓝图 DRAFT 全文 + §2 事实地图所列代码 + 上游方案 v1.1.3 + 交互规范 |
| Reviewer ① | google_architecture_engineer（架构规范审）·进行中 |
| Reviewer ② | google_quality_engineer（质量+测试完备性审）·进行中 |
| Reviewer ③ | apple_software_behavior（行为透明/Tier 审）·进行中 |
| Reviewer ④ | GC-37 独立挑战者（只给蓝图不给设计理由·含 coverage audit）·进行中 |
| 用户已拍板前置 | 方案 v1.1.3 整体通过 / DP-L4-1=A 横幅扩 link / 步骤图纳入 / 基准样本机制——审核不重议已拍板项，只审蓝图对它们的落地质量。**2026-09-03 追加：DP-P1-9=A（dish.source 维持 "user"+share_link.dish_id 回指+标签 UI 延后 Phase2）** |

## 检查点记录（追加式）

### 2026-09-03 · Reviewer ③ apple_software_behavior（行为透明审）——已回·判定「需修订」（2 阻断+4 必改+4 可选）

**总判定**：需修订。Tier 框架与 T2 门控设计合格、无操纵性设计；两阻断均为「状态/告知悬空」、修复量小。

**🔴 阻断**：
| AF | 问题 | 唯一最小修复 |
|---|---|---|
| **B-01** | **state=1「已解析未存」悬空态**：状态机 4 值、列表 UI 只映射 3 组（0/2/3）——state=1 行从所有可见面蒸发（红点只数 0、横幅不显、列表无组），且编码时必然被临场发明行为 | 交互规范 D+蓝图 STEP-L4-10/§4 补第四组「已解析 · 未保存」：组序=待解析→已解析未存→解析失败→已存菜品；状态点=中性色 `onSurfaceVariant`+「已解析」；点击=**本地重算**（clear_text+config·不联网不 T2）开 ParseSheet 仍可存；T-L4-07 补断言；INV-L4-05 状态机不动 |
| **B-02** | **菜品来源标注零落点**（GC-21 型）：save() 现状不传 source（落默认 "user"）无 STEP 让它传 "link"；方案 §4.4 承诺「来自 下厨房」菜品页标签无任何改动项 | **推荐 A**：STEP-L4-5 增一条 `save() 传 source = if (prefillLinkId != null) "link" else "user"`（一处参数·数据诚实先行）；「菜品页浅灰标签」UI 显式登记 §1 延后项归 Phase2（届时 UX 补规格）。备选 B 全量补齐不推荐进本批 |

**🟡 必改**：
- **Y-01 重复分享去重未定义**（真缺口）：`insertShareLink` 前按 link 精确匹配活跃行，命中复用 id 并直接按 D.3 分派（幂等·防红点计数虚增·防重复存菜）。
- **Y-02 state=3 死跳转**：菜品软删后 selectDishById 过滤 status=1 返 null。分派层兜底：跳转前查活性，失活按「已解析未存」处理（可重存）。STEP-10.2+V3 补判据。
- **Y-03 T2 未预告图片下载**（流量副作用）：第二行加「和图片」→「打开后会自动提取菜名、食材、步骤**和图片**，由你确认后才保存。」（交 copywriter 过目·交互规范 F 同步）。
- **Y-04 新组件日志红线**：失败/诊断日志只记 host/错误类型/耗时/linkId；禁完整 URL（含 query）/菜名/clear_text 片段（对齐 new_dish_save 只记 count 口径）。

**⚪ 可选**：①方案 §七#1「已收到链接」Snackbar 由 T2 弹窗承接——蓝图补映射注记防误报；②GC-20~22 勾销表自评在 B-01/B-02 处不成立，修复后同步改；③onCreate-only 读 EXTRA 可接受；④「没识别到链接」文案够用。

**正面**：幂等诚实（markSaved 守卫/复制不污染）；「仅保存链接」0 态+红点催办可接受；失败文案四路归一诚实；步骤图失败静默克制；privacy（clear_text 本地/URL 去 query）过关；文案冻结仅缺 state=1 点文案与 Y-03 三字。

### 2026-09-03 · Reviewer ② google_quality_engineer（质量+测试完备性审）——已回·判定「需修订」（6 阻断+8 建议+5 可选）

**总判定**：需修订。骨架质量高（§5/§6/INV↔T/F1-F21 扎实、T-L4-05 判别性到位），六阻断全为蓝图/测试文本级修复，不动架构。**Q-01 是交付链级断点（AF-04 复刻形态）。**

**🔴 阻断**：
| AF | 问题 | 唯一最小修复 |
|---|---|---|
| **Q-01** | **预填链路无量纲载体**：`NewDishPrefill.ingredients: List<Ingredient>` 而 Ingredient 无 quantity/unitId；`addIngredient` 硬置克单位——§4.4 单位映射落不了地（「鸡蛋 1个」→**1 克**营养差 50 倍；「盐 适量」0.0 直传=0 克），T-L4-09 照样全绿 | §4/STEP-4.1：ingredients 类型**替换**为 `List<DishIngredient>`（FreePairing 生产点一行包装并入 allowlist）；STEP-5.1 applyPrefill 走新私有 `addPrefilledIngredient(di)`（绕过强制克单位·addIngredient 零改动）；T-L4-09 扩四列断言（mapToPrefill 加 `unitIdsByName` 入参；冬瓜 500g/鸡蛋 1个→个字典id/生抽勺→默认克/盐适量→**quantity==null**）；冻结 isMain 规则「折算克数≥100g 为主料」 |
| **Q-02** | **生产 config 加载路径未定义**+测试可手搓 config（字段名拼错=真机全链失败 14 测试全绿） | §8 夹具口径：config 一律经 `SeedResourceLoader.readText("parsers/xiachufang.json")` 解码（与生产同路径·禁手搓）；STEP-9.1 补生产加载点（VM init 同路径直读·Phase2 再接表路由 O-03） |
| **Q-03** | RemoteImageSaver §4 签名**无 connectionFactory** 与 GC-33/T-L4-10 自称有注入矛盾；且 CookbookStorage 未 init 在 androidApp 单测必炸（无 Robolectric） | 签名改构造注入 `connectionFactory`+`dirProvider`（默认参·对齐 StreamTransport.kt:93-96 先例）；T-L4-10 落 androidApp/src/test 照抄 StreamTransportTimeoutTest fake 模式；`"img"` 字面量改 `CookbookStorage.IMG_DIR_NAME` 常量 |
| **Q-04** | STEP-2.6 悬空（不存在）+夹具路径三处不一致；**实读定论：`commonMain/resources/parsers/`**（build.gradle.kts:40/62 双接线+SeedResourceLoader File 兜底恰好只覆盖该目录；androidUnitTest/resources 零先例且需改 gradle 违红线） | 补 STEP-2.6（样本落 commonMain/resources/parsers/·经 readText 读）；§4/§8/§10 三处路径统一；**夹具必须含锚点行+其后推荐流文本**（否则 T-L4-03 碰巧绿）；注记随 APK 打包 10-20KB 可接受 |
| **Q-05** | **T-L4-09 编码断言写反**：单元素 joinToString 无 `\|`——正确实现也必红 | 改 round-trip 契约：`decodeImagePaths(steps[i].imagePath)==listOf(本地名)`；删管道计数断言 |
| **Q-06** | **fromJson 转义分支零覆盖**：干净夹具下忘写 removeSurrounding 全绿，真机 WebView 双重编码串恒 null 必挂 | 新增 T-L4-15：fromJson(带外层引号+内部转义) 非 null/`\n` 还原/emoji 中文不损/stepImages 完整/`fromJson("null")("")`→null；夹具保持干净 JSON |

**🟡 建议**：S-01（GC-09 补 HomeMealMutationBoundaryTest/HomeFocusSwitcherTest——HomeViewModel 构造签名将变编译红）；S-02（红点数据源与 §6 矛盾→定案 activity 作用域 `LinkBadgeViewModel` 由 MainScaffold 取·LinkListViewModel 维持路由作用域）；S-03（URL 提取抽 `extractUrl` 纯函数+尾随中文标点剥离四用例）；S-04（INV-L4-09 补 VM 级或 `shouldSkipPreselect` 纯函数行为断言·非仅 grep）；S-05（grep 阈值按必然命中数定：markUserTouched≥2/imported≥3；:684 区域 grep 改精确串）；S-06（T-L4-08 指纹变化改 forceReseedBaseData 两次）；S-07（T-L4-07 补软删后即不可见断言）；S-08（横幅推导抽 `shouldShowLinkBanner` 纯函数四态直测+LAST_SHOWN 写入时机=首帧展示时）。

**⚪ 可选**：O-01（grep 命中改≥1）；O-02（updateTitle 顺手断言）；O-03（parse_fun 表值 vs 硬编码双真相源注记 Phase2）；O-04（stepImages 多于文本反向用例）；O-05（烹饪方式双命中保序用例+「序」定义钉死）。

**修订后需复验**：§4/§7/§8/§10 交叉引用一致性（NewDishPrefill 类型替换牵动 STEP-4.1/5.1、allowlist 增 FreePairingScreen、T-L4-09 三处）。

### 2026-09-03 · Reviewer ① google_architecture_engineer（架构规范审）——已回·判定「需修订」（7 阻断+13 建议+3 可选·四审第三回）

**总判定**：需修订。核心架构方向成立（模块边界/状态机骨架/事实地图高保真/复用决策过红线/BL 自查到位——正面结论 7 条）；7 AF 均为「就地补决策/补 allow 行」级修复，AF-02 含真分叉需升级用户拍板。

**🔴 阻断**：
| AF | 问题 | 唯一最小修复 |
|---|---|---|
| **ARCH-01** | **DP-L4-1=A 落地是死代码**（BL-12 表面合规）：`:684` 判定还过 `vm.isPendingReview(id)`→`listPendingReview()`→`WHERE i.source='ai'` 把 link 挡在集合外；SQL 扩 IN('ai','link') 又会污染待复核 Tab 语义 | `IngredientPickerViewModel` 新增 `isLinkNutritionPending(id)`（走 nutritionRepo.review 判定·绕开 source 过滤）；`:684` 条件改 `(auto\|\|ai)&&isPendingReview \|\| link&&isLinkNutritionPending`；allowlist 增该 VM；补测试 |
| **ARCH-02** | **菜品来源标注整链无落点 + dish.source="link" 触发未审计门控**：`DishesScreen:397 if(d.source=="user")` 会让链接菜失去删除入口；多处 `source='user'` 过滤未审计；**真分叉升级用户拍板** | **推荐 (a)**：dish.source 维持 "user"，来源关系走 `share_link.dish_id` 回指（表结构天然承载·门控零扰动）；「来自下厨房」标签 UI 显式登记延后 Phase2；备选 (b) 坚持写 "link" 需新增 dish-source 门控核对表（改动面大不推荐） |
| **ARCH-03** | state=1 无分派/无分组定义（与 B-01 互证） | 冻结「1→不弹 T2·以库内 clear_text 重跑 parse 展示（GC-13 复用主路径）·不重新 WebView 采集；分组四组或 1 并入 0 组」；§6 补转移行 |
| **ARCH-04** | ParseViewModel 依赖面 `…` 未冻结+parse_dictionary write-only（与 Q-02 互证） | 冻结构造全签名（linkRepo/parser/extractor/saver/dishRepo/ingredientRepo·删 app）；配置=Phase1 硬编码 source→文件名映射·parse_dictionary 注记 seed-only Phase2 路由表；非 xiachufang 域名按同模板自然落 FAILED（V5 判据注明有意） |
| **ARCH-05** | allowlist 漏 androidApp/src/test（T-L4-09/10/13 无处安放） | 增一行 `androidApp/src/test/java/com/sxdbsm/cookbook/android/**` |
| **ARCH-06** | STEP 5.9 只写 autoAddMessage 不递增 autoAddSerial→Snackbar 永不显示 | 冻结「同一 copy 中 message 与 serial+1 成对」（既有惯例 :445-446）；T-L4-13/真机补断言 |
| **ARCH-07** | 红点数据源「共享 VM」两实例矛盾（与 Q-S02 互证） | 冻结「MainScaffold 体内 koinViewModel()=Activity 域单实例+collectAsState 传参；LINK_LIST composable 经参数收同一实例（不自行 koinViewModel）」；§6 生命周期表改 Activity 常驻 |

**🟡 建议**：S-01（跨 Activity Intent 补 FLAG_ACTIVITY_CLEAR_TOP\|SINGLE_TOP）；S-02（WebView 回调单发保证：AtomicBoolean once+removeCallbacks）；S-03（T2 dismiss 分支冻结=仅保存链接路径）；S-04（§5「clear_text 重算真相源」表述过强：stepImages/og:image 是 DOM 快照不在 clear_text——修正+Phase2 注记）；S-05（ParseConfig.steps selector 字段是死配置：标 Phase2 或删）；S-06（=Q-03 connectionFactory 注入+8s 软限落 VM）；S-07（PrefetchState/ParsedStepUi 字段集补 §4 表）；S-08（todayProvider 默认值+showLinkBanner 载体+LAST_SHOWN 写入时机=init 首值判定）；S-09（AndroidModule 允许操作扩「VM 工厂行补 get()」）；S-10（**INSERT OR IGNORE ON CONFLICT 混合语法在 sqlite_3_18 编译必败**——改纯 INSERT OR IGNORE 靠 UNIQUE 兜）；S-11（两段加载文案 vs 单 LOADING 调和）；S-12（cookingMethodName 单值与多值并存冻结互斥规则）；S-13（孤儿图片治理域不存在——改「同策略+技术债登记」）。

**⚪ 可选**：O-01（insert 与同意点击慢盘竞态注记）；O-02（=Y-01 重复分享注记）；O-03（listGrouped 全表查正面确认非债）。

**三审互证收敛**（5 处独立同发现）：state=1（B-01/ARCH-03）、config 加载（Q-02/ARCH-04）、RemoteImageSaver 注入（Q-03/ARCH-S06）、红点 VM 作用域（Q-S02/ARCH-07）、来源标注（B-02/ARCH-02）。

### 2026-09-03 · Reviewer ④ GC-37 独立挑战——已回·8 ISSUE+4 NIT+附加 A1~A8+coverage audit（四审全部完成）

**十项固定挑战**：#1 签名核对 FINE（附 2 局部矛盾）·#2 行号 NIT（2 处漂 1 行：色系墙 if 实在 :145/判定实在 :685）·#3 编译可达 **ISSUE**·#4 状态机完备 **ISSUE**·#5 数据流死角 **ISSUE**·#6 测试可达 **ISSUE**·#7 STEP 字面量 **ISSUE**·#8 allowlist 差集 **ISSUE**·#9 冻结值核对 **ISSUE（含功能报废级）**·#10 跨批冲突 FINE。

**ISSUE 按严重度**：
1. 🔴 **STEP 2.3 fromJson 实现错误**：evaluateJavascript 回调是**双层 JSON 编码**，removeSurrounding 只剥外层引号不解内部转义（`\n`/`\"`）→decodeOrNull 恒 null→**整功能恒走 FAILED**。修复=先 `decodeFromString<String>` 解一层再解对象（或 JS 返回对象）。
2. 🔴 **state=1 黑洞+列表路径宿主未定义**（三方收敛+独有）：列表路径的 ParseViewModel/WebView 由谁创建无字；§5「clear_text 重算」是幻影声明（无读取路径）。
3. 🔴 **三处编译级遗漏**：NewDishViewModel/HomeViewModel 构造参数+AndroidModule 注册行、MainActivity MainScaffold 调用点传参。
4. 🔴 **T-L4-10 不可构造**（=Q-03）+allowlist 缺 androidApp 测试（=ARCH-05）+STEP 2.6 悬挂（=Q-04）。
5. 🟡 **LAST_SHOWN 写入回环**（横幅自噬）：observeString 响应式写当天值会立刻重发→推导翻 false→横幅刚显示就被自己关掉；须定一次性冷读。
6. 🟡 **isMain 规则未定义**：恒 false 会触发既有 mainMissingHint「请标主料」——与「存为菜品」一键体验冲突。
7. 🟡 parse_dictionary 死表（=ARCH-04）。
8. 🟡 「grep linePairs ParseConfig.kt」判据易 0 命中（字面量在 json 不在类）。

**附加**：A1 死表/A2 config 加载者/A3 **方案 §六 QuantityConverter 全仓不存在（方案虚构·蓝图盘点处置应点名）**/A4 缩略图未对齐 8~12KB 字节限制/A5 重复分享不去重（=Y-01/O-02）/A6 og:image 无测试断言/A7 note 零下游断链/A8 变量名对不上（savedId vs savedDishId·MINOR）。

**Coverage Audit 结论**：§1/§2/§3.2/§3.4/§4.1/§4.4 主体/复用/Phase1/数据来源/V1~V10 均 Present；**缺口 C1=菜品来源标注（方案 §4.4 末行零落点·与 B-02/ARCH-02 三方收敛）**；C2 note 预填断链（A7·可选）；C5 接收 T1 Snackbar 未承载（低）；Semantic 风险=fromJson（#9）+LAST_SHOWN 回环（#5）。

**四审汇总（去重后独立阻断 16 项）**：fromJson 报废级（GC-37#9）/量纲载体（Q-01）/来源标注真分叉（B-02+ARCH-02+C1·推荐 a 待用户确认）/state=1+宿主（三方）/config 加载+死表（三方）/saver 注入（三方）/夹具位置（两方）/T-L4-09 断言写反（Q-05）/allowlist 测试目录（两方）/serial 不递增（ARCH-06）/红点 VM（两方）/:684 死代码（ARCH-01）/编译遗漏×3（GC-37#3）/LAST_SHOWN 回环（GC-37#5）/isMain（两方）/INSERT OR IGNORE ON CONFLICT 语法（ARCH-S10）。

### 2026-09-03 · v1.1 修订落地记录（ARCH·四审全部合入）

蓝图重写为 **DRAFT v1.1**，16 项独立阻断全部消解：①fromJson 改**双层解码**（decodeFromString<String> 先解一层——STEP 2.3 冻结+T-L4-15）②量纲载体=`List<DishIngredient>` 类型替换+`addPrefilledIngredient` 绕过克单位+FreePairing 一行适配+T-L4-09 四列扩断言（适量→quantity==null 红牌）③来源标注=**DP-P1-9 拍板 (a)**（dish.source 维持 "user"+share_link.dish_id 回指+标签 UI 延后 Phase2 显式登记——**待用户最终确认可推翻**）④state=1+宿主=**四组**+**DP-P1-1 双入口模式**（列表 0/1/2 统一转发 ShareReceiverActivity·ParseSheet 宿主唯一）+**share_link 增 step_images 列**（DP-P1-7 本地重建全量真相源·rebuildFrom 复用 parse=GC-13）⑤config=DP-P1-10 硬编码映射+测试禁手搓（生产同路径）+parse_dictionary 注记 Phase2 路由表 ⑥saver=构造注入 connectionFactory/dirProvider+缩略 8~12KB（A4）⑦夹具=commonMain/resources/parsers/ 定论（Q-04·STEP 2.6 补）+含锚点+推荐流 ⑧T-L4-09 改 round-trip 语义（shared 侧单文件名断言+androidApp 侧 decode round-trip 落点）⑨allowlist 补 androidApp/src/test+FreePairing+PickerVM ⑩serial 成对同一 copy（STEP 5.10）⑪LinkBadgeViewModel Activity 域单实例+参数传递 ⑫isLinkNutritionPending（ARCH-01 死代码修复+T-L4-18）⑬编译遗漏三处补 §4（NewDishViewModel/HomeViewModel 构造+注册行+MainActivity 调用点）⑭LAST_SHOWN 一次性冷读+禁 observe（INV 载体+grep 判据）⑮isMain=折算克数≥100g（DP-P1-11）⑯纯 INSERT OR IGNORE（ARCH-S10）。建议项吸收：S-01 Home* 回归基线/S-03 extractUrl 纯函数+尾随标点/S-04 真相源表述修正/S-05 死配置标注/S-07 PrefetchState 字段表/S-08 shouldShowLinkBanner 纯函数/S-11 单文案调和/S-12 单值互斥/S-13 孤儿图片改写/Y-01 去重 DP-P1-8/Y-02 活性兜底/Y-03 文案「和图片」/Y-04 日志红线/A3 QuantityConverter 虚构处置/A6 og:image 断言/A7 description 字段/A8 savedId。交互规范同步 v1.1（D 节四组+F 节文案+A.1 单文案）。**剩余：v1.1 修订后复核（四审修订处抽查）+用户确认 DP-P1-9 → BLUEPRINT_READY。**

### 2026-09-03 · v1.1 修订后复核（google_quality_engineer 二次介入）——已回·「仍有缺口」→ 复核发现的 R/Y 已全部消解 → **BLUEPRINT_READY**

- **16/16 四审阻断落地确认**（逐项对照台账定位到 STEP/决策点/类型表行，无丢失无空转）；STEP 编号连续；DP-P1-1~13 引用齐全；grep 判据逐条推演可达。
- **重写暴露 2 项阻断级不一致（当场已修）**：R-1 LinkPrefillMapper(shared) 签名引 StoredImagePair(androidApp)——跨模块反向依赖编译必败 → 改 shared 自有 `StoredImageRef(name,thumbName)` 轻量类型+VM 一行映射；R-2 state=1 重建 title §4 置空 vs §5 四列全量矛盾 → 统一 `title=row.title` 直用库值。
- **10 项小瑕疵 Y-1~10 全部消解**：Y-1 LinkBadge 消费方表述裁定（DishesScreen+LinkList 共用·Home 自建 stateIn）/Y-2 allowlist T-ID 归属（shared 01~09/12/14~18·androidApp 10/13）/Y-3 INV-L4-19/20 补+11/16 载体注记/Y-4 STEP9.2 config 生产加载补（与测试同路径）/Y-5 STEP5.11 NewDishScreen+grep/Y-6 DraftIngredient 增 unitName+ingredientId+VM 组装说明+断言列/Y-7 grep 去空格/Y-8 `applyParseResult` 单事务组合方法（取代分立 updateClearText/updateParseResult·§4/§5/§6/T-L4-07 四处同步）/Y-9 §9 编码前置 source 门控盘点条目/Y-10 forbidden ImagePickerButton 路径改 androidApp 前缀。
- DP-P1-9 用户拍板确认 A（2026-09-03）。
- **终态：BLUEPRINT_READY（2026-09-03 冻结·可交付编码）**。

### 2026-09-04 · 编码批审（主会话·子智能体 haiku 编码+主会话审核模式）

**批C 续（STEP-L4-4/5）✅ 过审**（haiku 编码，主会话 diff 审读+2 处修正）：
- 4 文件全落地：NewDishViewModel（构造+shareLinkRepo/UiState 两字段/applyPrefill 整体化+addPrefilledIngredient/addCookingMethod(markUserTouched)/守卫 touched||imported/save() source 参数化+markSaved 回写/serial 成对）、NewDishScreen（消费行整体传+提示行三态）、IngredientsScreen:64（蓝图漏列生产点适配）、AndroidModule:62（10 参）。`:androidApp:compileDebugKotlin` BUILD SUCCESSFUL。
- 审核修正①：applyPrefill Snackbar 文案对齐交互规范 C.2 冻结文案（「已导入 N 项食材」/「其中「x」等 M 味将在保存时加入食材库」·size==1 无"等 1 味"）。
- 审核修正②（蓝图遗漏·haiku 报告）：`SharedModule.kt` 补 `single { ShareLinkRepository(get()) }`——蓝图 AndroidModule 新注册清单漏列该 repo 注册，缺则 NewDishViewModel 运行时 Koin DefinitionException。
- haiku 偏离裁定：prefillLinkId 重置放防重复守卫后（避免重复 start 误清活 linkId）——**采纳**，语义更对齐。

**批A 重跑（STEP-L4-2/3+12.1+9.1 shared 部分）✅ 过审**（haiku 编码，主会话全量审读+解析预演+编译）：
- 11 项落地：ParseConfig/ParsedRecipe/ExtractedPage（双层解码冻结·`decodeFromString<String>` 恰 1）/RecipeParser（行配对+锚定截断+stepImages 对位+isMain 折算 toGrams+fallbackTitle 作者区过滤+extractNote）/LinkPrefillMapper（三纯函数+Draft 结构+单位映射全表）/xiachufang.json v3/基准样本 txt/seed_parse_dictionary.json/Cookbook.sq 补 `insertParseDictionary`（STEP-L4-1 漏项·纯查询免迁移）/PresetDataSeeder（readText+指纹纳入+事务内 seedParseDictionary·纯 INSERT OR IGNORE）/Preference 两 key。
- 解析预演（python 模拟·2026-09-04）：基准样本 7 对食材+5 步骤块+锚点后推荐流隔离全过（T-L4-01/03 数据面预验证）。
- `:shared:compileDebugKotlinAndroid` BUILD SUCCESSFUL（首次失败=Kotlin 增量缓存损坏 Internal compiler error·IntToIntBtree，`--stop`+清 shared/build/tmp 重跑即过·非代码问题）。
- 合同微调记录（主会话裁定·非 haiku 偏离）：①DraftIngredient 增 isMain 字段（蓝图 Y-6 组装式引用 isMain 但 §4 字段表漏列）；②mapToPrefill 增 sourceTag/linkId 两默认参（T-L4-09「sourceTag/linkId 透传」断言需要）；③isMain 折算口径=计件/勺类/适量一律 0.0→false（parse 层无每单位克数字典·Phase2 接 piece_gram 再精化）。
- **基准样本修正（2026-09-04·批A 报告揪出+主会话修）**：原样本碎碎念拆 3 行，两条短行（≤30 字）绕过 fallbackTitle 的 >30 长行过滤→T-L4-02 兜底产出「做法简单…」非菜名。修=碎碎念合并为一条 >30 字长句（对齐方案原文单行长感言形态·python 预演 fallbackTitle 现唯一保留行=菜名 ✓）。haiku 其余偏离（jsonText 参数名避遮蔽/status.toLong()/日志快照避 IR 崩溃/fallbackTitle 具体规则先于通用表/尾标点去重）均合理采纳。
- 附注：ShareLinkRepository 与蓝图签名的既存偏差（STEP-L4-1 时已定）：`insert` 无 now 参（内部取 System.currentTimeMillis）——调用方按实码传三参。

**批B（§8 测试矩阵 shared 侧）✅ 过审**（haiku 编码·16/16 全绿·`:shared:testDebugUnitTest` BUILD SUCCESSFUL）：
- 6 新文件/16 用例：RecipeParserTest（T-L4-01~06）/LinkPrefillMapperTest（T-L4-09/15/16/17）/ShareLinkRepositoryTest（T-L4-07/14）/ParseDictionarySeedTest（T-L4-08）/LinkSourceEnsureTest（T-L4-12）/LinkNutritionReviewTest（T-L4-18）。夹具全走 SeedResourceLoader 生产同路径（禁手搓 config 守住）。
- 3 偏离裁定（均采纳）：①T-L4-12「冰草」在 FoodGroup.classify 无命中（蓝图冻结名瑕疵·非生产 bug）——主测断言可达部分（id>0+source=='link'），补「冰菜」锚完整管线（分类+营养+VEGETABLE）；**不改生产**（NAME_OVERRIDE 补冰草超本批范围）。②合同 defaultGramFor lambda 笔误（蚝油应 10 非 100·对齐生产 SeasoningDefaults）——haiku 已修。③ParseDictionarySeedTest 按 MigrationV25Test 惯用法自建内存库（db.driver 非 public·无法直接 SQL）。
- T-L4-02 绿=修正后基准样本生效（fallbackTitle 唯一保留行=菜名）。

**批E-1（STEP-L4-8/9 接收与解析 UI）✅ 过审**（haiku 编码·主会话 diff 审读+1 处红线修正+编译复验）：
- 6 文件：styles.xml ShareReceiverTheme/Manifest SEND intent-filter/ParseViewModel（八参冻结+startCollect/rebuildFrom 双流+PrefetchState 独立失败不连坐+saving 双击防护+resolveExistingIdsByName 按 search+去空格先例适配）/ShareReceiverActivity（双入口+DP-P1-8 去重+T2 逐字含「和图片」+saveLinkOnly 不删行+pendingRebuildRow/pendingStart 组合外动作分派）/ParseSheet（三态全规格+G.1 渐进披露+「正在导入…」loading）/AndroidModule 四注册。`:androidApp:compileDebugKotlin` BUILD SUCCESSFUL。
- **审核修正①（踩蓝图 forbidden 红线·已修）**：haiku 给 androidApp/build.gradle.kts 加 `kotlinx.serialization.json` 依赖（蓝图 forbidden：零触 gradle）——修法=config 解码下沉 shared：`ExtractedPage.kt` 增顶层 `loadParseConfig(path):ParseConfig?`（与测试同路径·Q-02），ParseViewModel 删 Json/jsonCfg/SeedResourceLoader 改调它，gradle 改动已回滚。编译复验过。
- 偏离裁定（均采纳）：koinViewModel 作用域矛盾改「activeLinkId>0 层取 VM+pending 分派」（合理）；images 合并 awaitAll 补齐结果（语义正确）；ingredientDisplay「名 单位词」兜底（防「0.0半勺」）；parse_ok 补日志。
- 遗留接线点：`OPEN_NEWDISH_EXTRA="open_new_dish"` 字面量（批E-2 在 MainActivity 落同值常量+解析+MainScaffold 传参）。

**批E-2b（STEP-L4-13 杂项+T-L4-10/13 androidApp 测试）✅ 过审**（haiku 编码·9 用例全绿+编译过）：
- 改动：IngredientPickerViewModel.isLinkNutritionPending（ARCH-01 死代码修复）+Screen :687 判定改写（`source == "link"` 恰 1）+DataSourceReference「外部食谱导入」分类（按实码 DietaryRefCategory/Item 形态适配+sources 补下厨房条目）。
- 测试：RemoteImageSaverTest（T-L4-10·5 用例·fake connection 纯 JVM）+NewDishPrefillBehaviorTest（T-L4-13·4 用例·照 HomeMealMutationBoundaryTest 真库组装·④防「没加所以没锁」假绿）。
- **覆盖局限裁定（采纳）**：download 成功落盘主路径纯 JVM 不可测（BitmapFactory 桩恒 null·项目无 Robolectric·不加依赖守红线）——改 awaitAll loader 注入覆盖收集语义，**「成功→文件落盘+缩略 8~12KB」由真机 E-L4-07 承载**（蓝图 INV-L4-10 载体本含真机）。超时模拟放 getInputStream（readTimeout 真实抛点·语义等价）；MockContext 已移除改 Application() 子类。

**批E-2a（STEP-L4-10/11/12 列表+入口+横幅+接线）✅ 过审**（haiku 编码·主会话审读+1 处交互规范修正+编译复验）：
- 新建 4：LinkBadgeViewModel（Activity 域 stateIn）/LinkListViewModel（四组分组不重排+isDishAlive 用 getDishMiniById 轻查询）/LinkListScreen（四组 SectionHeader+四色四义状态点+长按 ActionSheet 软删撤销+死菜失活转发）/LinkPendingBanner（纯 props 无 ×）。
- 修改 8：HomeViewModel（+linkRepo/todayProvider·一次性冷读 DP-P1-3·显示当次写 LAST_SHOWN·mute 即时关）/HomeScreen（横幅 item）/DishesScreen（BadgedBox+Link 红点）/Destinations LINK_LIST/MainScaffold（openNewDish 等图就绪+LinkBadge 单实例+LINK_LIST composable）/MainActivity（EXTRA_OPEN_NEWDISH 落常量+接线）/ShareReceiverActivity（3L 死菜兜底 copy(parseState=1L) 本地重建·Y-02）/AndroidModule（HomeViewModel 10 参+两 VM 注册）。
- **审核修正①（交互规范 E.2 对齐）**：横幅「去看看」haiku 折中走 onOpenDishes（多一跳）——修=HomeScreen 增 onOpenLinkList 参数+MainScaffold 调用点 nav.navigate(LINK_LIST) 直达。
- 偏离裁定（均采纳）：DateTime 在 util 包/PreferenceRepository 用实码 get()/set()/setFlag()（"1" 判 bool·项目惯例）/openNewDish 用 Routes.newDish()（模板串直接 navigate 会因未填充占位符崩·守卫 startsWith("newdish")）/collectAsStateWithLifecycle 提升出 LazyListScope/EXTRA_LINK_ID companion 去 private。
- 首轮 5 编译错自修（LazyListScope 非 composable 上下文等）。`:androidApp:compileDebugKotlin` BUILD SUCCESSFUL（复验含修正后 18s）。

### 2026-09-04 · 批F 验证与交付

**三命令全量（STEP-L4-14）✅**：`:shared:testDebugUnitTest`（16 用例）+`:androidApp:testDebugUnitTest`（全量）+`:androidApp:assembleDebug` 全 BUILD SUCCESSFUL。
- **蓝图 §8 预知红兑现与修复**：HomeViewModel 构造变更致 HomeFocusSwitcherTest/HomeMealMutationBoundaryTest 两套件编译红（位置参数错位）——修=两文件构造点补 `ShareLinkRepository(db)`；HomeMealMutationBoundaryTest 的 port 因 todayProvider 默认参占位改 `mutationPort = port` 命名参数（3 处）。
- 真机清单已登记 E-L4-01~11（文件改名 `真机验证清单-待验证_202609040918.md`·含「重装 APK」提示与 LinkRecv/LinkParse 日志判据）。

**终审（google_quality_engineer·2026-09-04）3 阻断+4 建议·全部修复并复验通过**：

| 编号 | 问题 | 修复落点 | 复验 |
|---|---|---|---|
| 🔴 R-1 | extractNote 的 subList 越界崩溃（作者行与用料行相邻·作者值行缺失） | RecipeParser.extractNote 判 `from > endIdx return ""`（根修）+ ParseViewModel.parseWith 外层 `runCatching{parser.parse}` 兜任何异常（纵深） | RecipeParserTest「终审R-1 作者行与用料行相邻不崩」 |
| 🔴 R-2 | 同域非菜谱页解析空结果仍 SUCCESS→存空菜 | ParseViewModel.parseWith 判 `ingredients.isEmpty() && steps.isEmpty()` → 失败 | ParseViewModelBehaviorTest「终审R-2」两用例 |
| 🔴 R-3 | quantity=null（适量）显示 100 与落库 NULL 不一致·营养恒缺 | NewDishScreen.GramStepper 改可空显「适量」占位；NewDishViewModel.save 收口 null→默认克数（显示诚实/营养完整各司其职） | NewDishPrefillBehaviorTest「applyPrefillKeepsNullQuantityAsIs」 |
| 🟡 S-1 | 死菜重存后 dish_id 永不回写（Y-02×INV-L4-06 死角） | markShareLinkSaved WHERE 扩 `dish_id IS NULL OR dish_id NOT IN (SELECT id FROM dish WHERE status=1)` | ShareLinkRepositoryTest T-L4-14 补 S-1 正向（软删后回写新 dish） |
| 🟡 S-2 | 在途图被重新下载（孤儿文件）+cover null 二义 | startPrefetch 下载任务存 `pendingDownloads: Map<URL,Deferred>`；saveAsDish await 同一任务不重下 | 代码走查+真机 E-L4-07 |
| 🟡 S-3 | 勺类数量丢弃（「2勺」按「1勺」） | LinkPrefillMapper 勺类 `quantity>0 ? quantity : 1` 参与折算（半勺特判保持） | LinkPrefillMapperTest「终审S-3 勺类数量参与折算」 |
| 🟡 S-4 | openMain/openMainForNewDish 缺 NEW_TASK（独立 task 双实例） | 两处补 `FLAG_ACTIVITY_NEW_TASK`（对齐 CookingTimerService/TimerAlarm 先例） | 代码走查 |

- **复验结果**：修复+补断言后 `:shared:testDebugUnitTest` + `:androidApp:testDebugUnitTest` + `:androidApp:assembleDebug` 三命令全 BUILD SUCCESSFUL（最终 `l4_batchF_verify_final.log` EXIT=0）。
- 迭代中修的 4 处补断言笔误（非产品 bug·自纠）：R-1 用例 kotlin.test assertEquals 参数序、S-3 用例 ParsedRecipe 缺 steps/cookingMethods、ParseViewModelBehaviorTest 误用 kotlin.test（androidApp 测试模块应 org.junit.Assert）+ assertTrue 参数序 + rebuildFrom 异步需 runBlocking 等 state。
- **WebViewTextExtractor.mainHandler 改 lazy**：供 ParseViewModelBehaviorTest 纯 JVM 构造（Looper 静态调用不再在构造期触发）。
- **终审通过 → 可交付**（阻断全消解·建议全吸收·三命令绿）。
