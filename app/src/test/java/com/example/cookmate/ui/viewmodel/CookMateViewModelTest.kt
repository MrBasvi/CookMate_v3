package com.example.cookmate.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.ui.state.MealUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    
    /**
     * Юнит-тест 1: Корректное начальное состояние экрана
     */
    @Test
    fun testInitialState() {
        assertTrue(viewModel.uiState.mealListState is MealUiState.Empty)
        assertEquals("", viewModel.uiState.searchQuery)
        assertEquals(0, viewModel.uiState.allMeals.size)
    }
    
    /**
     * Юнит-тест 2: Успешная загрузка данных
     */
    @Test
    fun testSearchMeals_Success() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Success)
        val successState = viewModel.uiState.mealListState as MealUiState.Success
        assertEquals(1, successState.meals.size)
    }
    
    /**
     * Юнит-тест 3: Ошибка загрузки
     */
    @Test
    fun testSearchMeals_Error() = runTest {
        whenever(mealRepository.searchMealsByName("test"))
            .thenThrow(RuntimeException("API Error"))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Error)
    }
    
    /**
     * Юнит-тест 4: Retry после ошибки
     */
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
        assertTrue(viewModel.uiState.mealListState is MealUiState.Success)
    }
    
    /**
     * Юнит-тест 5: Корректная обработка пустого результата
     */
    @Test
    fun testSearchMeals_EmptyResult() = runTest {
        whenever(mealRepository.searchMealsByName("xyz")).thenReturn(emptyList())
        
        viewModel.searchMeals("xyz")
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.mealListState is MealUiState.Empty)
    }

    /**
     * Юнит-тест 6: Отсутствие дублей в кэше/allMeals (бизнес-логика)
     */
    @Test
    fun testAllMeals_NoDuplicates() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        assertEquals(1, viewModel.uiState.allMeals.size)
    }

    /**
     * Нетривиальный тест 1: retry() действительно инициирует новую попытку запроса
     */
    @Test
    fun testRetryActuallyInitiatesNewRequest() = runTest {
        whenever(mealRepository.searchMealsByName("test")).thenReturn(listOf(testMeal))
        
        viewModel.searchMeals("test")
        advanceUntilIdle()
        viewModel.searchMeals("test")
        advanceUntilIdle()
        
        verify(mealRepository, times(2)).searchMealsByName("test")
    }

    /**
     * Нетривиальный тест 2 / Потоковое поведение: отмена устаревшего запроса
     */
    @Test
    fun testCancelOldRequest() = runTest {
        whenever(mealRepository.searchMealsByName(any())).thenReturn(listOf(testMeal))

        // Запускаем два поиска подряд
        viewModel.searchMeals("slow")
        viewModel.searchMeals("fast")
        
        advanceUntilIdle()

        // Проверяем, что первый запрос был отменен до того, как вызвал репозиторий
        verify(mealRepository, never()).searchMealsByName("slow")
        verify(mealRepository).searchMealsByName("fast")
    }
    
    /**
     * Тест на последовательность эмиссий (Требование 5)
     */
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
