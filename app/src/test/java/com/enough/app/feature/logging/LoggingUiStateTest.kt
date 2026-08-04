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
    fun `decimal inputs accept a comma separator without changing the value`() {
        // The Decimal soft keyboard produces "," in many locales; "2,5" must be
        // 2.5, never a silent 25.
        val withFood = AddMealUiState(selectedFood = banana)
        assertEquals(1.5, withFood.copy(servingsText = "1,5").servings!!, 1e-9)

        val custom = AddMealUiState(customName = "Rye crispbread", customServingText = "1 slice")
        assertEquals(2.5, custom.copy(customFiberText = "2,5").customFiberG!!, 1e-9)
        assertTrue(custom.copy(customFiberText = "2,5").canSaveCustom)
    }

    @Test
    fun `custom food save needs a name and a fiber value, and 0 fiber is valid`() {
        val base = AddMealUiState(customName = "Fairlife shake", customServingText = "1 bottle")
        assertTrue(base.copy(customFiberText = "1").canSaveCustom)
        assertTrue("0g fiber (e.g. a protein shake) is valid", base.copy(customFiberText = "0").canSaveCustom)
        assertFalse("blank fiber is not saveable", base.copy(customFiberText = "").canSaveCustom)
        assertFalse("blank name is not saveable", base.copy(customName = "", customFiberText = "5").canSaveCustom)
        assertFalse("negative fiber is rejected", base.copy(customFiberText = "-3").canSaveCustom)
        assertFalse("blank serving is not saveable", base.copy(customServingText = "", customFiberText = "5").canSaveCustom)
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
