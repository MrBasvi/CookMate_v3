package com.example.cookmate.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cookmate.data.db.CookMateDatabase
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import com.example.cookmate.data.model.Ingredient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

@RunWith(AndroidJUnit4::class)
class DataLayerIntegrationTest {

    private lateinit var database: CookMateDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CookMateDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor { command -> command.run() }
            .setTransactionExecutor { command -> command.run() }
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAddMealToFavouritesAndObserve() = runBlocking {
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
    fun testFlowUpdatesOnDeletion() = runBlocking {
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

        val emissions = Collections.synchronizedList(mutableListOf<List<FavouriteMealEntity>>())
        val initialObserved = CompletableDeferred<Unit>()
        val insertObserved = CompletableDeferred<Unit>()
        val deleteObserved = CompletableDeferred<Unit>()

        val observation = launch(Dispatchers.IO) {
            dao.getAllFavourites().collect { current ->
                emissions += current

                if (current.isEmpty() && emissions.size == 1) {
                    initialObserved.complete(Unit)
                }
                if (current.any { it.idMeal == "1" }) {
                    insertObserved.complete(Unit)
                }
                if (insertObserved.isCompleted && current.isEmpty()) {
                    deleteObserved.complete(Unit)
                }
            }
        }

        withTimeout(2_000) { initialObserved.await() }

        withContext(Dispatchers.IO) {
            dao.addFavourite(mealEntity)
        }
        withTimeout(2_000) { insertObserved.await() }

        withContext(Dispatchers.IO) {
            dao.removeFavouriteById("1")
        }
        withTimeout(2_000) { deleteObserved.await() }
        observation.cancelAndJoin()

        assertEquals(0, emissions.first().size)
        assertTrue(emissions.any { it.singleOrNull()?.idMeal == "1" })
        assertTrue(emissions.last().isEmpty())
    }
}
