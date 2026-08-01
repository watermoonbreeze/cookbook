# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-08-01 · 分享链接解析方案会商 + L1-L4 待办登记**

## 本 session 交付

- ✅ **L1-L4 四项待办登记**：合规性（免责+AI弹窗）/ 脂肪肝App入口 / 全App自动化进阶 / 分享链接解析
- ✅ **L4 六角色会商**（Apple-UX/产品/算法/架构/行为师/文案）→ 综合方案文档落地
- ✅ **真实样本验证**：下厨房 `m.xiachufang.com/recipe/106889995/`（冬瓜丸子汤·innerText结构清晰·正则可解析）
- ✅ **方案文档** `分享链接解析_方案设计.md`（DB设计/解析引擎JSON Schema/交互流程/透明分级/文件清单/Phase分期）

## ⏭ 下一步

**首要**：用户将方案文档交给其他模型评估，评估后可能调整方案细节。

**方案确认后**：按 Phase1 编码（下厨房单源+预设解析+链接列表+存为菜品）：
1. DB：33.sqm 迁移 → Cookbook.sq 加两表+查询 → ShareLinkRepository
2. 解析引擎：`ParsedRecipe.kt` + `ParseConfig.kt` + `RecipeParser.kt`（shared·纯Kotlin正则·带单测）+ `xiachufang.json` 规则文件
3. Android：`ShareReceiverActivity`(Manifest intent-filter) → `ParseSheet`(三态) → `LinkListScreen` → 横幅+红点
4. 集成：存为菜品预填(复用 NewDishPrefillBus) → 数据来源页扩展

**其他待办**：L1(合规)·L2(脂肪肝)·L3(自动化方案) 各单开 session 做。

## 设计决策（已确立·后续沿用）

1. **BottomSheet 替代 WebView 全页**：后台隐藏 WebView 加载+JS提取文本 → 用户只看到 Sheet 进度/结果
2. **纯正则解析**：对 `innerText` 做正则提取（不用 CSS 选择器），JSON 预留 CSS 字段供 Phase2
3. **图片下载绑定保存操作**：预览用远程URL → 用户点"存为菜品"时才下载本地
4. **WebView 隐私 T2 弹框**：加载前告知"对方服务器会看到本次访问"
5. **未解析提醒=首页横幅（非弹框）**：复用 §9.31 范式 + 链接图标红点
6. **AI 解析=Phase2**：MVP 仅预设解析；AI 上线前必须先完成 L1 合规闸门

## 先读清单

1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线）
3. `.ai-context/docs/feature/分享链接解析_方案设计.md` ← **本次核心产出**
4. `.ai-context/docs/feature/待办总览.md` §L 段（L1-L4 四项）
5. `.ai-context/docs/功能路径索引.md`（定位先查）

## 工作规则（延续）

1. 🔴 权威方法论优先 · 数据来源真实 · 营养免责非医嘱
2. 🔴 一个 session 聚焦一个内聚任务 · off-type 进待办
3. 🔴 定位先查功能路径索引 · 增删改名文件同 commit 同步索引
4. 🔴 每功能/bug修复必登记真机待验证清单
5. 🔴 色系墙只看膳食结构 · 热量数字默认开可关 · 健康文案守免责
