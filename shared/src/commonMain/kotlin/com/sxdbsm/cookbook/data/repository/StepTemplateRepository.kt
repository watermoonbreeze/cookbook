package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.StepTemplate
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.withContext

/**
 * @File : StepTemplateRepository
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 操作步骤模板仓库（列出/新建/删除；预设+自建）
 * <p>
 * 模板=一组文字步骤，编辑菜品时"选择步骤"套用。预设(source=preset)不可删，自建(source=user)可删。
 * <p>
 * [AI生成] #2 复用做法步骤，降低录入成本。
 **/
class StepTemplateRepository(
    private val db: CookbookDatabase,
) {
    private val q = db.cookbookQueries

    /** 列出全部有效模板(预设在前)及其步骤。[AI生成] */
    suspend fun listTemplates(): List<StepTemplate> = withContext(ioDispatcher) {
        q.selectStepTemplates().executeAsList().map { t ->
            StepTemplate(
                id = t.id,
                name = t.name,
                source = t.source,
                steps = q.selectStepTemplateItems(t.id).executeAsList(),
            )
        }
    }

    /**
     * 新建自建模板。[AI生成]
     *
     * @param name 模板名(非空、按去空格名去重复用已有自建模板 id)
     * @param steps 步骤文字(过滤空白)
     */
    suspend fun createTemplate(name: String, steps: List<String>): Long = withContext(ioDispatcher) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "模板名称不能为空" }
        val cleanSteps = steps.map { it.trim() }.filter { it.isNotBlank() }
        require(cleanSteps.isNotEmpty()) { "模板至少需要一个步骤" }
        var templateId = 0L
        db.transaction {
            // [AI生成] 同名自建模板复用同一 id(重建其步骤)，避免重复堆积。
            val existing = q.selectStepTemplateByName(trimmed).executeAsOneOrNull()
            if (existing != null && existing.source != "preset") {
                templateId = existing.id
                q.deleteStepTemplateItems(templateId)
            } else {
                q.insertStepTemplate(trimmed, "user", DateTime.nowEpochSeconds())
                templateId = q.lastInsertId().executeAsOne()
            }
            cleanSteps.forEachIndexed { index, text ->
                q.insertStepTemplateItem(templateId, index.toLong(), text)
            }
        }
        templateId
    }

    /** 软删除模板(通常仅自建)。[AI生成] */
    suspend fun deleteTemplate(id: Long) = withContext(ioDispatcher) {
        db.transaction {
            q.deleteStepTemplateItems(id)
            q.deleteStepTemplate(id)
        }
    }
}
