package com.sxdbsm.cookbook.android.ui.newdish

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientGroupRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import com.sxdbsm.cookbook.data.repository.StepTemplateRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import com.sxdbsm.cookbook.domain.autogen.IngredientAutoGenerator
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-L4-13: NewDishViewModel 预填/做法锁定行为测试（蓝图 STEP-L4-13·§8）。
 *
 * 构造方式完全照 `HomeMealMutationBoundaryTest` 先例：纯 JVM + JdbcSqliteDriver 内存库 +
 * `Schema.create` + `Dispatchers.setMain(UnconfinedTestDispatcher())`（VM 的 viewModelScope 走 Main），
 * 真实 Repository 组装，无 Robolectric、无 mock 框架；收尾 `driver.close()`（CookbookDatabase 无 close）。
 *
 * 覆盖（INV-L4-09 导入锁 + AUTOGEN-UNIFY touched 语义 + ARCH-06 + Q-05）：
 * - ① applyPrefill 导入锁：imported 置位、做法按导入预填、预填不算用户触碰、
 *   autoAddSerial 恰 +1（ARCH-06：只换 message 不递增 serial 则 Snackbar 永不显示）、message 非空。
 * - ② 手动加做法 → touched 置位（锁定自动预选）。
 * - ③ encodeImagePaths/decodeImagePaths 单值 round-trip（Q-05）。
 * - ④ markUserTouched=false（导入预填路径）不置 touched——并断言做法确实已加入（防"没加所以没锁"恒真假绿）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewDishPrefillBehaviorTest {

    /** 照 HomeMealMutationBoundaryTest：内存库 + 真实 Repository 组装 VM。[AI生成] */
    private fun newVm(db: CookbookDatabase): NewDishViewModel {
        val dish = DishRepository(db)
        val ingredient = IngredientRepository(db)
        val nutrition = NutritionRepository(db)
        return NewDishViewModel(
            dishRepo = dish,
            ingredientRepo = ingredient,
            categoryRepo = FoodCategoryRepository(db),
            pref = PreferenceRepository(db),
            stepTemplateRepo = StepTemplateRepository(db),
            ingredientGroupRepo = IngredientGroupRepository(db),
            autoGen = IngredientAutoGenerator(ingredient, nutrition),
            db = db,
            aliasResolver = IngredientAliasResolver(emptyMap()),
            shareLinkRepo = ShareLinkRepository(db),
        )
    }

    private fun linkPrefill() = NewDishPrefill(
        name = "冬瓜汤",
        cookingMethodNames = listOf("煮"),
        ingredients = listOf(
            DishIngredient(
                ingredient = Ingredient(id = 1, name = "冬瓜"),
                quantity = 500.0,
                unitId = 5,
                unitName = "g",
                isMain = true,
            ),
        ),
    )

    /** 用例① 导入锁：applyPrefill 后 imported 置位、做法=导入值、serial 恰 +1、message 非空、预填不算触碰。[AI生成] */
    @Test
    fun applyPrefillLocksImportedMethodAndBumpsSerial() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = newVm(CookbookDatabase(driver))
            val serialBefore = vm.state.value.autoAddSerial
            vm.applyPrefill(linkPrefill())
            val s = vm.state.value
            assertEquals("导入做法应整组预填", listOf("煮"), s.cookingMethodNames)
            assertTrue("导入预填应置 imported 锁（INV-L4-09）", s.cookingMethodImported)
            assertFalse("预填不算用户触碰（markUserTouched=false）", s.cookingMethodTouched)
            assertEquals("ARCH-06：导入反馈须递增 autoAddSerial 恰 +1", serialBefore + 1, s.autoAddSerial)
            assertNotNull("导入食材应有一次性 Snackbar 文案", s.autoAddMessage)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }

    /** 用例② 既有入口：默认参手动加做法 → touched 置位（此后菜名推演不再覆盖）。[AI生成] */
    @Test
    fun manualAddCookingMethodMarksTouched() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = newVm(CookbookDatabase(driver))
            vm.addCookingMethod("蒸")
            assertTrue("手动加做法应置 touched（AUTOGEN-UNIFY STEP-AU-5.5a）", vm.state.value.cookingMethodTouched)
            assertEquals("做法应确实已加入", listOf("蒸"), vm.state.value.cookingMethodNames)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }

    /** 用例③ 纯函数 round-trip（Q-05）：单值 encode→decode 原样还原（单元素 join 无分隔符）。[AI生成] */
    @Test
    fun imagePathsRoundTripThroughEncodeDecode() {
        assertEquals(listOf("a.jpg"), decodeImagePaths(encodeImagePaths(listOf("a.jpg"))))
    }

    /**
     * 用例④ markUserTouched=false（导入预填同参路径）不置 touched。[AI生成]
     * 红线自查：同时断言做法已加入——若 addCookingMethod 因早退没加上，"touched 仍 false"是恒真假绿。
     */
    @Test
    fun addCookingMethodWithoutTouchDoesNotLock() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = newVm(CookbookDatabase(driver))
            vm.addCookingMethod("炖", markUserTouched = false)
            assertEquals("做法应确实已加入（防恒真假绿）", listOf("炖"), vm.state.value.cookingMethodNames)
            assertFalse("静默加入不得置 touched", vm.state.value.cookingMethodTouched)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }

    /**
     * 用例⑤ 终审 R-3：链接菜谱「适量」项（quantity=null·INV-L4-18）入表单后**保持 null**——
     * 显示层据此显「适量」占位（不显假值 100）；落库一致性由 save() 收口折默认保障（代码走查+真机 E-L4-10）。
     * [AI生成] 2026-09-04 终审阻断修复配套。
     */
    @Test
    fun applyPrefillKeepsNullQuantityAsIs() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val vm = newVm(CookbookDatabase(driver))
            vm.applyPrefill(
                NewDishPrefill(
                    name = "测试汤",
                    ingredients = listOf(
                        DishIngredient(Ingredient(id = 1L, name = "盐"), quantity = null, unitId = null, unitName = "g", isMain = false),
                        DishIngredient(Ingredient(id = 2L, name = "冬瓜"), quantity = 500.0, unitId = 5L, unitName = "g", isMain = true),
                    ),
                ),
            )
            val salt = vm.state.value.ingredients.first { it.ingredient.name == "盐" }
            assertNull("适量项应保持 null（显示「适量」占位的信号源·不被 100 覆盖）", salt.quantity)
            val melon = vm.state.value.ingredients.first { it.ingredient.name == "冬瓜" }
            assertEquals("有量纲项原样保留", 500.0, melon.quantity)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }
}
