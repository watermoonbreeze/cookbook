# AI 设置·云端模型配置指南入口

> 2026-07-16 无人值守落地。经 Apple-UX 设计门禁 + Google 代码审查门禁。

## 一、目标（少操作 · onboarding）
用户在 AI 设置选云端大模型后要填厂商 API Key，但常不知去哪申请。原页面只有静态一句"申请见文档《AI_API_KEY申请指南》"（点不了、打不开）。改成**可操作入口**：点"如何申请密钥"→按当前所选模型的厂商展示申请步骤 + "打开官网申请"一键跳浏览器，免翻文档。

## 二、方案（Apple-UX 设计门禁产出）
- **入口**：CloudSection 里 Key 状态行下方，静态文字拆为「隐私说明（保留）+ 可点链接」。**未配置 Key 时文案升级引导**"不知道去哪申请？查看步骤 →"（§9.6 空态给下一步），已配置时"如何申请密钥？"。primary 色小字链接。
- **容器**：`ModalBottomSheet`（非 AlertDialog/ActionSheet——富内容 + 主 CTA，ActionSheet 只吃动作列表）。内容随**当前所选模型的厂商**动态。
- **内容**：厂商名 +（当前模型 free 则)"免费"小标签 + 有序申请步骤（3-4 步，精简版，末步统一"复制粘贴到上方设置"闭环）+ `CapsuleButton`"打开官网申请"（applyUrl 非空才显）+ "Key 只存本机"隐私说明。verticalScroll 兜底长文。
- **打开官网**：主 CTA = 浏览器 `Intent(ACTION_VIEW)`，`runCatching` 兜底无浏览器。**不做二次确认**（用户主动点、语义清晰，苹果式少一步）。
- **数据**：`CloudModel.applyUrl`(shared 新增字段，data-driven 两端可用) + `VENDOR_KEY_STEPS`(UI 层 vendor→步骤 map)。零新增公共组件（复用 CapsuleButton/ModalBottomSheet）。

## 三、四厂商官网入口（AI 参考整理·随平台可能调整）
| vendor | 厂商 | 免费 | applyUrl |
|---|---|---|---|
| zhipu | 智谱 | GLM-4-Flash 免费 | open.bigmodel.cn/usercenter/apikeys |
| deepseek | DeepSeek | — | platform.deepseek.com/api_keys |
| dashscope | 通义千问 | — | bailian.console.aliyun.com |
| moonshot | 月之暗面 Kimi | — | platform.moonshot.cn/console/api-keys |

## 四、实现落点
- `CloudModel.kt`：加 `applyUrl` 字段 + 5 模型填值。
- `AiSettingsScreen.kt`：CloudSection 加 onShowGuide + 链接；guideOpen 状态 + LocalContext；`ApiKeyGuideSheet`(ModalBottomSheet) + `VENDOR_KEY_STEPS` map。

## 五、状态
- ✅ `:androidApp:assembleDebug` 通过。
- ✅ Apple-UX 设计门禁。
- ✅ Google 代码审查门禁：**无阻断**。采纳建议2（Intent 加 FLAG_ACTIVITY_NEW_TASK）+ 建议3（打开失败 Toast 反馈不静默）+ 建议1（免费标注释说明随模型非厂商）+ 建议5（隐私说明加"以官网实际为准"免责延伸到用户可见）；建议4（免费标抽 TagLabel 统一件）为后续项暂不做。
- **真机待验**：链接点击弹 sheet、切厂商内容更新、"打开官网"跳浏览器、深浅色。
- **剩余/免责**：官网 URL 与步骤为 AI 参考整理，随平台可能调整（如阿里云百炼/moonshot 路径），需版本迭代复核；本功能只跳浏览器不自己联网，无需 INTERNET 权限。
