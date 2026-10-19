package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Query("SELECT * FROM list_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items ORDER BY createdAt DESC")
    fun getAllItemsSync(): List<ListItemEntity>

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY createdAt DESC")
    fun getItemsForList(listId: Long): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY createdAt DESC")
    suspend fun getItemsForListSync(listId: Long): List<ListItemEntity>

    @Query("SELECT * FROM list_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ListItemEntity?

    @Query("SELECT * FROM list_items WHERE id = :id LIMIT 1")
    fun observeItemById(id: Long): Flow<ListItemEntity?>

    @Query("SELECT * FROM list_items WHERE imageUri IS NOT NULL AND imageUri != '' ORDER BY updatedAt DESC")
    fun getItemsWithImages(): Flow<List<ListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ListItemEntity): Long

    @Update
    suspend fun updateItem(item: ListItemEntity)

    @Delete
    suspend fun deleteItem(item: ListItemEntity)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM list_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM list_items WHERE listId = :listId AND isChecked = 1")
    suspend fun deleteCheckedItemsInList(listId: Long)

    @Query("DELETE FROM list_items WHERE isChecked = 1 AND checkedAt <= :cutoffTime")
    suspend fun deleteOldCheckedItems(cutoffTime: Long)
}
