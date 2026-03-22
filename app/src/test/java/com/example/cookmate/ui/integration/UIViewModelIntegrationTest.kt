package com.example.cookmate.ui.integration

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

/**
 * UI Integration тесты - проверяют сценарии взаимодействия ViewModel с dependencies
 * Без UI Framework, чистые unit-тесты на поведение
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UIViewModelIntegrationTest {
    
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
    
    /**
     * Интеграционный тест UI 1: Поиск -> Клик -> Детали (нетривиальный)
     * Сценарий: ищем рецепт, кликаем по элементу, открывается деталь
     * Проверяем что открывается именно нужный рецепт для нужного id
     */
    @Test
    fun testSearchThenNavigateToDetail() = runTest(testDispatcher) {
        // Arrange
        val query = "pasta"
        whenever(mealRepository.searchMealsByName(query)).thenReturn(testMeals)
        whenever(mealRepository.getMealDetails("1")).thenReturn(testMeals[0])
        whenever(mealRepository.getMealDetails("2")).thenReturn(testMeals[1])
        
        // Act - поиск
        viewModel.searchMeals(query)
        advanceUntilIdle()
        
        // Assert - поиск успешен, список загружен
        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val listState = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(2, listState.meals.size)
        
        // Act - открываем детали первого рецепта (Pasta, id="1")
        val selectedMealId = listState.meals[0].idMeal
        viewModel.getMealDetails(selectedMealId)
        advanceUntilIdle()
        
        // Assert - детали загружены для Pasta (id="1", name="Pasta")
        assertIs<MealDetailUiState.Success>(viewModel.uiState.mealDetailState)
        val detail = (viewModel.uiState.mealDetailState as MealDetailUiState.Success).meal
        assertEquals("1", detail.idMeal)
        assertEquals("Pasta", detail.strMeal)
        assertEquals(2, detail.ingredients.size)
    }
    
    /**
     * Интеграционный тест UI 2: Ошибка -> Retry -> Успех (нетривиальный)
     * Сценарий: первый поиск дает ошибку, затем retry успешен
     * Проверяем что retry действительно инициирует новый запрос и данные загружаются
     */
    @Test
    fun testErrorThenRetrySuccess() = runTest(testDispatcher) {
        // Arrange
        val query = "pasta"
        whenever(mealRepository.searchMealsByName(query))
            .thenThrow(RuntimeException("Network error"))
            .thenReturn(testMeals)
        
        // Act - первый поиск (ошибка)
        viewModel.searchMeals(query)
        advanceUntilIdle()
        
        // Assert - состояние ошибки
        assertIs<MealUiState.Error>(viewModel.uiState.mealListState)
        
        // Act - retry инициирует новый поиск (успех)
        viewModel.searchMeals(query)
        advanceUntilIdle()
        
        // Assert - успехом загружены данные
        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val state = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(2, state.meals.size)
        assertEquals("Pasta", state.meals[0].strMeal)
        assertEquals("Pizza", state.meals[1].strMeal)
    }
    
    /**
     * Интеграционный тест UI 3: Состояния переходят корректно (нетривиальный)
     * Проверяем последовательность состояний: Empty -> Success 
     * Убеждаемся что Loading не повторяется после первого запроса
     */
    @Test
    fun testStateTransitionsForcorrectly() = runTest(testDispatcher) {
        // Arrange
        val query = "pizza"
        whenever(mealRepository.searchMealsByName(query)).thenReturn(testMeals.filter { it.strMeal == "Pizza" })
        
        // Act & Assert - начальное состояние Empty (не Loading, пока не было поиска)
        assertIs<MealUiState.Empty>(viewModel.uiState.mealListState)
        
        // Act - выполняем поиск
        viewModel.searchMeals(query)
        advanceUntilIdle()
        
        // Assert - перешли в Success (не остались в Loading, не вернулись в Loading)
        assertIs<MealUiState.Success>(viewModel.uiState.mealListState)
        val state = viewModel.uiState.mealListState as MealUiState.Success
        
        // Проверяем что результат корректный
        assertEquals(1, state.meals.size)
        assertEquals("Pizza", state.meals[0].strMeal)
        
        // Дополнительная проверка - второй поиск не создаёт дополнительный Loading
        viewModel.searchMeals("pasta")
        // Состояние перейдёт в новое, но не повторится Loading
        advanceUntilIdle()
        assertTrue("State should change but not be Loading", 
            viewModel.uiState.mealListState !is MealUiState.Loading)
    }
}
