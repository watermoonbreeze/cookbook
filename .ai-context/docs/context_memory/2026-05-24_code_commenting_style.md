# 2026-05-24 代码注释风格记录

- 已为当前 Kotlin/KMP 项目的主要类、`data class`、`object`、`enum`、ViewModel、Composable 页面和公共组件补充中文注释。
- 注释面向 Java 背景开发者，重点解释 `data class`、默认参数、可空类型、`Flow`、`StateFlow`、`suspend`、`expect/actual`、Koin、Compose `@Composable` 等概念。
- 新增或修改注释均带 `[AI修改]` 标识，符合项目 AI 代码追溯要求。
- 后续编码时继续遵守：新增类要有类级 KDoc；修改关键方法/字段时在相邻位置补 `[AI修改]` 说明；复杂 Kotlin/Compose/KMP 语法要用 Java 类比解释。
- 验证命令：`./gradlew :androidApp:assembleDebug`，结果 BUILD SUCCESSFUL。
