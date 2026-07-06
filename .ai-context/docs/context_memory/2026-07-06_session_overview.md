# 会话总览：2026-07-06（Claude 主力）

[AI生成] 本日完成的工作与状态总览，细节见各 result 文件。

## 完成的任务链（均已提交，本次统一 push）

1. **双模式公共化** → `.ai-context/` 成为 Claude/Codex 唯一公共源（rules/hooks/docs），CLAUDE.md 与 AGENTS.md 引用它。见 `2026-07-03_ai_context_merge_and_ingredient_fixes_result.md`。
2. **CLI 双轨构建** → `scripts/build-cli.bat/.sh`（显式 JDK17，AS Hedgehog 打不开项目，CLI 为标准构建路径）。`scripts/README.md`。
3. **食材调养体系三批次**（A UI / B 调养模型统一 DB v10 / C 数据补充 153食材+163规则+44详情）。见 `2026-07-03_ingredient_care_batches_result.md` + `食材数据规范.md`。
4. **启动性能优化** → seed 内容指纹守卫 + 单事务，消除每次启动全量重写；新增"我的-更新基础数据"入口。见 `2026-07-06_seed_startup_perf_and_update_base_data.md`。
5. **食材失效状态(status/reason)** → 失效食材菜品引用不断裂+灰显、回收站恢复/彻底删除。见 `2026-07-06_ingredient_inactive_status_reason_result.md`。
6. **修复 reason 列缺失**（上个功能漏了迁移文件，旧库升级缺列）→ 驱动幂等补列。见 `2026-07-06_fix_missing_reason_column.md`。
7. 删除食材确认文案改为软失效语义（不再"移除"）。

## 关键技术约定（本日形成，已沉淀到 experience）

- 构建：`scripts\build-cli.bat <task>`（JDK17）；单测走 `Schema.create` 不覆盖迁移路径。
- SQLDelight：`user_preferences.value` 列 Kotlin 属性名是 `value_`；改 `.sq` 表结构**必须同步加 `.sqm` 迁移**，否则旧库升级缺列（本日踩坑，见 06_问题与踩坑）。
- 预设数据：只维护 `.ai-context/docs/feature/食材数据规范.md` 定义的 seed JSON；后台下架预设食材=JSON 里 status:0+reason。

## 待用户验证

- report「筛选自定义食材失败」：reason 列修复后重测；若仍失败需 `/sdcard/cookbook/log/` 日志（自定义分类筛选查询本身不读 reason，可能另有原因）。
- 升级安装无需清数据，驱动会自动补 reason 列；第二次启动起 seed 明显变快。
