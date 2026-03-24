package com.vibely.domain.inventory

import com.vibely.domain.common.Timestamp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InventoryTest {
    @Test
    fun `IngredientStock can be constructed`() {
        val stock =
            IngredientStock(
                id = IngredientId("ing-1"),
                name = "Tomatoes",
                quantity = 5.0,
                unit = UnitOfMeasure.KILOGRAM,
                alertThreshold = 2.0,
            )
        stock.name shouldBe "Tomatoes"
        stock.quantity shouldBe 5.0
        stock.alertThreshold shouldBe 2.0
        stock.unit shouldBe UnitOfMeasure.KILOGRAM
    }

    @Test
    fun `below-threshold detection - quantity less than alertThreshold`() {
        val stock =
            IngredientStock(
                id = IngredientId("ing-2"),
                name = "Cream",
                quantity = 1.5,
                unit = UnitOfMeasure.LITRE,
                alertThreshold = 2.0,
            )
        (stock.quantity < stock.alertThreshold) shouldBe true
    }

    @Test
    fun `above-threshold stock is not below threshold`() {
        val stock =
            IngredientStock(
                id = IngredientId("ing-3"),
                name = "Flour",
                quantity = 10.0,
                unit = UnitOfMeasure.KILOGRAM,
                alertThreshold = 2.0,
            )
        (stock.quantity < stock.alertThreshold) shouldBe false
    }

    @Test
    fun `StockAlert captures snapshot quantity`() {
        val alert =
            StockAlert(
                id = StockAlertId("alert-1"),
                ingredientId = IngredientId("ing-2"),
                quantityAtAlert = 1.5,
                unit = UnitOfMeasure.LITRE,
                timestamp = Timestamp(1_700_000_000_000L),
            )
        alert.quantityAtAlert shouldBe 1.5
        alert.ingredientId.value shouldBe "ing-2"
    }

    @Test
    fun `UnitOfMeasure is exhaustively handleable without else`() {
        UnitOfMeasure.values().forEach { unit ->
            val label =
                when (unit) {
                    UnitOfMeasure.KILOGRAM -> "kg"
                    UnitOfMeasure.GRAM -> "g"
                    UnitOfMeasure.LITRE -> "L"
                    UnitOfMeasure.MILLILITRE -> "mL"
                    UnitOfMeasure.UNIT -> "unit"
                    UnitOfMeasure.PORTION -> "portion"
                }
            label.isNotEmpty() shouldBe true
        }
    }
}
