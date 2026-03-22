package com.example.cookmate.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cookmate.data.db.entity.FavouriteMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteMealDao {
    
    @Query("SELECT * FROM favourite_meals ORDER BY addedAt DESC")
    fun getAllFavourites(): Flow<List<FavouriteMealEntity>>
    
    @Query("SELECT * FROM favourite_meals ORDER BY addedAt DESC")
    suspend fun getAllFavouritesSync(): List<FavouriteMealEntity>
    
    @Query("SELECT * FROM favourite_meals WHERE idMeal = :idMeal")
    suspend fun getFavourite(idMeal: String): FavouriteMealEntity?
    
    @Query("SELECT * FROM favourite_meals WHERE idMeal = :idMeal")
    suspend fun getFavouriteById(idMeal: String): FavouriteMealEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(meal: FavouriteMealEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(meal: FavouriteMealEntity)
    
    @Delete
    suspend fun removeFavourite(meal: FavouriteMealEntity)
    
    @Query("DELETE FROM favourite_meals WHERE idMeal = :idMeal")
    suspend fun removeFavouriteById(idMeal: String)
    
    @Query("DELETE FROM favourite_meals WHERE idMeal = :idMeal")
    suspend fun deleteFavourite(idMeal: String)
}
