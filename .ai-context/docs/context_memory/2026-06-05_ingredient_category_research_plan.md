# 2026-06-05 基础食材与多维分类调研方案上下文

- 前置测试：shared 单元测试已新增并通过，实际执行 6 个测试；迁移校验和 assembleDebug 通过。
- 调研来源：中国居民膳食指南 2022、NHLBI DASH Eating Plan、Mayo Clinic gout diet、Cleveland Clinic/Diabetes Canada glycemic index 资料。
- 方案方向：食材库按日常食材大类扩充，同时用多维分类挂载到 `food_category`/`ingredient_category`/`crowd_ingredient`。
- 推荐维度：日常使用、营养属性、慢病人群、GI/GL、嘌呤、钠/脂肪/胆固醇、烹调用途、季节/产地、加工状态。
- 下一步可落地：先建立 300-500 个基础食材种子数据，再补 crowd_ingredient 的 recommend/limit/avoid 规则。
