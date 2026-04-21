package com.example.cookmate.data.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.data.db.dao.FavouriteMealDao
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import com.example.cookmate.data.model.Ingredient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RoomIntegrationTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: CookMateDatabase
    private lateinit var dao: FavouriteMealDao
    
    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CookMateDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.favouriteMealDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndRetrieveFavouriteMeal() = runBlocking {
        val ingredients = listOf(
            Ingredient("Pasta", "400g"),
            Ingredient("Eggs", "3")
        )
        val entity = FavouriteMealEntity(
            idMeal = "1",
            strMeal = "Carbonara",
            strMealThumb = "http://example.com/image.jpg",
            strCategory = "Pasta",
            strArea = "Italian",
            strInstructions = "Cook pasta...",
            ingredients = ingredients,
            addedAt = System.currentTimeMillis()
        )

        dao.insertFavourite(entity)
        val retrieved = dao.getFavouriteById("1")

        assertNotNull(retrieved)
        assertEquals("1", retrieved!!.idMeal)
        assertEquals("Carbonara", retrieved.strMeal)
        assertEquals(2, retrieved.ingredients.size)
        assertEquals("Pasta", retrieved.ingredients[0].name)
    }

    @Test
    fun testNoDuplicatesInFavourites() = runBlocking {
        val entity = FavouriteMealEntity(
            idMeal = "1",
            strMeal = "Carbonara",
            strMealThumb = "http://example.com/image.jpg",
            strCategory = "Pasta",
            strArea = "Italian",
            strInstructions = "Cook...",
            ingredients = emptyList(),
            addedAt = System.currentTimeMillis()
        )

        dao.insertFavourite(entity)
        dao.insertFavourite(entity.copy(addedAt = System.currentTimeMillis() + 1000))

        val allFavourites = dao.getAllFavouritesSync()

        assertEquals(1, allFavourites.size)
        assertEquals("1", allFavourites[0].idMeal)
    }

    @Test
    fun testRemoveFromFavourites() = runBlocking {
        val entity = FavouriteMealEntity(
            idMeal = "1",
            strMeal = "Carbonara",
            strMealThumb = "http://example.com/image.jpg",
            strCategory = "Pasta",
            strArea = "Italian",
            strInstructions = "Cook...",
            ingredients = emptyList(),
            addedAt = System.currentTimeMillis()
        )

        dao.insertFavourite(entity)

        val beforeDelete = dao.getAllFavouritesSync()
        assertEquals(1, beforeDelete.size)

        dao.deleteFavourite("1")

        val afterDelete = dao.getAllFavouritesSync()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testFavouritesOrderByAddedTime() = runBlocking {
        val now = System.currentTimeMillis()
        val meal1 = FavouriteMealEntity(
            idMeal = "1",
            strMeal = "First",
            strMealThumb = "",
            strCategory = "",
            strArea = "",
            strInstructions = "",
            ingredients = emptyList(),
            addedAt = now
        )
        val meal2 = FavouriteMealEntity(
            idMeal = "2",
            strMeal = "Second",
            strMealThumb = "",
            strCategory = "",
            strArea = "",
            strInstructions = "",
            ingredients = emptyList(),
            addedAt = now + 1000
        )

        dao.insertFavourite(meal1)
        dao.insertFavourite(meal2)

        val retrieved = dao.getAllFavouritesSync()

        assertEquals(2, retrieved.size)
        assertEquals("2", retrieved[0].idMeal)
        assertEquals("1", retrieved[1].idMeal)
    }
}
