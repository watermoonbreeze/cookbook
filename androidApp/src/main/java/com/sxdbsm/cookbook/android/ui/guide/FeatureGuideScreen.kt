package com.sxdbsm.cookbook.android.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors

/**
 * @File : FeatureGuideScreen
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 功能介绍——Apple「欢迎/功能高亮」范式：大标题 hero + 三段章节卡 + 底部主 CTA
 * <p>
 * 一眼看懂 Cookbook 是什么、能做什么。三大块：基础功能(食材/菜品/餐食)、营养膳食体系、AI 能力。
 * 参照 Apple 健康/健身首启 feature-highlights；非设置分组语义(不用 InsetGroup)。图标=emoji+着色圆角容器。
 * <p>
 * [AI生成] 按 Apple-UX 设计规范重做：hero 排版建立气场、章节卡收纳、底部胶囊 CTA。
 **/

private data class GuideFeature(val emoji: String, val name: String, val desc: String)
private data class GuideBlock(val title: String, val summary: String, val features: List<GuideFeature>)

private val BLOCKS = listOf(
    GuideBlock(
        title = "记录每天吃什么",
        summary = "食材 → 菜品 → 餐食，三层把「今天吃了什么」轻松记下来。",
        features = listOf(
            GuideFeature("🥬", "食材", "你家的食材库，可自建、按大类分类、填营养，是菜品与采购清单的基础。"),
            GuideFeature("🍲", "菜品", "由食材组成的一道菜，可存做法步骤、收藏常做的，支持从食材一键「组成菜品」。"),
            GuideFeature("🍽️", "餐食", "按早/中/晚记录每天吃了什么，可复用历史菜单、套用收藏组合，少操作。"),
        ),
    ),
    GuideBlock(
        title = "吃得更健康",
        summary = "在记录之上分析每餐/每天的健康程度，温和提示，帮全家（尤其慢病成员）吃得更均衡。",
        features = listOf(
            GuideFeature("🎨", "营养色系墙", "用颜色深浅一眼看全年每天的膳食结构均衡度。"),
            GuideFeature("📊", "今日营养卡", "当天热量、宏量占比与达标情况，按你的身体数据个性化。"),
            GuideFeature("👪", "家庭档案", "登记成员健康状态（三高/痛风等），据此做忌口提醒与调养建议。"),
            GuideFeature("📚", "参考资料", "营养表、膳食依据、数据来源，全部透明可查。"),
        ),
    ),
    GuideBlock(
        title = "更省心的建议",
        summary = "离线规则打底 + AI 增强，帮你解决「今天吃什么」的决策疲劳，无网也能用。",
        features = listOf(
            GuideFeature("✨", "AI 推荐下一餐", "综合口味偏好、营养搭配与在手食材，给出可做的搭配建议。"),
            GuideFeature("🧊", "库存推荐", "按家里现有食材优先推能做的菜，物尽其用、少浪费。"),
            GuideFeature("🎲", "食材自由搭配", "用在手食材按规则搭出组合建议，纯离线。"),
            GuideFeature("🌈", "推荐多样性", "自动打散同主料，避免「顿顿五花肉」，可选偏熟悉/偏新鲜风格。"),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureGuideScreen(onBack: () -> Unit) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val green = LocalExtendedColors.current.success
    // 三块点缀色：记录=accent暖、营养=绿、AI=accent（语义色克制、单 tint 为主）。
    val accents = listOf(primary, green, primary)

    Surface(color = bg, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // 外层 NavHost 已对无底栏路由加 navigationBarsPadding，这里不重复
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = primary,
                    ),
                )
            },
            bottomBar = {
                Surface(color = bg) {
                    CapsuleButton(
                        text = "开始使用",
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                GuideHero(primary)
                Spacer(Modifier.height(20.dp))
                BLOCKS.forEachIndexed { i, block ->
                    GuideSectionCard(block, accents[i])
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(8.dp))
                GuidePlannedNote()
                Text(
                    "健康相关内容均为参考、非医嘱，慢病管理请遵医嘱。",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GuideHero(accent: Color) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)) {
        Text("Cookbook", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = accent)
        Text(
            "今天吃什么，交给它",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "面向家庭、尤其慢病（三高/痛风等）人群的饮食规划助手，帮你轻松记录每天吃什么、逐步吃得更健康，告别「今天吃什么」的决策疲劳。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuideSectionCard(block: GuideBlock, accent: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Text(block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                block.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                block.features.forEach { FeatureRow(it, accent) }
            }
        }
    }
}

@Composable
private fun FeatureRow(f: GuideFeature, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(f.emoji, fontSize = 20.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(f.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                f.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuidePlannedNote() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("登录后（规划中）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "账号登录后：家庭数据多设备同步；把你家的好菜自愿上报、审核后并入预设库，越用越丰富。功能规划中。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
