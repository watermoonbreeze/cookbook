# 2026-06-03 修复12执行前上下文快照

## 用户反馈

- 餐食卡片下菜品 block 需要点击进入菜品详情。
- 菜品编辑有时初次正常，返回再进为空；新增菜品编辑也为空；拍照保存后照片丢失。
- 点击添加食材闪退：selectAllIngredients 映射 NPE，IngredientRepository.search。
- 数据库和图片没有生成在 /sdcard/cookbook/db、/sdcard/cookbook/img。

## 执行模式

- 深度 BugFix + 数据/存储路径核验。
- 需要检查 UI block 点击入口、NewDish 编辑加载与保存链路、SQLDelight 迁移/旧数据 NULL、Android 公共目录写入策略。
