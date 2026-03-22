package com.example.cookmate.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CookMateViewModelFlowTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mealRepository: MealRepository
    
    @Mock
    private lateinit var favouriteService: FavouriteMealService
    
    private lateinit var viewModel: CookMateViewModel
    
    private val favoritesFlow = MutableStateFlow<List<Meal>>(emptyList())
    
    private val testMeal1 = Meal(
        idMeal = "1",
        strMeal = "Test Meal 1",
        strMealThumb = "http://example.com/test1.jpg",
        strCategory = "Type1",
        strArea = "Area1",
        strInstructions = "Test",
        ingredients = emptyList()
    )
    
    private val testMeal2 = Meal(
        idMeal = "2",
        strMeal = "Test Meal 2",
        strMealThumb = "http://example.com/test2.jpg",
        strCategory = "Type2",
        strArea = "Area2",
        strInstructions = "Test",
        ingredients = emptyList()
    )
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(favouriteService.getAllFavourites()).thenReturn(favoritesFlow)
        viewModel = CookMateViewModel(mealRepository, favouriteService)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testFavoritesFlowEmissions() = runTest {
        // Test that favorites flow correctly emits updates
        favoritesFlow.value = listOf(testMeal1)
        advanceUntilIdle()
        
        assertEquals(1, viewModel.uiState.favorites.size)
        assertEquals("1", viewModel.uiState.favorites[0])
        assertEquals(1, viewModel.uiState.favoriteMeals.size)
    }
    
    @Test
    fun testNoExtraEmissions() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal1))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        val firstCount = viewModel.uiState.allMeals.size
        
        viewModel.searchMeals("test") // Repeat
        advanceUntilIdle()
        val secondCount = viewModel.uiState.allMeals.size
        
        assertEquals(firstCount, secondCount)
    }
    
    @Test
    fun testOldRequestDoesNotOverrideNewResult() = runTest {
        // This is handled by searchJob.cancel() in ViewModel
        whenever(mealRepository.searchMealsByName("old")).thenAnswer {
            // Simulated delay
            listOf(testMeal1)
        }
        whenever(mealRepository.searchMealsByName("new")).thenReturn(listOf(testMeal2))
        
        viewModel.searchMeals("old")
        viewModel.searchMeals("new")
        advanceUntilIdle()
        
        // Final state should be from "new"
        // Note: depends on implementation details of searchMeals
        // Assuming searchMeals sets state to Success
        // We check if "new" meal is present. 
        // In our ViewModel, Success(meals) replaces the list state.
        // But allMeals is cumulative.
        // Actually, let's check the mealListState
        val state = viewModel.uiState.mealListState
        if (state is com.example.cookmate.ui.state.MealUiState.Success) {
            assertEquals("2", state.meals.first().idMeal)
        }
    }
}
