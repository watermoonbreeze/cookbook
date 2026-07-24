-- 预设食材数据生产线 · staging schema（P0）
-- [AI生成] 2026-07-24。独立于生产 SQLDelight schema（不同构·带溯源）。
-- 每张业务表带 source/confidence/review/status，支撑逐字段溯源与交叉校验。
-- 设计见 .ai-context/docs/feature/预设食材数据生产线_详细设计.md 第二节。

PRAGMA foreign_keys = ON;

-- 食材主表(元数据)
CREATE TABLE IF NOT EXISTS ingredient (
  id INTEGER PRIMARY KEY,
  code TEXT UNIQUE,                 -- 英文唯一码(对齐 ingredients.json.code)
  name TEXT NOT NULL,               -- 中文名
  name_key TEXT NOT NULL,           -- 去空格归一名(跨文件关联键)
  alias TEXT,
  unit TEXT DEFAULT 'g',
  emoji TEXT,
  piece_gram REAL,                  -- 计件默认克重(由 nutrition.pieceGram 归并到元数据)
  ref TEXT,                         -- ingredients.json 自带的 ref(如有)
  priority_tier INTEGER,            -- 1健康覆盖 / 2家庭常见 / 3其他(P0 暂空)
  source TEXT,                      -- 数据来源(seed/USDA/CN成分表/人工)
  status TEXT DEFAULT 'candidate',  -- candidate / imported / published
  collected_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_ingredient_name_key ON ingredient(name_key);

-- 食材↔分类(真·分类·食物层级)
CREATE TABLE IF NOT EXISTS ingredient_category (
  ingredient_id INTEGER NOT NULL,
  category_code TEXT NOT NULL,
  PRIMARY KEY (ingredient_id, category_code)
);

-- 食材↔特征标签(gi_high/fiber_high/whole_grain/nutrition_*…)
CREATE TABLE IF NOT EXISTS ingredient_feature (
  ingredient_id INTEGER NOT NULL,
  feature_tag TEXT NOT NULL,
  PRIMARY KEY (ingredient_id, feature_tag)
);

-- 食材↔属性标签(CONTAINS_ALCOHOL/PROCESSED_FRUCTOSE…·忌口中间层)
CREATE TABLE IF NOT EXISTS ingredient_attribute (
  ingredient_id INTEGER NOT NULL,
  attribute TEXT NOT NULL,
  PRIMARY KEY (ingredient_id, attribute)
);

-- 营养素(长表·每字段一行·因不同字段可能来自不同源)
CREATE TABLE IF NOT EXISTS nutrient (
  id INTEGER PRIMARY KEY,
  ingredient_id INTEGER NOT NULL,
  field TEXT NOT NULL,              -- kcal/protein/fat/carb/fiber/sodium/potassium/calcium/gi/purine/saturatedFat/cholesterol
  value REAL,
  unit TEXT,                        -- kcal / 'g/100g' / 'mg/100g' / '指数'
  source TEXT,                      -- USDA-FDC#id / 中国食物成分表6版 / 悉尼GI库 / 二手站名
  confidence TEXT,                  -- high(一手) / mid(交叉/权威近似) / low(二手)
  review TEXT,                      -- verified / pending
  collected_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_nutrient_ing ON nutrient(ingredient_id);

-- 健康/生命阶段忌口规则(指南驱动·人工核)
CREATE TABLE IF NOT EXISTS care_rule (
  id INTEGER PRIMARY KEY,
  ingredient_id INTEGER NOT NULL,
  condition TEXT NOT NULL,          -- care_diabetes/care_pregnancy/… 或 crowd(高尿酸…)
  scope TEXT NOT NULL,              -- 'care'(生命阶段/慢病category体系) / 'crowd'(人群体系)
  level TEXT,                       -- avoid/limit/recommend
  reason TEXT,
  guideline_source TEXT,            -- 指南名+机构+年
  guideline_quote TEXT,             -- 指南原文摘录(守"规则非数据")
  reviewed INTEGER DEFAULT 0,       -- 人工核过=1
  status TEXT DEFAULT 'candidate',
  collected_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_care_ing ON care_rule(ingredient_id);

-- 校验标记(质量门产出·首校体检单的数据源)
CREATE TABLE IF NOT EXISTS review_flag (
  id INTEGER PRIMARY KEY,
  ingredient_id INTEGER,            -- 可空(如孤儿引用无对应食材)
  ref_name TEXT,                    -- 关联失败时记原始名字便于定位
  field TEXT,
  issue TEXT NOT NULL,              -- 引用完整性/低置信度/缺guideline/离群/单位/合理性
  detail TEXT,
  resolved INTEGER DEFAULT 0
);

-- 中↔英食材名映射(USDA 采集用·P1 人工种子表逐步扩)
CREATE TABLE IF NOT EXISTS cn_en_map (
  name_key TEXT PRIMARY KEY,
  en_query TEXT,
  fdc_id INTEGER,
  match_note TEXT,
  confirmed INTEGER DEFAULT 0
);
