package com.example.cookmate.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.ui.state.MealUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CookMateViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mealRepository: MealRepository
    
    @Mock
    private lateinit var favouriteService: FavouriteMealService
    
    private lateinit var viewModel: CookMateViewModel
    
    private val favoritesFlow = MutableStateFlow<List<Meal>>(emptyList())
    
    private val testMeal = Meal(
        idMeal = "1",
        strMeal = "Test Meal",
        strMealThumb = "http://example.com/test.jpg",
        strCategory = "Dessert",
        strArea = "American",
        strInstructions = "Test instructions",
        ingredients = listOf(
            Ingredient("Test Ingredient", "1 cup")
        )
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
    fun testInitialState() {
        assertTrue(viewModel.uiState.mealListState is MealUiState.Empty)
        assertEquals("", viewModel.uiState.searchQuery)
        assertEquals(0, viewModel.uiState.allMeals.size)
    }

    @Test
    fun testSearchMeals_Success() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Success)
        val successState = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(1, successState.meals.size)
    }

    @Test
    fun testSearchMeals_Error() = runTest {
        whenever(mealRepository.searchMealsByName("test"))
            .thenThrow(RuntimeException("API Error"))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Error)
    }

    @Test
    fun testRetryAfterError() = runTest {
        whenever(mealRepository.searchMealsByName("test"))
            .thenThrow(RuntimeException("Network error"))
            .thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.mealListState is MealUiState.Error)
        
        viewModel.searchMeals("test")
        advanceUntilIdle()

        verify(mealRepository, times(2)).searchMealsByName("test")
        assertTrue(viewModel.uiState.mealListState is MealUiState.Success)
    }

    @Test
    fun testSearchMeals_EmptyResult() = runTest {
        whenever(mealRepository.searchMealsByName("xyz")).thenReturn(emptyList())
        
        viewModel.searchMeals("xyz")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Empty)
    }

    @Test
    fun testAllMeals_NoDuplicates() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertEquals(1, viewModel.uiState.allMeals.size)
    }

    @Test
    fun testRetryActuallyInitiatesNewRequest() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        verify(mealRepository, times(2)).searchMealsByName("test")
    }

    @Test
    fun testCancelOldRequest() = runTest {
        val slowMeal = testMeal.copy(idMeal = "1", strMeal = "Slow Meal")
        val fastMeal = testMeal.copy(idMeal = "2", strMeal = "Fast Meal")

        doAnswer {
            runBlocking {
                delay(200)
                listOf(slowMeal)
            }
        }.whenever(mealRepository).searchMealsByName(eq("slow"))
        whenever(mealRepository.searchMealsByName("fast")).thenReturn(listOf(fastMeal))

        viewModel.searchMeals("slow")
        advanceTimeBy(50)
        viewModel.searchMeals("fast")
        advanceUntilIdle()

        val state = viewModel.uiState.mealListState
        assertIs<MealUiState.Success>(state)
        assertEquals("2", state.meals.first().idMeal)
        verify(mealRepository, times(1)).searchMealsByName("slow")
        verify(mealRepository, times(1)).searchMealsByName("fast")
    }

    @Test
    fun testFavoritesEmissionSequence() = runTest {
        val m1 = testMeal.copy(idMeal = "1")
        val m2 = testMeal.copy(idMeal = "2")

        favoritesFlow.value = listOf(m1)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.favoriteMeals.size)

        favoritesFlow.value = listOf(m1, m2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.favoriteMeals.size)
    }
}
