package com.example.cookmate.data.repository

import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.example.cookmate.data.api.CategoriesResponse
import com.example.cookmate.data.api.Category
import com.example.cookmate.data.api.MealApiService
import com.example.cookmate.data.api.MealDetailsResponse
import com.example.cookmate.data.api.MealResponse
import com.example.cookmate.data.api.RemoteMeal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MealRepositoryFlowTest {

    @Test
    fun repositoryFlow_emitsApiResultThenCompletes() = runTest {
        val repository = MealRepository(FakeMealApiService())

        repository.searchMealsByNameFlow("pizza").test {
            val emission = awaitItem()
            assertEquals(listOf("2"), emission.map { it.idMeal })
            assertEquals("Pizza", emission.single().strMeal)
            awaitComplete()
        }
    }

    @Test
    fun repositoryFlow_emptyApiResponseEmitsEmptyListThenCompletes() = runTest {
        val repository = MealRepository(FakeMealApiService())

        repository.searchMealsByNameFlow("missing").test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun flatMapLatest_dropsSlowOldRepositoryResult() = runTest {
        val api = FakeMealApiService(oldDelayMs = 1_000)
        val repository = MealRepository(api)
        val queries = MutableSharedFlow<String>()
        turbineScope {
            val results = queries
                .flatMapLatest { query -> repository.searchMealsByNameFlow(query) }
                .testIn(backgroundScope)
            runCurrent()

            queries.emit("old")
            advanceTimeBy(100)
            queries.emit("new")

            val emission = results.awaitItem()
            assertEquals(listOf("2"), emission.map { it.idMeal })
            assertEquals(1, api.oldRequestStarted)
            assertEquals(1, api.newRequestStarted)

            results.expectNoEvents()
            results.cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeMealApiService(
        private val oldDelayMs: Long = 0
    ) : MealApiService {
        var oldRequestStarted = 0
        var newRequestStarted = 0

        override suspend fun searchMealsByName(name: String): MealResponse {
            return when (name) {
                "old" -> {
                    oldRequestStarted++
                    delay(oldDelayMs)
                    MealResponse(listOf(remoteMeal("1", "Old Result")))
                }
                "new", "pizza" -> {
                    newRequestStarted++
                    MealResponse(listOf(remoteMeal("2", "Pizza")))
                }
                else -> MealResponse(null)
            }
        }

        override suspend fun getMealDetails(id: String): MealDetailsResponse =
            MealDetailsResponse(listOf(remoteMeal(id, "Meal $id")))

        override suspend fun getCategories(): CategoriesResponse =
            CategoriesResponse(emptyList<Category>())

        override suspend fun getMealsByCategory(category: String): MealResponse =
            searchMealsByName(category)

        private fun remoteMeal(id: String, name: String) = RemoteMeal(
            idMeal = id,
            strMeal = name,
            strDrinkAlternate = null,
            strCategory = "Test",
            strArea = "Test",
            strInstructions = "Cook it",
            strMealThumb = "file:///meal_$id.jpg",
            strTags = null,
            strYoutube = null,
            strIngredient1 = "Ingredient",
            strIngredient2 = null,
            strIngredient3 = null,
            strIngredient4 = null,
            strIngredient5 = null,
            strIngredient6 = null,
            strIngredient7 = null,
            strIngredient8 = null,
            strIngredient9 = null,
            strIngredient10 = null,
            strIngredient11 = null,
            strIngredient12 = null,
            strIngredient13 = null,
            strIngredient14 = null,
            strIngredient15 = null,
            strIngredient16 = null,
            strIngredient17 = null,
            strIngredient18 = null,
            strIngredient19 = null,
            strIngredient20 = null,
            strMeasure1 = "1",
            strMeasure2 = null,
            strMeasure3 = null,
            strMeasure4 = null,
            strMeasure5 = null,
            strMeasure6 = null,
            strMeasure7 = null,
            strMeasure8 = null,
            strMeasure9 = null,
            strMeasure10 = null,
            strMeasure11 = null,
            strMeasure12 = null,
            strMeasure13 = null,
            strMeasure14 = null,
            strMeasure15 = null,
            strMeasure16 = null,
            strMeasure17 = null,
            strMeasure18 = null,
            strMeasure19 = null,
            strMeasure20 = null
        )
    }
}
