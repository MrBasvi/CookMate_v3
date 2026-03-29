package com.example.cookmate.ui.integration

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
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
import com.example.cookmate.data.model.Meal
import com.example.cookmate.data.repository.MealRepository
import com.example.cookmate.data.service.FavouriteMealService
import com.example.cookmate.navigation.CookMateNavHost
import com.example.cookmate.ui.viewmodel.CookMateViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiNavigationIntegrationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: CookMateDatabase
    private lateinit var viewModel: CookMateViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CookMateDatabase::class.java
        ).allowMainThreadQueries().build()

        val repository = MealRepository(FakeMealApiService())
        val favouriteService = FavouriteMealService(database.favouriteMealDao(), context)
        viewModel = CookMateViewModel(repository, favouriteService)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun listItemClick_opensDetailForCorrectId() {
        composeRule.setContent {
            CookMateNavHost(
                navController = rememberNavController(),
                viewModel = viewModel
            )
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("pizza")
        composeRule.onAllNodesWithText("Поиск")[0].performClick()
        composeRule.onNodeWithText("Pizza").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Pizza").assertIsDisplayed()
        composeRule.onNodeWithText("Pizza • Italian").assertIsDisplayed()
        composeRule.onNodeWithText("Как готовить").assertIsDisplayed()
        composeRule.onNodeWithText("Bake it").assertIsDisplayed()
    }

    private class FakeMealApiService : MealApiService {
        private val meals = listOf(
            RemoteMeal(
                idMeal = "1",
                strMeal = "Carbonara",
                strDrinkAlternate = null,
                strCategory = "Pasta",
                strArea = "Italian",
                strInstructions = "Boil pasta",
                strMealThumb = "file:///carbonara.jpg",
                strTags = null,
                strYoutube = null,
                strIngredient1 = "Pasta",
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
                strMeasure1 = "200g",
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
            ),
            RemoteMeal(
                idMeal = "2",
                strMeal = "Pizza",
                strDrinkAlternate = null,
                strCategory = "Pizza",
                strArea = "Italian",
                strInstructions = "Bake it",
                strMealThumb = "file:///pizza.jpg",
                strTags = null,
                strYoutube = null,
                strIngredient1 = "Dough",
                strIngredient2 = "Cheese",
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
                strMeasure2 = "100g",
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
        )

        override suspend fun searchMealsByName(name: String): MealResponse {
            val filteredMeals = meals.filter { it.strMeal.contains(name, ignoreCase = true) }
            return MealResponse(meals = filteredMeals.ifEmpty { null })
        }

        override suspend fun getMealDetails(id: String): MealDetailsResponse {
            return MealDetailsResponse(meals = meals.filter { it.idMeal == id }.ifEmpty { null })
        }

        override suspend fun getCategories(): CategoriesResponse {
            return CategoriesResponse(categories = emptyList<Category>())
        }

        override suspend fun getMealsByCategory(category: String): MealResponse {
            val filteredMeals = meals.filter { it.strCategory.equals(category, ignoreCase = true) }
            return MealResponse(meals = filteredMeals.ifEmpty { null })
        }
    }
}
