package com.sxdbsm.cookbook.platform

/** [AI生成] 结构化日志的封闭级别值域，避免调用方用任意字符串表达级别。 */
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

/** [AI生成] 结构化日志的封闭分类值域。 */
enum class LogCategory { OPERATION, UI_STATE, DATA_FLOW, PERFORMANCE, SYSTEM, LEGACY }
