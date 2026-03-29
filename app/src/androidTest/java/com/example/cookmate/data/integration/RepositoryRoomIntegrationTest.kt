package com.example.cookmate.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cookmate.data.api.CategoriesResponse
import com.example.cookmate.data.api.Category
import com.example.cookmate.data.api.MealApiService
import com.example.cookmate.data.api.MealDetailsResponse
import com.example.cookmate.data.api.MealResponse
import com.example.cookmate.data.api.RemoteMeal
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryRoomIntegrationTest {

    private lateinit var database: CookMateDatabase
    private lateinit var service: FavouriteMealService
    private lateinit var repository: MealRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            CookMateDatabase::class.java
        ).allowMainThreadQueries().build()

        service = FavouriteMealService(database.favouriteMealDao(), context)
        repository = MealRepository(FakeMealApiService())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun repositoryResult_canBePersistedAndReadFromRoom() = runBlocking {
        val meals = repository.searchMealsByName("chicken")
        assertEquals(1, meals.size)
        assertEquals("52772", meals.first().idMeal)
        assertEquals("Chicken Handi", meals.first().strMeal)
        assertEquals(
            listOf(Ingredient("Chicken", "1kg"), Ingredient("Onion", "1")),
            meals.first().ingredients
        )

        service.addFavourite(meals.first())

        val stored = service.getAllFavourites().first()
        assertEquals(1, stored.size)
        assertEquals("52772", stored.first().idMeal)
        assertEquals("Chicken Handi", stored.first().strMeal)
        assertEquals(meals.first().ingredients, stored.first().ingredients)
        assertTrue(stored.first().strMealThumb.startsWith("file://"))
    }

    private class FakeMealApiService : MealApiService {
        override suspend fun searchMealsByName(name: String): MealResponse {
            val meals = listOf(createChickenHandi()).filter {
                it.strMeal.contains(name, ignoreCase = true) ||
                    it.strCategory.contains(name, ignoreCase = true)
            }
            return MealResponse(meals = meals.ifEmpty { null })
        }

        override suspend fun getMealDetails(id: String): MealDetailsResponse {
            val meal = createChickenHandi().takeIf { it.idMeal == id }
            return MealDetailsResponse(meals = meal?.let(::listOf))
        }

        override suspend fun getCategories(): CategoriesResponse {
            return CategoriesResponse(categories = emptyList<Category>())
        }

        override suspend fun getMealsByCategory(category: String): MealResponse {
            return searchMealsByName(category)
        }

        private fun createChickenHandi() = RemoteMeal(
            idMeal = "52772",
            strMeal = "Chicken Handi",
            strDrinkAlternate = null,
            strCategory = "Chicken",
            strArea = "Indian",
            strInstructions = "Cook it",
            strMealThumb = "file:///fake.jpg",
            strTags = null,
            strYoutube = null,
            strIngredient1 = "Chicken",
            strIngredient2 = "Onion",
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
            strMeasure1 = "1kg",
            strMeasure2 = "1",
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
