package com.cashtelo.data.repository

import androidx.lifecycle.LiveData
import com.cashtelo.data.dao.UserDao
import com.cashtelo.data.entity.User

class UserRepository(private val userDao: UserDao) {
    val user: LiveData<User> = userDao.getUser()

    suspend fun insertOrUpdate(user: User) {
        userDao.insertOrUpdate(user)
    }
}
