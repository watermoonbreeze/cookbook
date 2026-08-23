# Test Matrix

| ID | 层级 | 覆盖 | 自动证据 | 设备证据 |
|---|---|---|---|---|
| T-AEP-01 | shared unit | Trace event / logging context | `TraceEventContractTest`, `ArchitectureGovernanceTest` | E-AEP-01~05 |
| T-AEP-02 | shared unit | CREATE/SAVE/RESTORE/MERGE/CLEAR 模型 | `MealFlowStateContractTest`, `ArchitectureGovernanceTest` | E-AEP-01/03 |
| T-AEP-03 | shared unit | Navigation/Parameter/Result contract | `ArchitectureGovernanceTest` | E-AEP-02/04 |
| T-AEP-04 | shared unit | 五类 Meal Flow | `MealFlowStateContractTest` | E-AEP-01~05 |
| T-AEP-05 | shared unit | Diagnostic taxonomy | `TraceDiagnosticTest` | E-AEP-01~05 |
| T-AEP-06 | static | module boundary / governance markers | `.ai-context/tools/architecture_quality_check.py` | N/A |
| T-AEP-07 | static | algorithm/schema/repository scope guard | git diff + forbidden-change review | N/A |

## 执行命令

```text
scripts\build-cli.bat :shared:testDebugUnitTest
python .ai-context/tools/architecture_quality_check.py
```

静态 PASS 仅说明代码和契约检查通过，不等于设备 PASS。
