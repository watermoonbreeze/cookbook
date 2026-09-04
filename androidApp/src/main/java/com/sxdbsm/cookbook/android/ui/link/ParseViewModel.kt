package com.sxdbsm.cookbook.android.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.android.link.RemoteImageSaver
import com.sxdbsm.cookbook.android.link.WebViewTextExtractor
import com.sxdbsm.cookbook.android.ui.component.StoredImagePair
import com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefill
import com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillStep
import com.sxdbsm.cookbook.android.util.AppLogger
import com.sxdbsm.cookbook.data.parser.ExtractedPage
import com.sxdbsm.cookbook.data.parser.LinkPrefillMapper
import com.sxdbsm.cookbook.data.parser.RecipeParser
import com.sxdbsm.cookbook.data.parser.extractPageFromJson
import com.sxdbsm.cookbook.data.parser.loadParseConfig
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import com.sxdbsm.cookbook.domain.SeasoningDefaults
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.ParseConfig
import com.sxdbsm.cookbook.domain.model.ParsedRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 解析 Sheet 三态（加载/成功/失败·交互规范 A 节）。[AI生成] STEP-L4-8
 */
enum class ParsePhase { LOADING, SUCCESS, FAILED }

/**
 * 解析结果页 UiState（单一真相源·UI 只渲染 + 上抛事件·准则 A）。[AI生成] STEP-L4-8
 *
 * @param unknownIngredientNames 食材库外名集合（「新」徽标·保存时经统一管线自建）
 */
data class ParseUiState(
    val phase: ParsePhase = ParsePhase.LOADING,
    val url: String = "",
    val sourceName: String = "",
    val recipe: ParsedRecipe? = null,
    val stepImages: List<String> = emptyList(),
    val unknownIngredientNames: Set<String> = emptySet(),
    val errorMessage: String = "",
)

/**
 * 远程图预下载进度（封面+步骤图各自独立·互不连坐）。[AI生成] STEP-L4-8
 *
 * @param steps 远程步骤图 URL→本地文件对；值可为 null=该图已结束但失败（整块不渲染·G.1 不留灰洞）
 * @param done 全部图已就绪/超时/失败（供「存为菜品」判断是否还需等剩余）
 */
data class PrefetchState(
    val cover: StoredImagePair? = null,
    val steps: Map<String, StoredImagePair?> = emptyMap(),
    val done: Boolean = false,
)

/**
 * 分享链接解析 VM（采集→解析→落库→预下载→组装预填·蓝图 STEP-L4-8）。[AI生成]
 *
 * 职责：WebView 采集回调送 [RecipeParser] 解析，结果经 [ShareLinkRepository.applyParseResult]
 * 持久化投影（state→1）；同时并行预下载封面/步骤图（8s 软限·各图独立失败不连坐）；
 * 「存为菜品」统一 await 剩余图（3s 上限·DP-P1-6）后组装 NewDishPrefill 交总线跳编辑页。
 *
 * 日志红线 Y-04：只记 host/linkId/计数/耗时，禁完整 URL/菜名/clear_text。
 */
class ParseViewModel(
    private val linkId: Long,
    private val sourceName: String,
    private val linkRepo: ShareLinkRepository,
    private val parser: RecipeParser,
    private val extractor: WebViewTextExtractor,
    private val saver: RemoteImageSaver,
    private val dishRepo: DishRepository,
    private val ingredientRepo: IngredientRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ParseUiState(sourceName = sourceName))
    val state: StateFlow<ParseUiState> = _state.asStateFlow()

    private val _prefetch = MutableStateFlow(PrefetchState())
    val prefetch: StateFlow<PrefetchState> = _prefetch.asStateFlow()

    // 「存为菜品」进行中（就地转 loading + 禁点·交互规范 B.2 双击防护）。[AI生成]
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    // 库外食材的占位临时负 id（递减唯一；保存时经 NewDishViewModel ensureCreated 换真 id·与其自身序列同惯例）。[AI生成]
    private var pendingIdSeq = -1L


    /**
     * 入口一：联网采集解析（首次/T2 同意后/失败重试共用）。[AI生成] STEP-L4-8
     *
     * extract 须主线程调用（WebView 约束），回调后转 IO 做解析与落库。
     */
    fun startCollect(url: String) {
        _state.value = _state.value.copy(
            phase = ParsePhase.LOADING,
            url = url,
            recipe = null,
            stepImages = emptyList(),
            unknownIngredientNames = emptySet(),
            errorMessage = "",
        )
        AppLogger.d(TAG, "start_collect linkId=$linkId host=${hostOf(url)}") // 只记 host·禁完整 URL(Y-04)
        extractor.extract(url) { raw ->
            viewModelScope.launch(Dispatchers.IO) { handleRaw(raw, url) }
        }
    }

    /**
     * 采集回调处理：解码→配置→解析→结果落库→算库外名→进成功态+预下载。[AI生成]
     */
    private suspend fun handleRaw(raw: String?, url: String) {
        val page = extractPageFromJson(raw)
        if (page == null) {
            fail("网页内容提取失败")
            return
        }
        val parsed = parseWith(page)
        if (parsed == null) {
            fail("不支持来源或非菜谱页")
            return
        }
        val (recipe, unknown) = parsed
        // 解析结果持久化投影（四列+state→1 原子写）；失败走失败态（结果不可找回，须让用户可重试）。[AI生成]
        val applied = runCatching {
            linkRepo.applyParseResult(linkId, recipe.title, recipe.imageUrl, page.innerText, page.stepImages.joinToString("|"))
        }.isSuccess
        if (!applied) {
            fail("解析结果保存失败")
            return
        }
        _state.value = _state.value.copy(
            phase = ParsePhase.SUCCESS,
            recipe = recipe,
            stepImages = page.stepImages,
            unknownIngredientNames = unknown,
        )
        // 成功留痕：带 host 便于定位来源（RecipeParser 的 parse_result 无 host·禁完整 URL Y-04）。[AI生成]
        AppLogger.d(TAG, "parse_ok host=${hostOf(url)} unknown_count=${unknown.size}")
        startPrefetch(recipe)
    }

    /**
     * 解析公共段：来源配置+烹饪方式字典→解析→算库外名（handleRaw 与 rebuildFrom 共用）。[AI生成]
     *
     * @return null=判失败（无该来源配置 / 页面不是菜谱页 / 解析器异常——终审 R-1 纵深+R-2 空结果防御）
     */
    private suspend fun parseWith(page: ExtractedPage): Pair<ParsedRecipe, Set<String>>? {
        val config = loadConfig() ?: return null
        val dict = runCatching { dishRepo.listCookingMethods().map { it.name } }.getOrDefault(emptyList())
        // [AI修改] 终审 R-1 纵深：解析器面对任意页面文本，任何未预见异常都兜成失败态而非崩溃。
        val recipe = runCatching { parser.parse(page, config, dict) }.getOrNull() ?: return null
        // [AI修改] 终审 R-2：同域非菜谱页（分类/首页/用户主页）解析出空结果——判失败，
        //   防"0 食材 0 步骤的空菜"经 SUCCESS+state=1（只前进）被存进菜品库。
        if (recipe.ingredients.isEmpty() && recipe.steps.isEmpty()) {
            AppLogger.d(TAG, "parse_empty_result title_blank=${recipe.title.isBlank()}")
            return null
        }
        val existingIds = resolveExistingIdsByName(recipe.ingredients.map { it.name })
        val unknown = recipe.ingredients.map { it.name }.filter { it !in existingIds.keys }.toSet()
        return recipe to unknown
    }

    /**
     * 入口二：本地重建（链接列表 state=1 转发·DP-P1-7）。[AI生成] STEP-L4-8
     *
     * **不联网、不重写库、不弹 T2**（当初已同意过；飞行模式也能打开·INV-L4-16）——
     * 全量真相源=行内 clearText/step_images/title/image_url 四列持久化投影。
     */
    fun rebuildFrom(row: ShareLinkRepository.ShareLinkRow) {
        _state.value = _state.value.copy(
            phase = ParsePhase.LOADING,
            url = row.link,
            sourceName = sourceName,
            recipe = null,
            stepImages = emptyList(),
            unknownIngredientNames = emptySet(),
            errorMessage = "",
        )
        viewModelScope.launch(Dispatchers.IO) {
            val page = ExtractedPage(
                title = row.title,
                author = "",
                imageUrl = row.imageUrl,
                stepImages = row.stepImages,
                innerText = row.clearText,
            )
            val parsed = parseWith(page)
            if (parsed == null) {
                fail("不支持来源或非菜谱页")
                return@launch
            }
            val (recipe, unknown) = parsed
            _state.value = _state.value.copy(
                phase = ParsePhase.SUCCESS,
                recipe = recipe,
                stepImages = row.stepImages,
                unknownIngredientNames = unknown,
            )
            startPrefetch(recipe)
        }
    }

    /** 失败收口：状态回写（留痕不抛）+日志+进失败态。[AI生成] */
    private suspend fun fail(message: String) {
        runCatching { linkRepo.markFailed(linkId) }
        AppLogger.d(TAG, "parse_failed linkId=$linkId")
        _state.value = _state.value.copy(phase = ParsePhase.FAILED, errorMessage = message)
    }

    /**
     * 预下载封面+步骤图（解析成功即并行开跑·交互规范 B.1）。[AI生成]
     *
     * 每图独立 async + 8s 软限（withTimeoutOrNull 不抛）：单图失败/超时只影响自己（null），
     * 不连坐其余图（Supervisor 语义靠 runCatching 达成）；全部结束后置 done=true 并清在途表。
     *
     * [AI修改] 终审 S-2：下载任务以 Deferred 存入 [pendingDownloads]（URL→在途任务）——
     * 「存为菜品」对在途图 **await 同一任务**而非重新下载（RemoteImageSaver 无 URL 级缓存，
     * 重复 awaitAll 会同 URL 双下载双落盘留孤儿文件）；已失败的图不在表内（失败不再重下·B.3）。
     */
    private val pendingDownloads = LinkedHashMap<String, kotlinx.coroutines.Deferred<StoredImagePair?>>()

    private fun startPrefetch(recipe: ParsedRecipe) {
        _prefetch.value = PrefetchState() // 重试/重建时清掉上一轮预下载进度
        pendingDownloads.clear()
        viewModelScope.launch {
            // 每图独立子协程（runCatching 兜异常=互不连坐）；joinAll 等全部结束后置 done。[AI生成]
            val jobs = buildList {
                val coverUrl = recipe.imageUrl
                if (coverUrl.isNotBlank()) {
                    add(
                        async {
                            val pair = runCatching { withTimeoutOrNull(PREFETCH_TIMEOUT_MS) { saver.download(coverUrl) } }.getOrNull()
                            if (pair == null) AppLogger.d(TAG, "prefetch_failed kind=cover host=${hostOf(_state.value.url)}")
                            _prefetch.update { it.copy(cover = pair) }
                            pair
                        }.also { pendingDownloads[coverUrl] = it },
                    )
                }
                recipe.steps.map { it.imageUrl }.filter { it.isNotBlank() }.distinct().forEach { url ->
                    add(
                        async {
                            val pair = runCatching { withTimeoutOrNull(PREFETCH_TIMEOUT_MS) { saver.download(url) } }.getOrNull()
                            if (pair == null) AppLogger.d(TAG, "prefetch_failed kind=step host=${hostOf(_state.value.url)}")
                            _prefetch.update { it.copy(steps = it.steps + (url to pair)) }
                            pair
                        }.also { pendingDownloads[url] = it },
                    )
                }
            }
            jobs.joinAll()
            pendingDownloads.clear()
            _prefetch.update { it.copy(done = true) }
        }
    }

    /**
     * 「存为菜品」：等剩余在途图（3s 上限·DP-P1-6）→ 组装 NewDishPrefill → onReady 交宿主跳编辑页。[AI生成]
     *
     * 已结束的图直接复用（失败不再重下——失败静默无图·交互规范 B.3）；仅 !done 时
     * 等尚未出现在结果集里的在途 URL。保存期间 [saving]=true（禁点·防双击双跳）。
     */
    fun saveAsDish(onReady: (NewDishPrefill) -> Unit) {
        val current = _state.value
        val recipe = current.recipe
        if (current.phase != ParsePhase.SUCCESS || recipe == null) return // 仅成功态可保存
        if (_saving.value) return // 双击防护
        _saving.value = true
        viewModelScope.launch {
            runCatching {
                val started = System.currentTimeMillis()
                val pf = _prefetch.value
                // [AI修改] 终审 S-2：在途判定改看 pendingDownloads（URL→Deferred）——在表=在途（await 同一任务，
                //   不重下不产孤儿文件）；不在表=已终态（成功的已在 pf / 失败的按 B.3 不再重下）。
                //   原「pf.cover==null 即在途」有二义（null 也表示已失败），会把失败图再次发起下载。
                val images = LinkedHashMap<String, LinkPrefillMapper.StoredImageRef?>()
                pf.cover?.let { images[recipe.imageUrl] = it.toStoredRef() }
                pf.steps.forEach { (url, pair) -> if (pair != null) images[url] = pair.toStoredRef() }
                val awaiting = buildList {
                    if (!pf.done && recipe.imageUrl.isNotBlank() && images[recipe.imageUrl] == null) {
                        pendingDownloads[recipe.imageUrl]?.let { add(it) }
                    }
                    if (!pf.done) {
                        recipe.steps.map { it.imageUrl }.filter { it.isNotBlank() }.distinct().forEach { url ->
                            if (images[url] == null) pendingDownloads[url]?.let { add(it) }
                        }
                    }
                }
                if (awaiting.isNotEmpty()) {
                    withTimeoutOrNull(SAVE_AWAIT_TIMEOUT_MS) { awaiting.joinAll() } // 等同一任务（3s 上限·DP-P1-6）
                    val after = _prefetch.value
                    after.cover?.let { if (images[recipe.imageUrl] == null) images[recipe.imageUrl] = it.toStoredRef() }
                    after.steps.forEach { (url, pair) -> if (pair != null && images[url] == null) images[url] = pair.toStoredRef() }
                }
                // 组装预填：库内食材给真 id、库外给占位负 id；单位/调料默认克数按字典口径（Y-6 单位映射表）。[AI生成]
                val existingIds = resolveExistingIdsByName(recipe.ingredients.map { it.name })
                val units = runCatching { ingredientRepo.listMeasurementUnits() }.getOrDefault(emptyList())
                val unitIdsByName = units.associate { it.name to it.id }
                val unitNamesByName = units.associate { it.name to it.name }
                val seasoningIds = runCatching { ingredientRepo.seasoningIngredientIds() }.getOrDefault(emptySet())
                val draft = LinkPrefillMapper.mapToPrefill(
                    recipe = recipe,
                    existingIdsByName = existingIds,
                    pendingSeq = { pendingIdSeq-- },
                    unitIdsByName = unitIdsByName,
                    unitNamesByName = unitNamesByName,
                    defaultGramFor = { name ->
                        SeasoningDefaults.defaultGramFor(name, (existingIds[name] ?: -1L) in seasoningIds)
                    },
                    images = images,
                    sourceTag = "link",
                    linkId = linkId,
                )
                val prefill = NewDishPrefill(
                    name = draft.name,
                    ingredients = draft.ingredients.map {
                        DishIngredient(
                            ingredient = Ingredient(id = it.ingredientId, name = it.name),
                            quantity = it.quantity,
                            unitId = it.unitId,
                            unitName = it.unitName,
                            isMain = it.isMain,
                        )
                    },
                    cookingMethodNames = draft.cookingMethodNames,
                    steps = draft.steps.map { NewDishPrefillStep(text = it.text, imagePath = it.imageFile, thumbnailPath = it.thumbFile) },
                    imagePath = draft.coverImage,
                    thumbnailPath = draft.coverThumb,
                    sourceTag = "link",
                    linkId = linkId,
                    description = draft.description,
                )
                // 日志红线 Y-04：只记耗时/计数，禁菜名。[AI生成]
                AppLogger.d(TAG, "save_as_dish ready elapsed_ms=${System.currentTimeMillis() - started} ingredient_count=${draft.ingredients.size}")
                prefill
            }.onSuccess { prefill ->
                _saving.value = false
                onReady(prefill)
            }.onFailure { e ->
                _saving.value = false
                AppLogger.d(TAG, "save_as_dish_failed error_type=${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * 按名解析库内食材 id（合同按实码适配·repo 未暴露批量按名查 id）。[AI生成]
     *
     * 沿 NewDishViewModel autoAddFromName 先例：search(keyword) + 去空格精确比对，兼容
     * 老库同名多 id/名内空格；查不到即库外名（返回 map 不含该键→「新」徽标+保存时自建）。
     */
    private suspend fun resolveExistingIdsByName(names: List<String>): Map<String, Long> =
        names.map { it.trim() }.filter { it.isNotEmpty() }.distinct().mapNotNull { name ->
            runCatching { ingredientRepo.search(name) }.getOrDefault(emptyList())
                .firstOrNull { it.name.replace(" ", "").trim() == name.replace(" ", "").trim() }
                ?.let { name to it.id }
        }.toMap()

    /** 来源→解析配置（Phase1 硬编码路由·DP-P1-10，路由函数与宿主共用）。[AI修改] 审核修正(2026-09-04)：解码下沉 shared 的 loadParseConfig——androidApp 无须自引 kotlinx-serialization（蓝图零触 gradle 红线），且与测试同一条路径。 */
    private fun loadConfig(): ParseConfig? = loadParseConfig(configPathFor(sourceKey()))

    /** 当前 url 的来源标识（host 归一·与宿主同款路由函数）。[AI生成] */
    private fun sourceKey(): String = sourceFromHost(hostOf(_state.value.url))

    /** 取 URL host；解析失败兜空串（日志只记 host·Y-04）。[AI生成] */
    private fun hostOf(url: String): String = runCatching { java.net.URI(url).host }.getOrNull().orEmpty()

    private fun StoredImagePair.toStoredRef(): LinkPrefillMapper.StoredImageRef =
        LinkPrefillMapper.StoredImageRef(name = imagePath, thumbName = thumbnailPath)

    private companion object {
        private const val TAG = "LinkParse"

        /** 预下载单图软限（8s·超时=该图失败，不阻断其余）。[AI生成] */
        private const val PREFETCH_TIMEOUT_MS = 8_000L

        /** 存为菜品统一等待剩余图的硬上限（3s·DP-P1-6）。[AI生成] */
        private const val SAVE_AWAIT_TIMEOUT_MS = 3_000L
    }
}
