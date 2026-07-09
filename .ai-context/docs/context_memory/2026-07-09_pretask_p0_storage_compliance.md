# P0 存储权限合规迁移 — 任务快照

> [AI生成] 2026-07-09 建立。深度级别任务，常规交互。分析阶段结论，供跨会话续接。

## 目标（来自 待办总览 / 工程优化待办 P0）
上架/普通分发前最高优先合规项：
1. 去掉 `MANAGE_EXTERNAL_STORAGE` 依赖（+ 广义 WRITE/READ_EXTERNAL_STORAGE、requestLegacyExternalStorage）。
2. DB/图片/日志默认迁到 app 专属目录（`getExternalFilesDir`/`filesDir`）。
3. 保留旧 `/sdcard/cookbook` 数据迁移，避免已有测试数据丢失。
4. 导出/备份走 SAF（用户选位置）。

## 现状（已读代码确认）
- **存储根**：`/sdcard/cookbook/`（公共外部），子目录 `db/ img/ log/ backups/`。协调器 `shared/androidMain/.../platform/CookbookStorage.android.kt`。
- **权限**：Manifest 声明 MANAGE_EXTERNAL_STORAGE、WRITE(max29)、READ(max32)、`requestLegacyExternalStorage=true`。
- **门禁**：`MainActivity` 未授权时显示 `StoragePermissionScreen`，`hasPublicStorageAccess` 前 DB/seed 全阻塞；R+ 走 `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`，≤Q 走运行时 WRITE/READ。
- **DB**：`DatabaseDriverFactory.android.kt` 用绝对路径 `/sdcard/cookbook/db/cookbook.db` 建 `AndroidSqliteDriver`；已有 内部`getDatabasePath`→公共 的迁移。`ensureLegacyColumns` 幂等补 `ingredient.reason`（保留）。
- **图片**：`ImagePickerButton.kt` 存 `imageFile.absolutePath`（**DB image_path/thumbnail 存绝对路径**！），文件在 `/sdcard/cookbook/img/`。`StoredImage.kt` 用 `BitmapFactory.decodeFile(uri.path)` 按绝对路径读。
- **日志**：`AppLogger`/`LogFileManager` 写 `/sdcard/cookbook/log/`。
- **备份**：`BackupManager.android.kt` 写 `/sdcard/cookbook/backups/`，保留5份。

## 关键风险 / 难点
1. **图片是绝对路径存 DB**：移目录后旧行仍指向 `/sdcard/cookbook/img/...`；去 MANAGE 后 Android 11+ 读不到 → 旧图全裂。
   → 需把图片改为**存文件名(相对)**，读时按当前 img 目录解析 + 一次性 DB 数据迁移把旧绝对路径转文件名。
2. **旧公共数据迁移的合规悖论**：Android 11+ 去掉 MANAGE 后无法直接读 `/sdcard/cookbook` 来迁移。
   → 直接 File 拷贝仅在 Android ≤10（legacy）或仍可读时成功，需 runCatching 兜底；11+ 完整迁移需 SAF 手动导入。
   → 本 App 尚未上架，`/sdcard/cookbook` 数据在开发/内测设备上（多为测试数据）。
3. 改 Manifest 权限后**必须重装 APK**（红线）。

## 方案骨架（待用户确认迁移策略后实施）
- 改 `CookbookStorage`：根从 `Environment.getExternalStorageDirectory()/cookbook` → `getExternalFilesDir(null)/cookbook`（null 时回退 `filesDir`）；`hasPublicStorageAccess` 恒 true 或删除。
- 删 Manifest 三条存储权限 + `requestLegacyExternalStorage`；删/简化 `StoragePermissionScreen` 门禁。
- DB 迁移源优先级：新 app 专属(已在→no-op) → 旧公共 `/sdcard/cookbook/db`(best-effort) → 内部 `getDatabasePath`。
- 图片：写入存文件名；`StoredImage` 解析 `img 目录/文件名`；DB 一次性把绝对路径→文件名（无副作用/幂等）。
- 备份/导出：备份仍写 app 专属 `backups/`；新增「导出到用户选择位置」走 SAF（`ACTION_CREATE_DOCUMENT`）。

## 待用户拍板
- 旧测试数据迁移策略：A 自动 best-effort（简单、11+ 可能丢，推荐，因是测试数据） / B 保留 MANAGE 一个过渡版 / C SAF 手动导入（11+ 最稳、多 UI）。
