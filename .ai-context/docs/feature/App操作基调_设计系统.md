# App 操作基调 · 设计系统（编辑/选择页家族统一规范）

> [AI生成] 2026-07-20 用户确立"定操作基调·以后所有界面按此走·让 app 有自己统一的操作逻辑"。
> 由 5 位设计工程师会诊综合（菜品编辑 Apple-UX / 食材编辑 Apple-UX / 视觉设计师 / 质量工程师；家族统一 Lead 因会话额度未跑，由主 agent 综合）。
> **这是全 App 界面的操作基调**：新页面/改页面一律向本规范对齐，是 §九交互模式库的"系统级"总纲。分批向全 App 推广（本文§六）。

## 一、操作基调八条（同类动作·全 App 同一套）

1. **保存/主 CTA 永远在底部**：`FormBottomBar` 胶囊常驻底部、合拇指热区、一屏一个；顶栏右上不放保存（§9.13/§9.30）。必填不满足→置灰（最轻反馈·不弹错不加红字）。次 CTA"保存并继续"同位。
2. **选择永远同一套**：勾选圈 + 底部"已选 N 项"栏（点击上拉展开已选清单·×就地移除）+ 搜索 + 结果末尾"新建"入口。菜品选择↔食材选择用**同一个 `SelectionSummaryBar`**（F#3·中性 `SelectionItem` 模型·回调决定显隐）。
   > **已选栏落地（P3·`ui/component/SelectionSummaryBar.kt`·已实现）**：摘要行(已选 N 项 + 上拉 chevron + 次操作插槽 + 主 CTA 胶囊) + 上拉展开清单(每项前导 emoji/缩略图 + 主/副文本 + 标签 + 就地 × 移除 16dp 中性色)。三态同一组件、参数区分无 mode 布尔：菜品"完成"、食材"完成"、食材"组成菜品"(secondaryText="取消")。中性 `SelectionItem(id,title,subtitle,badges,emoji,thumbnail)`——菜品 badges=tags/thumbnail=封面，食材 subtitle=预设/家庭·emoji。**移除默认直接**(非撤销·选择集内取消勾选)；空态 `alwaysShowWhenEmpty` 控制显隐(菜品/组成菜品传 true 保 CTA 可见·食材选食材未选即隐)；全屏 Dialog 载体 `navBarPadding=true` 只在摘要行消费一次。菜品页顶部横滑条已下沉底部统一。
3. **返回永远有未保存守卫**：`rememberUnsavedGuard(isDirty, onConfirmLeave)`（§9.17 非包裹式）；顶栏返回 + 系统 Back 统一走；脏表单→"放弃更改？"(放弃=红字)。
4. **反馈永远走统一 Snackbar**：保存成功/失败、新建、可撤销删除都走 `LocalAppSnackbar`（§9.12）；可逆破坏操作=软删+撤销、不硬确认弹框。Toast 仅纯告知兜底。
5. **高频直出 / 低频折叠**（§9.31 新范式）：必填+高频核心字段直出主区（一屏一焦点）；中低频/装饰/进阶字段收进 `MoreOptionsHeader` 折叠区（默认收起·有内容自动展开）；字段多但属"一条连贯录入流"用此分层，别拆 §9.24 分段。
6. **必填最小化 + 智能默认**：每个表单唯一真必填尽量压到 1 个（菜名/食材名）；其余给智能默认（营养大类按名 classify 预选、单位按大类默认、餐次按菜名 Matcher 预选、克数默认 100g/调料缩小）——让用户少填少选。
7. **分区容器统一 `InsetGroup` 白卡**（§9.3/9.24）：语义分区包白卡、页面底 `background` 灰、卡 `surface`，层次分明；InsetGroup 自带 16dp 内距、外层别叠 padding（防双重坑）。
8. **顶栏统一**：Tab 落地页大标题（`LargeTopAppBar`）、带返回二级页 `AppTopBar`；系统栏色跟随界面背景。

## 二、视觉基调（全 App 走 token·不硬编码色值）

- **字阶**：页面标题 titleLarge22 / **分区标题 titleSmall 17 SemiBold onSurface**（强调色 primary **只给可交互元素**、不染静态标题·这是拉齐一致性的核心）/ 字段值 bodyLarge17 / 副文本 bodyMedium15 onSurfaceVariant / 注脚 labelSmall12。
- **间距 8pt 网格**：屏边距 16 / 块间 24 / 分区标题→内容 8 / 卡内 16 / 列表行 v12(触达≥48) / chip 间 8 / 同区字段间 12 / 图标↔文字 4。
- **图标**：全 `Icons.Outlined.*`；尺寸档：行内删除/× **16dp** / 文字按钮前导 18dp / 步进箭头 20dp / 顶栏 24dp。删除图标中性 onSurfaceVariant（不处处染红·仅破坏性确认弹框文字用 error）。
- **必填 `*` 用 error 色**（AnnotatedString·全 App 一致）。
- **字段标签统一"框内浮动 label"**（`OutlinedTextField(label=)`·聚焦 primary 描边），淘汰"框上 FormFieldLabel + 无 label"的另一套。
- **chip 三态语义分工**（不混用）：增删型 `AssistChip`(带×/+添加) / 勾选多选 `ToggleChip`(实心=选) / 单选固定项 `FilterChip`(✓)；统一高 32dp、圆角胶囊 16dp。
- **圆角**：输入/卡 medium12 / 大卡弹层 large16 / 小徽标 extraSmall6。
- **深色**：全 token 自动适配；仅 3 处硬编码半透明 α 深色下需调高（预填态描边 0.3→0.45、Banner 底 0.06→0.14、care 行底）。

## 三、菜品编辑页落地方案（首个基调参考实现）
从上到下：**封面引导卡(高频①·无图120dp虚线卡/有图160dp通栏·ActionSheet拍照相册·选填不入校验) → 菜名*(高频②) → 食材 InsetGroup(高频③·核心·MiniStepper克数+待自建标+配料组入口) → 适合餐次(高频④·智能预选) → MoreOptionsHeader 低频区(步骤/烹饪方式/标签/特殊说明/描述)** → 底部 FormBottomBar 保存。
关键：把"步骤/烹饪方式/标签"从主区**下沉低频区**→第一屏只剩封面+菜名+食材+餐次四件事；保存 CTA 顶栏→底部；hasMore 去掉 imagePath 条件；封面用新 `DishCoverPicker`(复用 ImagePickerButton 管线)。**§9.30 P2 高风险·需真机全回归 新建/编辑/导入/回传 savedDishId 四链路**。

## 四、食材编辑页落地方案
三层：**基础卡(名称*+营养大类chip智能预选[去"必选"红字·唯一真必填=名称]+单位[按大类默认]+单件克重[仅计件单位条件显·从营养区上移]) → 折叠段①营养数值(预填可核对·不再强制展开·提示条给"展开核对") → 折叠段②更多信息(别名/图片/其它分类·从基础卡下沉) → 折叠段③做法说明 → 折叠段④调养建议** → 底部保存/再记一个。
关键：`moreExpanded` 单开关拆 4 个独立折叠段(按频率排序)；`EditorSection`→`InsetGroup`、内联 TopAppBar→`AppTopBar`、自绘守卫→`rememberUnsavedGuard`；去营养大类必选。**P2 载体升级**：全屏 Dialog→路由页(接统一 Snackbar·现被迫用 Toast)。

## 五、质量走查·必修项（质量工程师·两页通用）
**阻断(必修复验)**：①食材编辑保存失败错误提示在长表单最底看不见→用户以为存上其实没存(改顶部固定错误条/滚动到它) ②食材编辑保存成功无反馈(对齐菜品页·统一 Snackbar) ③进程被杀表单全丢无草稿(至少关键字段 SavedStateHandle 持久化·或显式记为已知降级) ④克数输入框 remember 无 key(改 remember(grams))+空串兜底不跟手 ⑤图片压缩期能点保存致空图路径存库(ImagePickerButton 上报 processing·压缩中禁保存)。
**建议**：营养小数字段用 KeyboardType.Decimal(Number 键盘无小数点)；名称类字段设 maxLength；配料组批量建食材按实际成功数反馈；删除高价值项给撤销(对齐 §9.12)；长表单 imePadding+bringIntoView 防键盘遮挡。

## 六、分批推广计划（全 App 家族化·风险控制）
1. **P0 沉淀基调**（本文·done）+ 抽/确认共享件（`SelectionSummaryBar`/`DishCoverPicker`/`rememberUnsavedGuard`/`InsetGroup`/`FormBottomBar` 已多在）。
2. **P1 首个参考实现=菜品编辑页**（含 F#2 拍照·§9.31 分层）+ 质量阻断项修复→真机全回归四链路。
3. **P2 食材编辑页**（分层拆折叠段+视觉统一·载体升级独立轮）。
4. **P3 两选择页统一**（`SelectionSummaryBar`·F#3）✅ 已实现(设计门禁过·两页迁移·就地×移除治食材页3步移除缺陷) + 餐食编辑页对齐(保存 CTA→FormBottomBar·待做)。
5. **P4 全 App 其余页**按基调逐批对齐（视觉字阶/间距/图标/顶栏/底部 CTA/守卫/反馈），每批真机验证。
> 每批过 Apple-UX(已出稿)→编码→Google 代码质量审→装机真机验。基调新范式沉淀回 `苹果风格UI设计方案.md §9.31` 及本文。
