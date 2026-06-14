# 端侧 AI 模型与场景方案结果

- 时间：2026-06-12
- 用户需求：结合 `temp/端侧模型加载.md`、`temp/端侧模型的使用场景.md` 和当前菜单 App，分析端侧 AI 选型与可覆盖场景，输出近期接入方案。
- 产出文件：`.codex/docs/feature/端侧AI能力接入方案.md`。
- 核心结论：Android 近期推荐 `LiteRT-LM + 小参数中文指令模型`，以 `Qwen2.5-0.5B-Instruct` 做 POC；若转换/性能不稳定，则切换 Google 官方 LiteRT-LM/Gemma 生态模型跑通工程闭环。
- 核心结论：Gemini Nano/AICore 适合作为支持设备上的优先通道，不适合作为唯一依赖；iOS 后续单独评估 Apple Foundation Models 或 LiteRT-LM Swift。
- 产品场景优先级：先做 AI 快速记一餐、AI 新建菜品草案、AI 食材风险解释；再做下一餐建议、一周菜单、饮食周报；拍照识别/OCR/复杂处方后置。
- 架构原则：模型只做理解、生成草案和解释；数据库查询、保存、慢病规则、营养阈值由 App 代码和 Repository/UseCase 执行。
- 安全边界：禁止模型直接执行任意 SQL；所有写入必须用户确认；慢病建议不能表达为医疗诊断或处方。
- 验证状态：本轮为 Research/方案任务，未改业务代码，未运行构建测试。
