# 2026-06-04 编辑菜品首次进入异常执行前快照

## 用户反馈

- 整体可以，但编辑菜品第一次好像还是不行。
- 添加菜品后，编辑菜品才可以。

## 执行模式

- 定向 BugFix 复查模式。
- 优先检查 NewDishScreen/NewDishViewModel 启动加载、NavBackStack ViewModel 作用域、MainActivity 授权后 seed 时序、DishRepository.getDishById/observe 流程。
