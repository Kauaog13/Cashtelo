package com.cashtelo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1, // Fixamos em 1 para garantir que só exista um usuário no app
    val name: String,
    val password: String = "",
    val useBiometrics: Boolean = false,
    val avatarUri: String? = null
)
