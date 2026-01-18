package xyz.zt.mindbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey val id: String,
    val serviceName: String,
    val accountEmail: String,
    val secretKey: String
)
