package com.example.cookmate.ui.contract

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.ui.state.MealDetailUiState
import com.example.cookmate.ui.state.MealUiState
import com.example.cookmate.ui.viewmodel.CookMateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelContractTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mealRepository: MealRepository

    @Mock
    private lateinit var favouriteService: FavouriteMealService

    private lateinit var viewModel: CookMateViewModel

    private val testMeals = listOf(
        Meal(
            idMeal = "1",
            strMeal = "Pasta",
            strMealThumb = "http://example.com/1.jpg",
            strCategory = "Pasta",
            strArea = "Italian",
            strInstructions = "Cook pasta...",
            ingredients = listOf(Ingredient("Pasta", "400g"), Ingredient("Tomato Sauce", "200ml"))
        ),
        Meal(
            idMeal = "2",
            strMeal = "Pizza",
            strMealThumb = "http://example.com/2.jpg",
            strCategory = "Pizza",
            strArea = "Italian",
            strInstructions = "Make pizza...",
            ingredients = listOf(Ingredient("Dough", "500g"), Ingredient("Cheese", "200g"))
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(favouriteService.getAllFavourites()).thenReturn(flowOf(emptyList()))
        viewModel = CookMateViewModel(mealRepository, favouriteService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchThenNavigateToDetail() = runTest(testDispatcher) {
        val query = "pasta"
        whenever(mealRepository.searchMealsByName(query)).thenReturn(testMeals)
        whenever(mealRepository.getMealDetails("1")).thenReturn(testMeals[0])
        whenever(mealRepository.getMealDetails("2")).thenReturn(testMeals[1])

        viewModel.searchMeals(query)
        advanceUntilIdle()

        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val listState = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(2, listState.meals.size)

        val selectedMealId = listState.meals[0].idMeal
        viewModel.getMealDetails(selectedMealId)
        advanceUntilIdle()

        assertIs<MealDetailUiState.Success>(viewModel.uiState.mealDetailState)
        val detail = (viewModel.uiState.mealDetailState as MealDetailUiState.Success).meal
        assertEquals("1", detail.idMeal)
        assertEquals("Pasta", detail.strMeal)
        assertEquals(2, detail.ingredients.size)
    }

    @Test
    fun testErrorThenRetrySuccess() = runTest(testDispatcher) {
        val query = "pasta"
        whenever(mealRepository.searchMealsByName(query))
            .thenThrow(RuntimeException("Network error"))
            .thenReturn(testMeals)

        viewModel.searchMeals(query)
        advanceUntilIdle()
        assertIs<MealUiState.Error>(viewModel.uiState.mealListState)

        viewModel.retrySearch()
        advanceUntilIdle()

        verify(mealRepository, times(2)).searchMealsByName(query)
        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val state = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(2, state.meals.size)
    }

    @Test
    fun testStateTransitionsCorrectly() = runTest(testDispatcher) {
        val query = "pizza"
        whenever(mealRepository.searchMealsByName(query)).thenReturn(testMeals.filter { it.strMeal == "Pizza" })
        whenever(mealRepository.searchMealsByName("missing")).thenReturn(emptyList())

        assertIs<MealUiState.Empty>(viewModel.uiState.mealListState)

        viewModel.searchMeals(query)
        advanceUntilIdle()

        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val state = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(1, state.meals.size)
        assertEquals("Pizza", state.meals[0].strMeal)

        viewModel.searchMeals("missing")
        advanceUntilIdle()

        assertIs<MealUiState.Empty>(viewModel.uiState.mealListState)
        verify(mealRepository).searchMealsByName(query)
        verify(mealRepository).searchMealsByName("missing")
    }
}
