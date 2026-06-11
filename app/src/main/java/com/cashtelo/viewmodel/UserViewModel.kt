package com.cashtelo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.cashtelo.data.database.CashteloDatabase
import com.cashtelo.data.entity.User
import com.cashtelo.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    val user: LiveData<User>

    init {
        val userDao = CashteloDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
        user = repository.user
    }

    fun saveUser(updatedUser: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdate(updatedUser)
        }
    }
}
