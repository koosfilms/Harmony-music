package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_accounts")
data class MusicAccount(
    @PrimaryKey val serviceId: String, // "youtube_music", "spotify", "tidal"
    val displayName: String,
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val avatarColorSeed: String = "#FF1F1F" // Hex code for procedural custom avatar
)
