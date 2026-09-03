package com.sxdbsm.cookbook.android.ui.newdish

import com.sxdbsm.cookbook.domain.model.DishIngredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 预填步骤草稿(单步)。[AI生成] STEP-L4-4：text=步骤文字；imagePath/thumbnailPath=该步过程图本地**单文件名**
 * (未编码——NewDishViewModel 侧经 encodeImagePaths(listOf(x)) 单元素包装落 UiState·单元素 join 无分隔符)。
 **/
data class NewDishPrefillStep(
    val text: String,
    val imagePath: String = "",
    val thumbnailPath: String = "",
)

/**
 * @File : NewDishPrefillBus
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 跨屏"新建菜品预填"事件总线
 * <p>
 * 场景：①菜品/首页搜索无结果"＋新建菜品「x」"→预填菜名；②食材页多选"组成菜品"→预填一批食材；
 * ③自由搭配"存为菜品"→预填食材+做法；④分享链接解析"存为菜品"→预填完整菜谱(菜名/含量纲食材/多做法/
 * 步骤+过程图/封面/描述/linkId 回写)[AI生成] STEP-L4-4 链接导入扩展。
 * 因新建菜品页与来源页各持独立 ViewModel(viewModel 作用域)，用单例总线传预填数据；
 * NewDishScreen 进入时消费。消费失败只退回"空白新建"，不影响原流程。
 * <p>
 * [AI生成] 统一搜索"点此新建" + 从食材出发生成菜品 + 外部菜谱导入。
 **/
data class NewDishPrefill(
    val name: String = "",
    // [AI生成] STEP-L4-4：类型替换 List<Ingredient>→List<DishIngredient>(Q-01 量纲住 DishIngredient)——
    //   链接导入产出含完整量纲(quantity/unitId/unitName)；旧生产点(组成菜品/自由搭配)只带 ingredient+isMain，
    //   无量纲项由 NewDishViewModel.addPrefilledIngredient 兜底补默认克数。
    val ingredients: List<DishIngredient> = emptyList(),
    val cookingMethodName: String = "", // [AI生成] 自由搭配"存为菜品"：预填做法(如清炒/红烧)，空=不预填；与新多值互斥(DP-P1-13·多值非空忽略单值)
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] STEP-L4-4：链接导入的多值做法(非空时忽略上面单值)
    val steps: List<NewDishPrefillStep> = emptyList(), // [AI生成] STEP-L4-4：步骤草稿(每步单图未编码)
    val imagePath: String = "", // [AI生成] STEP-L4-4：封面原图(encodeImagePaths 口径·单图即文件名本身)
    val thumbnailPath: String = "", // [AI生成] STEP-L4-4：封面缩略图(同上)
    val sourceTag: String = "user", // [AI生成] STEP-L4-4：食材来源标签(链接导入传"link"·保存时 ensureCreated 按 tag 走)
    val linkId: Long? = null, // [AI生成] STEP-L4-4：来源链接行 id(保存成功回写 share_link.dish_id·DP-P1-4)
    val description: String = "", // [AI生成] STEP-L4-4：网页备注/心得(note 断链闭合 A7)
)

class NewDishPrefillBus {
    private val _pending = MutableStateFlow<NewDishPrefill?>(null)
    val pending: StateFlow<NewDishPrefill?> = _pending.asStateFlow()

    /** 请求以给定名称/食材预填新建菜品。[AI生成] */
    fun request(prefill: NewDishPrefill) {
        _pending.value = prefill
    }

    /** 消费后清空，避免重复触发。[AI生成] */
    fun consume() {
        _pending.value = null
    }
}
