package com.sxdbsm.cookbook.android.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup

/**
 * @File : TcmReferenceScreen
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 中医药膳/食养参考（药食同源官方目录 + 免责说明）——参考资料类，纯科普透明展示
 * <p>
 * 说明"药食同源"是国家卫健委法定食品安全管理认定（非疗效背书）、中医食养属传统说法（仅供参考·非医嘱），
 * 以及本 App 如何用（食材"食养"标签 + AI"食补"过滤，只正向展示、不接入慢病评级）。守健康免责红线。
 * <p>
 * [AI生成] 用户 2026-07-18：把中医药参考内容写到 我的-参考资料 下。
 **/
@Composable
fun TcmReferenceScreen(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppTopBar(title = "食养参考", onBack = onBack) },
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
                Body(
                    "以下为**传统食养分类的参考科普**，仅供了解、**非医嘱**；不构成任何诊断或治疗建议，是否适合你、吃多少，请听你的医生。",
                )

                InsetGroup(title = "什么是“药食同源”") {
                    Para(
                        "“药食同源”指国家卫生健康委（原卫生部）与市场监管总局公布的**“按传统既是食品又是中药材的物质”目录**——" +
                            "这是**食品安全管理层面的认定**（这些物质可作普通食品食用），**不是疗效背书**，更不代表“吃了能治病/养生见效”。",
                    )
                    Para("目录经多批公告累计约 106 种，如 枸杞子、山药、大枣、生姜、莲子、陈皮、海带 等。列入目录只表示“可作普通食品食用”，**不代表更营养或该多吃**。")
                }

                InsetGroup(title = "本 App 怎么用它") {
                    Para("· **食材详情“食养”标签**：命中官方目录的食材会标“药食同源”，附来源与免责，仅作事实告知。")
                    Para(
                        "· **AI 推荐“食养”筛选**：可把含药食同源食材的菜优先展示，只是正向排序、**不代表更该吃**——" +
                            "**绝不接入忌口/限量/慢病评级**，不替你判断该不该吃。慢病相关仍由营养维度（钠/GI/嘌呤/热量）单独负责。",
                    )
                }

                InsetGroup(title = "关于中医“食性/四气”") {
                    Para(
                        "中医的“寒/凉/平/温/热”（四气）等属**传统经验说法、无国家标准，不同典籍间也有分歧**，" +
                            "本 App 一期**未纳入**；若后续加入，会显著标注“传统说法·非国标·可能有分歧”，且同样不参与任何健康判定。",
                    )
                }

                InsetGroup(title = "官方来源") {
                    Para("· 卫法监发〔2002〕51 号《既是食品又是药品的物品名单》")
                    Para("· 国家卫健委 2019 年第 8 号公告（当归、山柰、西红花、草果、姜黄、荜茇，限香辛料/调味品）")
                    Para("· 国家卫健委 2023 年第 9 号公告（党参、铁皮石斛、西洋参、黄芪、灵芝等 9 种）")
                    Para("· 国家卫健委·市场监管总局 2024 年第 4 号公告（地黄、麦冬、天冬、化橘红）")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "注：以上为公开公告的名录整理，具体以官方最新版为准；健康数据均为参考整理、非权威核对，不作医疗依据。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text.replace("**", ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun Para(text: String) {
    Text(
        text.replace("**", ""), // 说明性文案，星号仅标重点，展示时去除。
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
