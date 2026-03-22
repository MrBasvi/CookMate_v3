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
    
    /**
     * Интеграционный тест 1: Сохранение и чтение из Room
     * Данные должны корректно сохраняться и читаться из БД
     */
    @Test
    fun testSaveAndRetrieveFavouriteMeal() = runBlocking {
        // Arrange
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
        
        // Act
        dao.insertFavourite(entity)
        val retrieved = dao.getFavouriteById("1")
        
        // Assert
        assertNotNull(retrieved)
        assertEquals("1", retrieved!!.idMeal)
        assertEquals("Carbonara", retrieved.strMeal)
        assertEquals(2, retrieved.ingredients.size)
        assertEquals("Pasta", retrieved.ingredients[0].name)
    }
    
    /**
     * Интеграционный тест 2: Отсутствие дубликатов в избранном
     * При повторном сохранении того же рецепта не должно быть дубликатов
     */
    @Test
    fun testNoDuplicatesInFavourites() = runBlocking {
        // Arrange
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
        
        // Act - сохраняем дважды
        dao.insertFavourite(entity)
        dao.insertFavourite(entity.copy(addedAt = System.currentTimeMillis() + 1000))
        
        // Act - получаем все
        val allFavourites = dao.getAllFavouritesSync()
        
        // Assert - должно быть только одно
        assertEquals(1, allFavourites.size)
        assertEquals("1", allFavourites[0].idMeal)
    }
    
    /**
     * Интеграционный тест 3: Удаление из избранного
     * Удаленное блюдо не должно быть в избранном
     */
    @Test
    fun testRemoveFromFavourites() = runBlocking {
        // Arrange
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
        
        // Act
        val beforeDelete = dao.getAllFavouritesSync()
        assertEquals(1, beforeDelete.size)
        
        dao.deleteFavourite("1")
        
        // Assert
        val afterDelete = dao.getAllFavouritesSync()
        assertTrue(afterDelete.isEmpty())
    }
    
    /**
     * Нетривиальный интеграционный тест 4: Порядок избранных по времени добавления
     * Избранные должны возвращаться в порядке добавления (новые в начале)
     */
    @Test
    fun testFavouritesOrderByAddedTime() = runBlocking {
        // Arrange
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
        
        // Act
        dao.insertFavourite(meal1)
        dao.insertFavourite(meal2)
        
        val retrieved = dao.getAllFavouritesSync()
        
        // Assert - новое в начале
        assertEquals(2, retrieved.size)
        assertEquals("2", retrieved[0].idMeal)  // Добавлено позже
        assertEquals("1", retrieved[1].idMeal)
    }
}
