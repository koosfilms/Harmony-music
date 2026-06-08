package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1, // Single row state
    val activeServiceId: String = "spotify", // default to spotify
    val isPlaying: Boolean = false,
    val currentTrackId: String = "spotify_track_1",
    val progressMs: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false
)
