package com.example.cookmate.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import com.example.cookmate.data.model.Ingredient
import com.example.cookmate.data.model.Meal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataLayerIntegrationTest {

    private lateinit var database: CookMateDatabase
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            CookMateDatabase::class.java
        ).setQueryExecutor { command -> command.run() }
        .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddMealToFavouritesAndObserve() = runTest(testDispatcher) {
        val testEntity = FavouriteMealEntity(
            idMeal = "52772",
            strMeal = "Teriyaki Chicken Casserole",
            strMealThumb = "file:///local/image.jpg",
            strCategory = "Chicken",
            strArea = "Japanese",
            strInstructions = "Cook it...",
            ingredients = listOf(Ingredient("Chicken", "1lb"), Ingredient("Rice", "1 cup")),
            addedAt = System.currentTimeMillis()
        )

        val dao = database.favouriteMealDao()

        withContext(Dispatchers.IO) {
            dao.addFavourite(testEntity)
        }

        val favourites = withContext(Dispatchers.IO) {
            dao.getAllFavourites().first()
        }

        assertEquals(1, favourites.size)
        val result = favourites[0]
        assertEquals(testEntity.idMeal, result.idMeal)
        assertEquals(testEntity.strMeal, result.strMeal)
        assertEquals(2, result.ingredients.size)
        assertEquals("Chicken", result.ingredients[0].name)
    }

    @Test
    fun testFlowUpdatesOnDeletion() = runTest(testDispatcher) {
        val dao = database.favouriteMealDao()
        val mealEntity = FavouriteMealEntity(
            idMeal = "1", 
            strMeal = "Test", 
            strMealThumb = "file:///local/image.jpg",
            strCategory = "",
            strArea = "",
            strInstructions = "",
            ingredients = emptyList(),
            addedAt = System.currentTimeMillis()
        )
        
        withContext(Dispatchers.IO) {
            dao.addFavourite(mealEntity)
        }
        
        val initialList = withContext(Dispatchers.IO) {
            dao.getAllFavourites().first()
        }
        assertEquals(1, initialList.size)

        withContext(Dispatchers.IO) {
            dao.removeFavouriteById("1")
        }
        
        val updatedList = withContext(Dispatchers.IO) {
            dao.getAllFavourites().first()
        }
        assertTrue(updatedList.isEmpty())
    }
}
