package com.sxdbsm.cookbook.platform

/**
 * 从受控结构化事件还原业务链路的最小诊断器。[AI生成]
 * 只消费事件类型和代码标识，不解析或输出用户输入、菜名和完整草稿。
 */
object TraceDiagnostic {
    enum class Status { COMPLETE, INCOMPLETE, EMPTY }

    enum class Finding {
        FLOW_PASS,
        FLOW_INCOMPLETE,
        STATE_RESTORE_FAILURE,
        MERGE_FAILURE,
    }

    enum class Node { ACTION, NAVIGATION, OPERATION, SAVE_STATE, RESTORE_STATE, MERGE_RESULT }

    data class Result(
        val traceId: TraceId?,
        val status: Status,
        val finding: Finding,
        val observed: Set<Node>,
        val missing: Set<Node>,
    ) {
        val summary: String
            get() = when (status) {
                Status.COMPLETE -> "FLOW_COMPLETE"
                Status.INCOMPLETE -> "FLOW_INCOMPLETE_MISSING_${missing.joinToString("_") { it.name }}"
                Status.EMPTY -> "FLOW_EMPTY"
            }
    }

    /** 默认要求一次可诊断的返回链路具备 action/navigation/operation 和三段状态生命周期。 */
    private val requiredNodes = linkedSetOf(
        Node.ACTION,
        Node.NAVIGATION,
        Node.OPERATION,
        Node.SAVE_STATE,
        Node.RESTORE_STATE,
        Node.MERGE_RESULT,
    )

    fun diagnose(events: List<StructuredLogEvent>): Result {
        if (events.isEmpty()) return Result(null, Status.EMPTY, Finding.FLOW_INCOMPLETE, emptySet(), requiredNodes)
        val traceId = events.mapNotNull { it.traceId }.distinct().singleOrNull()
        val observed = buildSet {
            events.forEach { event ->
                when (event) {
                    is StructuredLogEvent.Action -> add(Node.ACTION)
                    is StructuredLogEvent.Navigation -> add(Node.NAVIGATION)
                    is StructuredLogEvent.Operation -> add(Node.OPERATION)
                    is StructuredLogEvent.StateLifecycle -> when (event.event) {
                        "state.snapshot.before_navigation" -> add(Node.SAVE_STATE)
                        "state.restore" -> add(Node.RESTORE_STATE)
                        "state.merge.result" -> add(Node.MERGE_RESULT)
                    }
                    else -> Unit
                }
            }
        }
        val missing = requiredNodes - observed
        val finding = when {
            missing.isEmpty() -> Finding.FLOW_PASS
            Node.RESTORE_STATE in missing -> Finding.STATE_RESTORE_FAILURE
            Node.MERGE_RESULT in missing -> Finding.MERGE_FAILURE
            else -> Finding.FLOW_INCOMPLETE
        }
        return Result(
            traceId = traceId,
            status = if (missing.isEmpty()) Status.COMPLETE else Status.INCOMPLETE,
            finding = finding,
            observed = observed,
            missing = missing,
        )
    }
}
