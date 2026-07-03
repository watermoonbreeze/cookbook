# 2026-05-24 图片压缩与菜品喜爱度规则

## 图片存储链路

- 相机原始输出位置：`context.cacheDir/images/cookbook_*.jpg`，通过 `${applicationId}.fileprovider` 暴露给系统相机。
- 原始相机文件只作为临时输入；拍照成功后会压缩并写入 `context.filesDir/images/cookbook_*.jpg`。
- 相册选择的 `content://` 图片也会立即压缩到 `filesDir/images/`，避免后续依赖相册临时授权。
- `image_path` 继续保存图片路径，多张图片仍用 `|` 分隔；保存的是压缩后的 app 私有文件绝对路径。
- `StoredImage` 在所有页面读取 `image_path` 的第一张图展示，点击真实图片会弹出较大预览。

## 压缩规则

- 压缩逻辑位于 `ImagePickerButton.kt`。
- 后台 `Dispatchers.IO` 执行，避免拍照/相册返回后阻塞主线程。
- 目标大小：单张压缩图控制在 `10 * 1024` bytes 内。
- 策略：先按最长边采样解码，再循环降低 JPEG quality；仍超限时继续降低采样尺寸。

## 喜爱度规则

- `dish.preference` 使用 `INTEGER` / Kotlin `Int`，不再使用小数。
- 总分上限 1000。
- 每次菜品被添加到餐食记录时，调用 `incrementDishPreference`，累加 1，上限 1000。
- 星级展示规则：5 颗星，每颗 200 分，`preference / 200.0` 映射为 0-5 星。

