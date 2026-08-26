# FOUNDATION-OBSERVABILITY-03 Runtime Alignment

状态：`BLUEPRINT_READY`。执行：`OBS-NEXT-A → B → C`。真机统一末期，不阻断代码；每批自动化通过后登记为 `PENDING_DEVICE_VERIFICATION`。

## 目标

收口 KMP/Android 日志、Trace、MDC3 与隐私边界，使 debug JSONL 可用于排障；不改变业务结果。

## 已知 AF

- AF-OBS-NEXT-01：BusinessTrace 进程级 current Trace，异步会串链。
- AF-OBS-NEXT-02：MDC3 legacy message 未进入 JSONL，证据不可产生。
- AF-OBS-NEXT-03：CrashReporter 第二文件且含原始 exception message。
- AF-OBS-NEXT-04：事件注册表遗漏生产事件，测试自证。
- AF-OBS-NEXT-05：日志初始化晚；前移时必须隔离 :crash。
- AF-OBS-NEXT-06：全景图/真机清单与已提交代码漂移。

## A：核心与 Trace/MDC

范围：shared platform LogLevel/TraceModel/TraceEventContract/Logger/CookbookLog/MealDataTraceLogger、android CookbookLog、AppLogger/CrashReporter、JsonlLogWriter、MainProcessLoggingPolicy、Application/MainActivity、路由及 AI/AddMeal/Search trace 点、质量脚本及对应单测。

- Trace 显式传递，删除 global current/currentTraceId。
- 唯一持久化出口为每日 JSONL；删除 crash_reported.log 写路径。
- Application 主进程、Koin 前幂等安装；`:crash` 不安装普通 sink。
- MDC3 发结构化 `meal_data.stage(source,count)`，不记 token、日期原文或日期列表。

## B：隐私机械收口

范围：VoiceRecognizer、Dishes/NewDish ViewModel/Screen、AddMeal/AddDayFood/MainScaffold、UmengAnalyticsSink、analytics LogSink。

自由文本、菜名、健康值、prompt/response、token/key/cookie、路径、异常 message/stack 一律不落盘；集合记 count，错误记稳定 code/type。

## C：证据与文档

更新 BLUEPRINT_STATE、SESSION、ADR-0002、D-28、诊断/待办/F-TOOLS 状态、唯一真机清单与经验。真机项只标 `PENDING_DEVICE_VERIFICATION`。

## 不变量

1. 唯一 JSONL 持久化出口；每行独立 decode、固定 envelope。
2. Debug 普通日志可落盘；Release 普通日志零落盘，仅 crash/report marker 可写。
3. 敏感内容全路径禁止泄漏；日志失败不影响业务。
4. session 内 seq 严格递增；Trace 无全局状态；OperationTrace first-wins、terminal 唯一。
5. 固定事件注册表；MDC3 仅结构化安全字段。
6. 不改 schema、Repository 行为、算法/prompt、UI视觉、iOS；不做删除/轮转/保留期策略。

## 自动化门

JSONL golden/脱敏、debug-release-crash 矩阵、并发写/trace 交错、初始化/写失败、路由 UUID、事件注册表反向核验、MDC3 安全字段、无第二 writer/未知事件/敏感日志负例。

必须执行 shared+Android debug/release 单测、shared 编译、debug/release assemble、quality Python 与 diff check；真机操作统一登记于唯一清单。
