package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * @File : RecommendationDataSourceTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 取数层（自由搭配分类）守护测试
 * <p>
 * 端到端跑真实 seed 数据：在手食材经 classifyPairRole 分类后应能产出搭配，
 * 防止 seed 食材-分类关联演进后角色判定静默退化为全 OTHER（搭配恒空）。
 * <p>
 * [AI生成] 自由搭配一期回归守护。
 **/
class RecommendationDataSourceTest {

    @Test
    fun freePairingWorksOnRealSeedData() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded() // 灌入食材 + general 大类关联
        val pantry = PantryRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val dataSource = RecommendationDataSource(
            db, pantry, DishRepository(db), HealthProfileRepository(db), ingredientRepo,
        )

        // 在手：一荤(猪瘦肉)一素(青椒) → 应能搭出荤×素
        val pork = ingredientRepo.search("猪瘦肉").first { it.name == "猪瘦肉" }.id
        val pepper = ingredientRepo.search("青椒").first { it.name == "青椒" }.id
        pantry.addToPantry(pork)
        pantry.addToPantry(pepper)

        val out = dataSource.freePairing()
        assertTrue(out.isNotEmpty(), "有荤有素应产出搭配——守护 classifyPairRole 不因 seed 演进而全判 OTHER")
        assertTrue(
            out.any { it.items.contains("猪瘦肉") || it.items.contains("青椒") },
            "搭配项应包含在手食材",
        )
        Unit
    }
}
