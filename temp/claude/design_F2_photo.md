# F2 拍照入口前移 · 交互设计规范（Apple-UX 门禁产出）

## 核心结论
把**封面图**从底部折叠区(`NewDishScreen.kt` 397-406)上移到"菜名"输入框之上，
新增薄封装 `DishCoverPicker`(复用 ImagePickerButton 图片管线 + StoredImage 展示 + ActionSheet 多操作)：
- 无图=120dp 虚线引导卡(相机图标 AddAPhoto 28dp + 引导文案 + "拍照·相册(选填)")
- 有图=160dp 通栏封面(StoredImage fillWidth) + 右上角 ⋯(重新拍照/从相册更换/删除红字)
折叠区仅留特殊说明/描述，同步清 hasMore 的 imagePath 条件(否则老菜有封面误强制展开)。
DB/VM/步骤配图全不动(state.imagePath/thumbnailPath 字段零改)。封面选填不入保存校验。

## 位置：顶栏下、菜名*之前(173-181行前插入)
## 无图引导卡：fillMaxWidth 高120dp 圆角12dp 1dp虚线separator 底色surface；图标28dp onSurfaceVariant；主文案bodyMedium15；次说明labelMedium13含"(选填)"
## 有图封面卡：StoredImage(fillWidth,imageHeight=160dp,corner12dp)+点图全屏预览；右上⋯ 44x44触达半透明黑圆底
## 交互：无图点卡→ActionSheet(拍照/相册)(建议从AlertDialog升级ActionSheet)；有图点⋯→ActionSheet(重拍/换/删destructive)。删除不弹撤销(表单内可逆)。
## 单封面(maxCount=1)；步骤过程图不动
## 新组件签名
@Composable fun DishCoverPicker(imagePath:String, thumbnailPath:String, onImagesChanged:(image:String,thumb:String)->Unit, modifier:Modifier=Modifier)
建议抽 rememberImageCaptureController() 供 DishCoverPicker 与 ImagePickerButton 共用图片管线，避免复制。
能力由回调决定显隐(不传onImagesChanged=只读)，守红线。
## 文案交 copywriter 定(选填不施压、不用感叹号)
