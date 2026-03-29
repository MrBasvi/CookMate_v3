package com.example.cookmate.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
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
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun testFavoritesFlowEmissions() = runTest(testDispatcher) {
        val emissions = mutableListOf(viewModel.uiState.favorites)
        advanceUntilIdle()

        favoritesFlow.value = listOf(testMeal1)
        advanceUntilIdle()
        emissions += viewModel.uiState.favorites

        favoritesFlow.value = listOf(testMeal1, testMeal2)
        advanceUntilIdle()
        emissions += viewModel.uiState.favorites

        assertEquals(
            listOf(emptyList(), listOf("1"), listOf("1", "2")),
            emissions
        )
    }

    @Test
    fun testNoExtraEmissions() = runTest(testDispatcher) {
        assertEquals(emptyList(), viewModel.uiState.favorites)
        advanceUntilIdle()

        favoritesFlow.value = listOf(testMeal1)
        advanceUntilIdle()
        assertEquals(listOf("1"), viewModel.uiState.favorites)

        favoritesFlow.value = listOf(testMeal1)
        advanceUntilIdle()
        assertEquals(listOf("1"), viewModel.uiState.favorites)
        assertEquals(1, viewModel.uiState.favoriteMeals.size)
    }

    @Test
    fun testOldRequestDoesNotOverrideNewResult() = runTest(testDispatcher) {
        doAnswer {
            runBlocking {
                delay(200)
                listOf(testMeal1)
            }
        }.whenever(mealRepository).searchMealsByName(eq("old"))
        whenever(mealRepository.searchMealsByName("new")).thenReturn(listOf(testMeal2))

        viewModel.searchMeals("old")
        advanceTimeBy(50)
        viewModel.searchMeals("new")
        advanceUntilIdle()

        val state = viewModel.uiState.mealListState
        assertIs<com.example.cookmate.ui.state.MealUiState.Success>(state)
        assertEquals("2", state.meals.first().idMeal)
        verify(mealRepository, times(1)).searchMealsByName("old")
        verify(mealRepository, times(1)).searchMealsByName("new")
    }
}
