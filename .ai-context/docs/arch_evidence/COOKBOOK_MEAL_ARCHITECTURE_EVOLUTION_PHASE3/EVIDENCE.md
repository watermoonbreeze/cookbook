# COOKBOOK Meal Architecture Evolution — Phase 3 Evidence

## Scope

Phase 3 Projection Migration only. No schema, Legacy Model deletion, Domain redesign, AI flow, Recipe/Nutrition expansion, or Repository rewrite.

## Projection boundary

`MealProjectionRepository` is a read-only facade over the existing `MealRecordRepository` read implementation. It exposes `MealDayContent`, `DayMealCardData`, and date read results to UI-facing ViewModels. It has no mutation API.

## Migrated flows

| Flow | Before | After |
|---|---|---|
| Home plan cards | HomeViewModel → MealRecordRepository | HomeViewModel → MealProjectionRepository → MealRecordRepository → MealDayContent → MealDayCardProjector |
| Home nutrition wall / today card | HomeViewModel → MealRecordRepository | HomeViewModel → MealProjectionRepository → projection cards |
| Timeline dates / page content | TimelineViewModel → MealRecordRepository | TimelineViewModel → MealProjectionRepository → projection content |
| Meal history search | SearchViewModel → MealRecordRepository | SearchViewModel → MealProjectionRepository → projection cards |

## Remaining legacy reads

`MealRecordRepository` compatibility read APIs remain intentionally available. Other non-Home/Timeline/Search consumers still use them, including edit/readback flows and domain automation. They are not deleted or refactored in Phase 3. Home's historical wall/date range is also routed through the projection facade; its deletion/ratio APIs remain on the legacy repository as writes.

## Tests

- `MealDayCardProjectorTest`: existing projection invariants retained.
- `MealProjectionRepositoryTest`: verifies projection content and timeline dates preserve stored meal facts.
- `:androidApp:assembleDebug`: PASS (final run, 2026-08-24).
- `:shared:testDebugUnitTest`: first cached/full execution returned exit code 0; a subsequent clean test-source recompilation is BLOCKED by pre-existing missing symbols in unrelated tests (`cookingHeaviness`, `isWesternCuisine`, `parseDecimalInput`, `toProjection`, `installCookbookLogSink`). The new production/shared main sources compile successfully as part of Android assemble.

## Acceptance notes

- Projection does not mutate Domain truth.
- UI does not receive Storage entities.
- `MealPlan != MealRecord` and `AI Suggestion != Domain Truth` are unchanged.
- Device verification is not claimed by this static/unit evidence.
