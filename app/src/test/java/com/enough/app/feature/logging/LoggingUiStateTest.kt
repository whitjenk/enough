package com.enough.app.feature.logging

import com.enough.app.data.local.entity.Food
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoggingUiStateTest {

    private val banana = Food(id = 2, name = "Banana", servingLabel = "1 medium", carbsG = 27.0, fiberG = 3.1, proteinG = 1.3)

    @Test
    fun `add meal cannot save without a selected food`() {
        assertFalse(AddMealUiState(servingsText = "1").canSave)
    }

    @Test
    fun `add meal requires a positive numeric serving`() {
        val withFood = AddMealUiState(selectedFood = banana)
        assertTrue(withFood.copy(servingsText = "1").canSave)
        assertTrue(withFood.copy(servingsText = "1.5").canSave)
        assertFalse(withFood.copy(servingsText = "0").canSave)
        assertFalse(withFood.copy(servingsText = "").canSave)
        assertFalse(withFood.copy(servingsText = "abc").canSave)
        assertNull(withFood.copy(servingsText = "-2").servings)
    }

    @Test
    fun `log weight parses pounds and gates save`() {
        assertFalse(LogWeightUiState(weightLbText = "").canSave)
        assertFalse(LogWeightUiState(weightLbText = "0").canSave)
        assertTrue(LogWeightUiState(weightLbText = "182.5").canSave)
        assertEquals(182.5, LogWeightUiState(weightLbText = "182.5").weightLb!!, 1e-9)
    }

    @Test
    fun `log activity parses a positive integer amount`() {
        assertFalse(LogActivityUiState(amountText = "").canSave)
        assertFalse(LogActivityUiState(amountText = "0").canSave)
        assertTrue(LogActivityUiState(amountText = "30").canSave)
        assertEquals(30, LogActivityUiState(amountText = "30").amount)
    }
}
