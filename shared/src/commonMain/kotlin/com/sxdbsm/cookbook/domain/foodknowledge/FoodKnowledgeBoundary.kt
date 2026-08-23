package com.sxdbsm.cookbook.domain.foodknowledge

/** Food knowledge is reusable knowledge; it is not a meal occurrence. */
data class DishRef(val id: Long)

data class IngredientRef(val id: Long)

interface DishBoundary {
    fun dishRef(id: Long): DishRef = DishRef(id)
}

interface IngredientBoundary {
    fun ingredientRef(id: Long): IngredientRef = IngredientRef(id)
}
