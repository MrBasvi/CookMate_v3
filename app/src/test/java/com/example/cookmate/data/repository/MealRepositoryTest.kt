package com.example.cookmate.data.repository

import com.example.cookmate.data.api.MealApiService
import com.example.cookmate.data.api.RemoteMeal
import com.example.cookmate.data.api.MealResponse
import com.example.cookmate.data.api.MealDetailsResponse
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MealRepositoryTest {
    
    @Mock
    private lateinit var apiService: MealApiService
    
    private lateinit var repository: MealRepository
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = MealRepository(apiService)
    }

    private fun createRemoteMeal(id: String, name: String) = RemoteMeal(
        idMeal = id,
        strMeal = name,
        strDrinkAlternate = null,
        strCategory = "Test",
        strArea = "Test",
        strInstructions = "Test",
        strMealThumb = "url",
        strTags = null,
        strYoutube = null,
        strIngredient1 = "Flour",
        strMeasure1 = "2 cups",
        strIngredient2 = "Sugar",
        strMeasure2 = "1 cup",
        strIngredient3 = null,
        strMeasure3 = null,
        strIngredient4 = null,
        strMeasure4 = null,
        strIngredient5 = null,
        strMeasure5 = null,
        strIngredient6 = null,
        strMeasure6 = null,
        strIngredient7 = null,
        strMeasure7 = null,
        strIngredient8 = null,
        strMeasure8 = null,
        strIngredient9 = null,
        strMeasure9 = null,
        strIngredient10 = null,
        strMeasure10 = null,
        strIngredient11 = null,
        strMeasure11 = null,
        strIngredient12 = null,
        strMeasure12 = null,
        strIngredient13 = null,
        strMeasure13 = null,
        strIngredient14 = null,
        strMeasure14 = null,
        strIngredient15 = null,
        strMeasure15 = null,
        strIngredient16 = null,
        strMeasure16 = null,
        strIngredient17 = null,
        strMeasure17 = null,
        strIngredient18 = null,
        strMeasure18 = null,
        strIngredient19 = null,
        strMeasure19 = null,
        strIngredient20 = null,
        strMeasure20 = null
    )
    
    @Test
    fun testSearchMealsByName_SuccessfulTransformation() = runTest {
        val query = "pasta"
        val remoteMeal = createRemoteMeal("1", "Pasta")
        whenever(apiService.searchMealsByName(query)).thenReturn(MealResponse(listOf(remoteMeal)))
        
        val result = repository.searchMealsByName(query)
        
        assertEquals(1, result.size)
        assertEquals("1", result[0].idMeal)
        assertEquals(2, result[0].ingredients.size)
    }
    
    @Test
    fun testSearchMealsByName_EmptyResult() = runTest {
        val query = "test"
        whenever(apiService.searchMealsByName(query)).thenReturn(MealResponse(null))
        
        val result = repository.searchMealsByName(query)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testGetMealDetails_Success() = runTest {
        val remoteMeal = createRemoteMeal("1", "Carbonara")
        whenever(apiService.getMealDetails("1")).thenReturn(MealDetailsResponse(listOf(remoteMeal)))
        
        val result = repository.getMealDetails("1")
        
        assertNotNull(result)
        assertEquals("1", result.idMeal)
    }

    @Test
    fun testGetMealDetails_NotFound() = runTest {
        whenever(apiService.getMealDetails("999")).thenReturn(MealDetailsResponse(null))
        
        assertFailsWith<Exception> {
            repository.getMealDetails("999")
        }
    }
}
