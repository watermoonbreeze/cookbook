# AI快捷记餐 K1 入库 Bug 根因分析（新方案避坑参考）

> 2026-07-29 排查记录。问题：语音按钮无反应、发送入库无反应。
> 语音已修复（集成了 SpeechRecognizer）。入库问题在新方案中会被替代，此文档记录根因供新方案设计时避免。

## 语音按钮"无反应"

**根因**：`VoiceInputSection` 只有 UI 状态切换（`voiceActive: Boolean`），**没有真实的 SpeechRecognizer 集成**。代码中有 TODO 注释标记。

**影响**：用户点击/长按语音按钮无任何实际语音识别行为。

**已修复**（K2）：集成 `VoiceRecognizer` 封装 + 长按手势 + RECORD_AUDIO 权限 + 波形动画。

---

## 发送入库"无反应" — 多条 Failure Path

### Path 1：新用户无 API Key → localFallback 覆盖严重不足（**最常见**）

```
用户输入 → submit() → isModelReady()=false（无 Key）
→ 走 localFallback() → 仅处理单餐短句 → 输出空/错误
→ ErrorPhase "没能识别出菜品"
```

**根因**：
- 默认 `AiRuntimeType.from(null)` 返回 `CLOUD`，但 `isModelReady()` 检查 `currentCloudApiKey().isNotBlank()`
- 新装用户未在 AI 设置页配 Key → `isModelReady` 恒 false → 永远走 localFallback
- localFallback 只支持**单餐单句**（一个 meal_type + 几道菜的简单拆分）
- 用户输入稍复杂（多餐次/带备注/非典型格式）→ 解析失败

### Path 2：多餐次/多天输入不支持

```
用户输入"早饭吃了鸡蛋，午饭吃了面条" 
→ localFallback 只检测**第一个** meal_type（早饭→breakfast）
→ 后面的"午饭"被当做菜品文本处理
→ 面条→breakfast 的一道菜（餐次错误）
```

**根因**：`localFallback()` 设计为每次只解析**一个餐次**。不支持按餐次关键词分段再分别解析。

### Path 3：备注文本被误当菜品

```
用户输入"中午吃了红烧肉、米饭，少放盐"
→ splitDishes 按 [、，,+] 硬分隔 → ["红烧肉", "米饭", "少放盐"]
→ "少放盐" 通过 couldBeDishName（≥2字，含中文）→ 被当做一道菜
→ 预览显示 "少放盐 ×1份"
```

**根因**：`extractMealNote()` 只从原文匹配备注关键词但**不移除**已匹配的备注文本，导致 `splitDishes` 仍能从中切出"菜"。

### Path 4：AI 解析返回后 JSON 提取失败

```
AI 返回 ```json { ... } ``` → extractJson 剥离 markdown → 正常
AI 返回 "好的，这是您的..." 开头 + JSON → extractJson 首{到尾}截取 → 可能漏截
AI 返回非法 JSON（缺少引号/多余逗号/嵌套错误）→ decode 失败 → null
```

**根因**：`extractJson` 的兜底策略（首{到尾}）对 AI 输出"JSON前有自然语言后也有"的场景脆弱。

### Path 5：AI 超时/网络故障

```
parseWithAi() → aiRuntime.complete() → CloudAiRuntime
→ HTTP 连接超时 15s + 读取超时 45s + 重试 2 次
→ 最坏 ~120s 用户等待 → 最终 Result.failure → 回退 localFallback
```

**根因**：当前无用户可见的超时提示，用户可能以为 App 卡死了。

---

## 新方案设计中的避坑要点

| # | 坑 | 新方案应对 |
|---|-----|----------|
| 1 | 无 Key 用户永远走 localFallback | **规则引擎独立增强**为可靠兜底（多餐次分段+菜品+食材），不依赖 AI |
| 2 | 多餐次/多天不支持 | `TextSegmenter` 按星期+餐次关键词切分→`RuleMealParser` 逐块独立解析 |
| 3 | 备注误当菜品 | 备注提取+移除两步分开：先提取备注→**从文本中删除**→再拆菜品 |
| 4 | AI JSON 提取不稳定 | 增强 `extractJson` + Prompt 强化"仅JSON"约束 + 重试一次 |
| 5 | 无超时提示 | 解析中动画 + 超时自动降级 + "正在用本地规则重试"提示 |
| 6 | ViewModel Koin 注入 | 所有依赖已在 sharedModule/androidModule 注册，确认可解析 |
