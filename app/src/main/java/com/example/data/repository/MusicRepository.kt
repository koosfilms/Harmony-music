package com.example.data.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.MusicDao
import com.example.data.model.MusicAccount
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MusicRepository(
    private val musicDao: MusicDao,
    private val context: Context
) {
    val allAccounts: Flow<List<MusicAccount>> = musicDao.getAllAccounts()
    val playbackState: Flow<PlaybackStateEntity?> = musicDao.getPlaybackState()

    fun getTracksForService(serviceId: String): Flow<List<Track>> {
        return musicDao.getTracksForService(serviceId)
    }

    suspend fun getTrackById(trackId: String): Track? {
        return musicDao.getTrackById(trackId)
    }

    suspend fun seedDatabaseIfNeeded() {
        try {
            val existingAccounts = musicDao.getAllAccounts().firstOrNull() ?: emptyList()
            if (existingAccounts.isEmpty()) {
                Log.d("MusicRepository", "Seeding database with initial music accounts and tracks...")

                // 1. Core Accounts
                val seedAccounts = listOf(
                    MusicAccount("spotify", "Spotify Premium", isLoggedIn = true, username = "@sreekumar.spotify", avatarColorSeed = "#1DB954"),
                    MusicAccount("youtube_music", "YouTube Music Red", isLoggedIn = false, username = "", avatarColorSeed = "#FF0000"),
                    MusicAccount("tidal", "Tidal Lossless HiFi", isLoggedIn = false, username = "", avatarColorSeed = "#00D2FF")
                )
                seedAccounts.forEach { musicDao.insertAccount(it) }

                // 2. Tracks catalog for each service
                val seedTracks = listOf(
                    // Spotify Tracks - Green energetic vibes
                    Track(
                        id = "spotify_track_1",
                        title = "Blinding Lights",
                        artist = "The Weeknd",
                        album = "After Hours",
                        durationMs = 201000,
                        serviceId = "spotify",
                        coverGradientStart = "#1DB954",
                        coverGradientEnd = "#115E2E"
                    ),
                    Track(
                        id = "spotify_track_2",
                        title = "Ocean Eyes",
                        artist = "Billie Eilish",
                        album = "Don't Smile at Me",
                        durationMs = 200000,
                        serviceId = "spotify",
                        coverGradientStart = "#44A1A0",
                        coverGradientEnd = "#0D5C75"
                    ),
                    Track(
                        id = "spotify_track_3",
                        title = "Starlight Symphony",
                        artist = "Cosmic Echo",
                        album = "Beyond the Void",
                        durationMs = 240000,
                        serviceId = "spotify",
                        coverGradientStart = "#8E2DE2",
                        coverGradientEnd = "#4A00E0"
                    ),

                    // YouTube Music Tracks - Red passionate and chill visualizers
                    Track(
                        id = "yt_track_1",
                        title = "Lofi Sunset Drive",
                        artist = "Chillhop Beats",
                        album = "Dusty Anthems",
                        durationMs = 180000,
                        serviceId = "youtube_music",
                        coverGradientStart = "#FF0000",
                        coverGradientEnd = "#800000"
                    ),
                    Track(
                        id = "yt_track_2",
                        title = "Neon Midnight",
                        artist = "Retro Synth",
                        album = "Cyber Runner",
                        durationMs = 192000,
                        serviceId = "youtube_music",
                        coverGradientStart = "#FF416C",
                        coverGradientEnd = "#FF4B2B"
                    ),
                    Track(
                        id = "yt_track_3",
                        title = "Bollywood Melodies Mix",
                        artist = "Arijit Singh & Shreya",
                        album = "Studio Acoustic Sessions",
                        durationMs = 264000,
                        serviceId = "youtube_music",
                        coverGradientStart = "#F2994A",
                        coverGradientEnd = "#F2C94C"
                    ),

                    // Tidal Tracks - Deep dynamic cyan fidelity tunes
                    Track(
                        id = "tidal_track_1",
                        title = "Kind of Blue (Master)",
                        artist = "Miles Davis",
                        album = "Classic Jazz Remastered",
                        durationMs = 322000,
                        serviceId = "tidal",
                        coverGradientStart = "#00D2FF",
                        coverGradientEnd = "#002244"
                    ),
                    Track(
                        id = "tidal_track_2",
                        title = "Cello Suite No. 1 in G Major",
                        artist = "Yo-Yo Ma",
                        album = "The Bach Cello Suites",
                        durationMs = 148000,
                        serviceId = "tidal",
                        coverGradientStart = "#3949AB",
                        coverGradientEnd = "#1A237E"
                    ),
                    Track(
                        id = "tidal_track_3",
                        title = "Acoustic Horizon Lossless",
                        artist = "Neil Young",
                        album = "Harvest Wind",
                        durationMs = 212000,
                        serviceId = "tidal",
                        coverGradientStart = "#D4AF37",
                        coverGradientEnd = "#5D4037"
                    )
                )
                musicDao.insertTracks(seedTracks)

                // 3. Playback State
                val initialState = PlaybackStateEntity(
                    id = 1,
                    activeServiceId = "spotify",
                    isPlaying = false,
                    currentTrackId = "spotify_track_1",
                    progressMs = 0L,
                    isShuffle = false,
                    isRepeat = false
                )
                musicDao.insertPlaybackState(initialState)
                Log.d("MusicRepository", "Seeding completed successfully!")
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error seeding database: ${e.message}", e)
        }
    }

    suspend fun updateLoginState(serviceId: String, isLoggedIn: Boolean, username: String) {
        musicDao.updateLoginState(serviceId, isLoggedIn, username)
        
        // If logging out active service, switch active service to first available logged-in service or default
        val state = musicDao.getPlaybackStateSync()
        if (state?.activeServiceId == serviceId && !isLoggedIn) {
            val accounts = musicDao.getAllAccounts().firstOrNull() ?: emptyList()
            val nextActive = accounts.firstOrNull { it.isLoggedIn && it.serviceId != serviceId }?.serviceId ?: "spotify"
            changeActiveService(nextActive)
        } else {
            notifyWidgetUpdate()
        }
    }

    suspend fun updatePlayPause(isPlaying: Boolean) {
        musicDao.updatePlayPause(isPlaying)
        notifyWidgetUpdate()
    }

    suspend fun updateProgress(progressMs: Long) {
        musicDao.updateProgress(progressMs)
    }

    suspend fun toggleShuffle() {
        val state = musicDao.getPlaybackStateSync() ?: return
        musicDao.updatePlaybackState(state.copy(isShuffle = !state.isShuffle))
    }

    suspend fun toggleRepeat() {
        val state = musicDao.getPlaybackStateSync() ?: return
        musicDao.updatePlaybackState(state.copy(isRepeat = !state.isRepeat))
    }

    suspend fun changeActiveService(serviceId: String) {
        val serviceTracks = musicDao.getTracksForServiceSync(serviceId)
        val firstTrackId = serviceTracks.firstOrNull()?.id ?: when (serviceId) {
            "spotify" -> "spotify_track_1"
            "youtube_music" -> "yt_track_1"
            else -> "tidal_track_1"
        }
        
        val currentState = musicDao.getPlaybackStateSync()
        if (currentState != null) {
            musicDao.updatePlaybackState(currentState.copy(
                activeServiceId = serviceId,
                currentTrackId = firstTrackId,
                progressMs = 0L,
                isPlaying = false
            ))
        } else {
            musicDao.insertPlaybackState(PlaybackStateEntity(
                activeServiceId = serviceId,
                currentTrackId = firstTrackId,
                isPlaying = false,
                progressMs = 0L
            ))
        }
        notifyWidgetUpdate()
    }

    suspend fun playTrack(trackId: String, serviceId: String) {
        val currentState = musicDao.getPlaybackStateSync()
        if (currentState != null) {
            musicDao.updatePlaybackState(currentState.copy(
                activeServiceId = serviceId,
                currentTrackId = trackId,
                progressMs = 0L,
                isPlaying = true
            ))
        }
        notifyWidgetUpdate()
    }

    suspend fun skipToNext() {
        val state = musicDao.getPlaybackStateSync() ?: return
        val tracks = musicDao.getTracksForServiceSync(state.activeServiceId)
        if (tracks.isEmpty()) return
        
        val currentIndex = tracks.indexOfFirst { it.id == state.currentTrackId }
        val nextIndex = if (state.isShuffle) {
            (tracks.indices).random()
        } else {
            (currentIndex + 1) % tracks.size
        }
        
        val nextTrack = tracks[nextIndex]
        musicDao.updatePlaybackState(state.copy(
            currentTrackId = nextTrack.id,
            progressMs = 0L
        ))
        notifyWidgetUpdate()
    }

    suspend fun skipToPrevious() {
        val state = musicDao.getPlaybackStateSync() ?: return
        val tracks = musicDao.getTracksForServiceSync(state.activeServiceId)
        if (tracks.isEmpty()) return
        
        val currentIndex = tracks.indexOfFirst { it.id == state.currentTrackId }
        val prevIndex = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
        
        val prevTrack = tracks[prevIndex]
        musicDao.updatePlaybackState(state.copy(
            currentTrackId = prevTrack.id,
            progressMs = 0L
        ))
        notifyWidgetUpdate()
    }

    fun notifyWidgetUpdate() {
        try {
            val intent = Intent().apply {
                component = ComponentName(context, "com.example.widget.MusicSwitcherWidgetReceiver")
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(ComponentName(context, "com.example.widget.MusicSwitcherWidgetReceiver"))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
            Log.d("MusicRepository", "Dispatched Widget Update Broadcast for ids: ${ids.size}")
        } catch (e: Exception) {
            Log.e("MusicRepository", "Could not dispatch widget broadcast: ${e.message}")
        }
    }
}
