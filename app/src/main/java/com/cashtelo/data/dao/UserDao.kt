package com.cashtelo.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: com.cashtelo.data.entity.User)

    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): LiveData<com.cashtelo.data.entity.User>
}
