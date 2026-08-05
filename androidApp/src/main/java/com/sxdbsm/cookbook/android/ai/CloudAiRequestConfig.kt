package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.CloudModel

/**
 * @File : CloudAiRequestConfig
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : AF-13: Runtime 所需的 model/key 查询抽象，替代直接依赖 AiRuntimeConfig。
 */
internal interface CloudAiRequestConfig {
    suspend fun selectedModel(): CloudModel
    suspend fun apiKeyForSelectedModel(): String
}

/** 生产 adapter：委托给现有 AiRuntimeConfig（保持 final）。 */
internal class PreferenceCloudAiRequestConfig(
    private val delegate: AiRuntimeConfig,
) : CloudAiRequestConfig {
    override suspend fun selectedModel() = delegate.selectedModel()
    override suspend fun apiKeyForSelectedModel() = delegate.currentCloudApiKey()
}
