# AI 云端 API Key 申请指南

> [AI生成] 2026-07-08。为「AI 推荐下一餐」申请免费云端大模型 API Key。
> 首选 **智谱 GLM-4-Flash（完全免费）**；下方也列了备选。拿到 key 后填到 App 设置里即可（见末尾）。

---

## 首选：智谱 GLM-4-Flash（免费、中文强、接口简单）

**为什么选它**：`glm-4-flash` 模型**免费**、中文和 JSON 输出稳定、Bearer Token 认证最简单、数据境内合规。

### 申请步骤
1. 打开智谱开放平台：**https://open.bigmodel.cn/** （也叫 BigModel / 智谱AI开放平台）
2. **注册账号**（手机号），登录
3. 完成**实名认证**（个人实名即可，免费模型也需要）
4. 进入**「API Keys」**页面（右上角头像 → API Keys，或直接 https://open.bigmodel.cn/usercenter/apikeys ）
5. 点**「添加新的 API Key」**，复制生成的 key（形如 `xxxxxxxx.yyyyyyyy`）
6. 确认可用模型里有 **`glm-4-flash`**（免费额度，通常无需充值）

### 我们会用到的信息
| 项 | 值 |
|---|---|
| 接口地址 | `https://open.bigmodel.cn/api/paas/v4/chat/completions` |
| 模型名 | `glm-4-flash` |
| 认证 | 请求头 `Authorization: Bearer <你的APIKey>` |
| 计费 | glm-4-flash 免费 |

> 你只需要把 **API Key** 这一串给到 App（填进设置），其余接口/模型名代码里已内置。

---

## 备选（如果不想用智谱）

| 厂商 | 免费模型 | 申请入口 | 备注 |
|---|---|---|---|
| **讯飞星火** | Spark Lite | https://xinghuo.xfyun.cn/ → 控制台 | 免费；认证较复杂(appid/apikey/apisecret + WebSocket)，接入成本高些 |
| **百度文心** | ERNIE-Speed / ERNIE-Lite | https://qianfan.cloud.baidu.com/ | 免费；需 API Key + Secret |
| **DeepSeek** | deepseek-chat | https://platform.deepseek.com/ | 极便宜(非免费)，新用户有试用额度；接口 OpenAI 兼容 |

> 我们的 `AiRuntime` 抽象层是通用的，换厂商只改适配器 + 接口地址/认证，业务代码不动。**首选还是智谱 GLM-4-Flash（最省事、免费）。**

---

## 拿到 Key 之后怎么填（App 侧，我会做好入口）

- App 里会有个 **「AI 设置」** 入口（我在 UI 阶段加），把 Key 粘进去保存即可。
- Key **只存本机**（偏好存储），不上传、不写日志。
- 没填 Key / 无网络时，AI 推荐会**自动回退到纯规则推荐**（仍可用，只是少了模型的组合与解释）。

---

## 隐私说明（申请前你该知道的）
- 走云端时，只发送**在手食材名 + 粗约束标签（如"忌高嘌呤"）+ 候选菜名**，**不发送**完整健康档案/体检/身份信息。
- 后续会做**端侧本地模型版**（数据完全不出设备），与云端通过同一抽象层随时切换。
