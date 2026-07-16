package com.sxdbsm.cookbook.android.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup

/**
 * @File : FeatureGuideScreen
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 功能介绍——一页讲清 Cookbook 是做什么的、每块功能怎么用
 * <p>
 * 三大块：基础功能(食材/菜品/餐食=记录每天吃什么)、营养膳食体系(分析每餐健康程度)、
 * AI 能力(离线规则+AI更好服务)；登录后能力预留。首次使用不迷路。
 * <p>
 * [AI生成] 用户要求：新增功能模块详解，让首次使用者知道 app 做什么、怎么用。
 **/

private data class GuideFeature(val emoji: String, val name: String, val desc: String)
private data class GuideBlock(val title: String, val summary: String, val features: List<GuideFeature>)

private val BLOCKS = listOf(
    GuideBlock(
        title = "① 基础功能 · 记录每天吃什么",
        summary = "食材 → 菜品 → 餐食，三层把「今天吃了/要吃什么」轻松记下来，家庭日常高频用。",
        features = listOf(
            GuideFeature("🥬", "食材", "家里的食材库。可自建、按大类分类、填营养；也是菜品和采购清单的基础。"),
            GuideFeature("🍲", "菜品", "由食材组成的一道菜，可存做法/步骤、收藏常做的；支持从食材一键「组成菜品」。"),
            GuideFeature("🍽️", "餐食", "按早/中/晚记录每天吃了什么。可复用历史菜单、套用收藏组合（能全选/部分选），少操作。"),
        ),
    ),
    GuideBlock(
        title = "② 营养膳食体系 · 吃得更健康",
        summary = "在记录之上分析每餐/每天的饮食健康程度，温和提示，帮全家（尤其慢病成员）吃得更均衡。",
        features = listOf(
            GuideFeature("🎨", "营养色系墙", "用颜色深浅一眼看全年每天的膳食结构均衡度（蛋白/主食/蔬果齐不齐）。"),
            GuideFeature("📊", "今日营养卡", "当天热量、宏量占比与达标情况，按你的身体数据个性化。"),
            GuideFeature("👪", "家庭档案", "登记家庭成员的健康状态（三高/痛风等），据此做忌口提醒与调养建议。"),
            GuideFeature("📚", "参考资料", "食材营养表、膳食参考依据（引用的国家标准/指南）、数据来源，全部透明可查。"),
        ),
    ),
    GuideBlock(
        title = "③ AI 能力 · 更省心的建议",
        summary = "离线规则打底 + AI 增强，帮你解决「今天吃什么」的决策疲劳；无网也能用。",
        features = listOf(
            GuideFeature("✨", "AI 推荐下一餐", "综合你的口味偏好、营养搭配与在手食材，给出可做的搭配建议。"),
            GuideFeature("🧊", "库存推荐", "根据家里现有食材，优先推能做的菜，物尽其用、少浪费。"),
            GuideFeature("🎲", "食材自由搭配", "用在手食材按规则搭出组合建议，纯离线。"),
            GuideFeature("🌈", "推荐多样性", "自动打散同主料，避免「顿顿五花肉」，可选偏熟悉/偏新鲜等风格。"),
        ),
    ),
)

@Composable
fun FeatureGuideScreen(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "功能介绍", onBack = onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IntroBanner()
                BLOCKS.forEach { block ->
                    InsetGroup(title = block.title) {
                        Text(
                            block.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        block.features.forEach { f -> FeatureRow(f) }
                    }
                }
                InsetGroup(title = "登录后（规划中）") {
                    Text(
                        "后续支持账号登录：家庭数据多设备同步；把你家的好菜自愿上报，审核后并入预设库、越用越丰富。具体功能规划中。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Text(
                    "小提示：健康相关内容均为参考、非医嘱；慢病管理请遵医嘱。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun IntroBanner() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cookbook 是什么", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "一款面向家庭、尤其慢性病（三高/痛风等）人群的饮食规划助手。帮你轻松记录每天吃什么，" +
                    "并逐步吃得更均衡、更健康——解决「今天吃什么」的决策疲劳。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeatureRow(f: GuideFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(f.emoji, style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(f.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
