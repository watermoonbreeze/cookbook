# AI-交接文档（新 AI / Codex 接手总入口）

> [AI生成] 2026-07-26·Claude Code → Codex 交接。**你是接手本项目的 AI，先完整读本文，再按「先读清单」补上下文，即可无缝接续。** 本项目为 **Claude Code / Codex 双模式**开发，公共规范/文档/记忆统一在 `.ai-context/`，两端共读共写。
> 交接完整性自检见文末 §十（"除了上下文/经验/会话记录，还要做什么"都在那）。

---

## 〇 一句话
**Cookbook = 面向慢病（三高/痛风等）家庭的"每天吃什么"决策 App**，Kotlin Multiplatform（当前只交付 Android·Compose）。核心价值=**家庭多成员 × 慢病忌口 × 生命阶段调养 × 中式家常定量 × 决策闭环**，五维交集竞品全空（详见 `feature/核心竞争价值.md`）。MVP 三大核心（记一餐/历史食历/复用菜单）已完成，现处**功能扩展与权威化打磨**阶段。

## 一 先读清单（按序·补齐上下文）
1. **`CLAUDE.md`（项目根）** —— 所有强制规范：双模式说明、会话交接协议、体验/透明/文案/架构准则、**权威方法论优先准则**、**踩坑红线（必避·几十条一行命令式）**、技术栈、构建命令。**最高优先，先读透。**
2. **`.ai-context/docs/功能路径索引.md`** —— feature→文件路径（AI-terse）。**定位需求先查它、别 grep 重找**（省 token·维护触发见全局规范）。
3. **`.ai-context/docs/context_memory/SESSION_交接.md`** —— 最新一次会话交接（当前进行中任务状态 + ⏭下一步）。
4. **`.ai-context/docs/feature/待办索引.md`** —— 待办全局入口（按优先级+分类速览→链到专项文件）。旧 `待办总览.md` 已拆分为索引+6专项（Bug修复/功能算法/UI交互/数据健康/工程合规/战略会商）。
5. **`.ai-context/docs/experience/INDEX.md`** —— 经验手册索引（工程规范/问题踩坑）。
6. **战略三件套**：`feature/核心竞争价值.md`（定位真相源）、`feature/全方位竞品对比_侧重亮点深挖.md`（三视角会诊·该做/该补/该弃）、`feature/竞品对比与商业化规划.md`（完整竞品+商业化+合规）。
7. **权威化主线**：`feature/功能总线_权威方法论对照.md`（自创 vs 权威·三自创点已全权威化）。
8. 需要项目全貌反查：`.ai-context/docs/projectReview/00_导读与索引.md`（多分册说明书）。

## 二 定位与核心竞争价值（内部精准·对外温和不标病名）
- 详见 `feature/核心竞争价值.md`。3 大核心：①家庭多成员管理·餐食分摊到人 ②健康状态+生命阶段调养关联食材与推荐 ③健康营养贯穿全应用。4 enabler（缺一不可）：食材营养数据全面·算法贴合权威·数据来源真实·基础功能高质量好用。
- **对外文案红线**：不显性标"三高/痛风"，用"健康管理/家人饮食需要"（`竞品 doc §2.5`）。

## 三 当前进度（2026-07-26）
- ✅ **MVP 三大核心**（记一餐/食历/复用）+ 食材体系/厨房小助手/搜索/健康档案/库存/推荐（库存/随机/周期/一周计划/AI/自由搭配）/今日卡/色系墙/营养线/双设备同步 等已落地。
- ✅ **权威化重审 P0–P3 全收官**（膳食结构对齐《中国居民膳食指南2022》膳食宝塔·`DietaryGuideline` 单一真相源）：P0 地基·P1 色系墙均衡评级·P2 餐次差异化·P3 推荐份量/结构+营养线口径收敛宝塔四层。**三个自创点全部权威化。**
- ✅ **战略层**：竞品全方位对比（PM+运营+架构三视角会诊）已收敛出侧重/亮点/补短板/该弃三张单（见 `全方位竞品对比_侧重亮点深挖.md`）。
- ⏳ **下一步候选**（各单开 session·过门禁）：成员化健康红绿灯（P0 头号）、引擎正向兑现到推荐质量（多成员折算贯穿推荐 + 忌口扩正向"推有利菜"）、生命阶段调养做透一个场景（P1）、餐食系数改占比%呈现、条码扫描、基础顺手度打磨。详见 `待办索引.md`。
- 近期 git：`git log --oneline -20` 看 P3 + 战略 + 索引各 commit（均**未 push**·用户要才 push）。

## 四 架构与技术栈
- **Clean Architecture 简化版**（UI/Domain/Data 三层）。`:shared`（commonMain/androidMain·Domain+Data·纯 Kotlin 领域算法+SQLDelight）；`:androidApp`（Compose UI/VM/Theme/Nav）；`iosApp/`（暂不做）。
- Kotlin 1.9.20 / AGP 8.2.2 / Compose 1.5.4 / **Material3 1.1.2（无 SegmentedButton/HorizontalDivider/SelectableDates·见红线）** / SQLDelight / Koin / Gradle KDSL。包名 `com.sxdbsm.cookbook`。minSdk21 / target34 / JVM1.8。
- **架构准则**（`CLAUDE.md`）：A=UI 层 UDF（MVVM+不可变 UiState 单一真相源）；B=跨平台共享逻辑不共享像素（Domain/Data 下沉 commonMain+单测·UI 各端原生）。反过度设计红线：不为不存在平台预抽象。

## 五 强制规范 / 门禁 / 红线（必守·违则返工）
- 🔴 **踩坑红线**（`CLAUDE.md`·几十条）：SQLDelight 迁移/UPSERT/加列、营养折算防天价、Compose SlotTable early-return 崩溃、改 B 表刷 A 表令牌、seed 指纹、DB 外部目录路径、adb 诊断等——**改代码前扫一遍相关条**。
- 🔴 **门禁**：①界面/交互改动**编码前先 Apple-UX 设计**；②算法/健康改动**过算法+UX 会商 + Google 质量终审（阻断必修）**；③健康数据涉营养/care 必配权威来源+免责、入"数据来源"页。
- 🔴 **权威方法论优先**：功能/算法前先查权威（膳食指南/DRIs/食养指南/国标）有没有现成方案，有就用、别自创（膳食结构走膳食宝塔=`DietaryGuideline`，同"阈值走国标"）。
- 🔴 **一个 session 只做一类**（省 token）：聚焦一个内聚任务（含数据+界面等多面·2026-07-25 修正）；off-type 的**另一个任务**进待办、别当场做。
- 🔴 **透明准则**（分级 T0–T3）+ **文案准则**（清晰>精简>说人话·健康免责非医嘱·惯例非国标）+ **色系墙红线**（只看膳食结构·不关联热量/慢病）。
- 🔴 **AI 注释规范**：AI 生成/改的代码加 `[AI生成]`/`[AI修改]` 标识 + 中文注释。

## 六 构建与测试（显式 JDK17）
```
scripts\build-cli.bat :androidApp:assembleDebug     # 构建 Android(Windows)
scripts\build-cli.bat :shared:testDebugUnitTest     # shared 单测(健康/算法改动必跑)
scripts\build-cli.bat clean
```
macOS/Linux 用 `./scripts/build-cli.sh <任务>`。🔴**构建看输出别信 exit code**（管道 tail 掩盖 gradle 失败·必须 grep `BUILD SUCCESSFUL`）。IDE 打开会报模块实体错误·不作为构建路径。

## 七 会话交接 / 续接协议（跨 session 无缝）
- 用户说「交接/保存session」→ ①落档（context_memory/experience/feature）②**覆盖式更新** `SESSION_交接.md`③git 提交（不自动 push）④在 `SESSION_交接_历史.md` 顶部追加一行索引。
- 用户说「会话继续/查看session继续」→ 先读 `SESSION_交接.md` 按"先读清单"补上下文、按"⏭下一步"接着干。
- `SESSION_交接.md` 只留当前状态（≤1屏·全覆盖）·历史靠 git log + `SESSION_交接_历史.md`。

## 八 待办与下一步
- 全量：`feature/待办索引.md`（全局入口→6专项文件：Bug修复/功能算法/UI交互/数据健康/工程合规/战略会商）。
- 顶部 **2026-07-26 战略三张单**（该 all-in / 该补 / 该弃）是当前方向纲领。
- 每个待办标了归属 session 类型·各单开、过对应门禁。

## 九 配置迁移（Codex 自身能力）
- Claude 的 agents/skills/commands/workflow/rules 已导出到（源机）`~/.claude-config-export-20260726_155826/`，含 `CODEX_MIGRATION_GUIDE.md`（Claude→Codex 概念映射/转换步骤/路径适配/验证/限制）。**在目标机按该指南把配置落到 `~/.codex/`**。
- 关键：CLAUDE.md→AGENTS.md（语义转·设 `project_doc_fallback_filenames=["CLAUDE.md"]` 让 Codex 也读本项目 CLAUDE.md）；agents/skills 直接搬；commands 包装成 skills；hooks→AGENTS.md 文字红线。

## 十 交接完整性自检（除上下文/经验/会话记录外·还要做的都在此）
- [x] **保存上下文**：本文件 + `SESSION_交接.md` + `context_memory/`。
- [x] **总结经验**：`experience/`（索引 INDEX.md）+ CLAUDE.md 踩坑红线（新经验用 `/zongjie` 沉淀）。
- [x] **会话交接记录**：`SESSION_交接.md`（最新态）+ `SESSION_交接_历史.md`（索引）。
- [x] **功能路径索引**：`功能路径索引.md`（定位省 token·有维护触发）。
- [x] **定位与竞品战略**：核心竞争价值 + 全方位竞品对比 + 竞品商业化三文档。
- [x] **规范/门禁/红线**：CLAUDE.md（项目）+ `.ai-context/rules/通用规则.md`。
- [x] **配置导出**：`~/.claude-config-export-20260726_155826/` + Codex 迁移指南。
- [ ] **项目记忆迁移**（可选·Codex 需要才做）：`~/.claude/projects/D--Company-Gitee-cookbook/memory/`（含 MEMORY.md 索引 + 各 project/feedback/user 记忆）——**未随配置包导出**（属运行时数据），要接续记忆则单独拷到 Codex 的 memory 位置。
- [ ] **目标机构建环境**：装 JDK17、配 `org.gradle.java.home`、Android SDK；`gradle/libs.versions.toml` 版本对齐；首次 `scripts/build-cli.* :shared:testDebugUnitTest` 验证绿。
- [ ] **git 状态**：本项目多个 commit 未 push（用户决定是否 push）；`temp/` 未 gitignore→提交显式 add、**绝不 `git add -A`**。
- [ ] **真机验收**：部分改动标"真机待验"（见待办）——Codex 接手后按需 adb 真机验证（`adb_transfer` 技能·华为等无 sqlite3 用 `adb pull` 外部目录）。

---
**一句话给 Codex**：先读 `CLAUDE.md` + 本文 + `功能路径索引.md` + `SESSION_交接.md`，就懂了"是什么/在哪/做到哪/怎么接着做"；动代码守门禁与踩坑红线，定位先查索引，改文件结构同步维护索引，一个 session 只做一类，健康/算法改动过会商。
