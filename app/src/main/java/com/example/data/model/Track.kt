package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val serviceId: String, // "youtube_music", "spotify", "tidal"
    val coverGradientStart: String = "#FF5555",
    val coverGradientEnd: String = "#FF1111"
)
