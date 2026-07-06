# 结论：启动 seed 性能优化 + 更新基础数据入口

时间：2026-07-06。[AI生成] 提交：4d00813。

## 问题根因

每次启动 `PresetDataSeeder.seedIfNeeded()` 无条件全量补齐：153 食材 + 163 调养规则 + 44 详情 + 全部分类关系，
用 `INSERT OR REPLACE` 覆写，且**整个 seed 无事务包裹**——SQLite autocommit 下每条写各自 fsync，
数百上千次落盘（数据库在 /sdcard 外部存储更慢）。阻塞式初始化直接表现为启动"初始化数据中"卡顿。
数据从 87→153、规则 15→163 后被放大。

## 优化（方案A）

- **内容指纹守卫**：5 个 seed JSON 各自"长度:hashCode"组合成指纹，存 `PreferenceKeys.SEED_CONTENT_FINGERPRINT`。
  启动比对：相同→整段跳过（零写入）；不同→重跑。日常启动 seed 阶段 ≈ 读5文件+算hash 几十毫秒。
- **单事务**：需要写时把分类/食材/详情/调养全套包进一个 `db.transaction`，上千次 fsync 降为一次提交。
- **旧库 NULL 清洗一次性**：`SEED_LEGACY_SANITIZED` 标记守卫，不再每次全表 UPDATE。
- **seedUserPreferences 守卫改按 key**：user_preferences 现在混入 seed 元数据 key，不能再用 `count==0`，
  改为判断 `THEME_MODE` 是否存在，否则用户默认偏好会被漏写（自己引入又修正的坑）。
- 不做记录级 diff：INSERT OR REPLACE 在单事务内幂等，全量 vs 增量体感无差；守住"没变不写"即解决痛点。

## 新功能：我的-数据-更新基础数据

- `PresetDataSeeder.forceReseedBaseData()`：忽略指纹强制重跑（`reseedContentIfChanged(force=true)`），
  **强制刷新语义、非删除重建**——只幂等 upsert，不删任何行。用户自建食材、对预设食材的名称/图片修改、
  菜品对预设食材引用关系均不受影响（避免 ON DELETE CASCADE 误删用户数据）。
- MineViewModel.updateBaseData(成功,是否变化) + updatingBaseData loading 态；MineScreen 用 `CloudSync` 图标行 + Toast。
- **预留扩展点**：未来把数据源从内置 assets 换成"后台拉取的 JSON 数据包"，逻辑不变，即可实现"后台加预设→前端手动拉取"。

## 关键实现注意

- SQLDelight 生成的 user_preferences 列 `value` → Kotlin 属性名 `value_`（value 是关键字），读取用 `.value_`。
- 指纹用 Kotlin String.hashCode（跨平台稳定）；最坏漏更新由手动"更新基础数据"兜底。

## 验证

- PresetDataSeederTest 6 个全过（新增：指纹+用户默认偏好写入、强制刷新幂等且保留用户自建食材）。
- :androidApp:assembleDebug BUILD SUCCESSFUL。

## 建议用户实测

- 首次装新版仍会 seed 一次（写指纹）；第二次起启动应明显变快。
- 若之前已装旧版：升级后第一次启动因指纹不存在会重跑一次（此时顺带补上事务，也比旧版快），之后跳过。
