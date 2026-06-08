package com.example.data.db

import androidx.room.*
import com.example.data.model.MusicAccount
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // --- Accounts Queries ---
    @Query("SELECT * FROM music_accounts")
    fun getAllAccounts(): Flow<List<MusicAccount>>

    @Query("SELECT * FROM music_accounts WHERE serviceId = :id LIMIT 1")
    suspend fun getAccountById(id: String): MusicAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: MusicAccount)

    @Query("UPDATE music_accounts SET isLoggedIn = :isLoggedIn, username = :username WHERE serviceId = :serviceId")
    suspend fun updateLoginState(serviceId: String, isLoggedIn: Boolean, username: String)


    // --- Tracks Queries ---
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE serviceId = :serviceId")
    fun getTracksForService(serviceId: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): Track?

    @Query("SELECT * FROM tracks WHERE serviceId = :serviceId")
    suspend fun getTracksForServiceSync(serviceId: String): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)


    // --- Playback State Queries ---
    @Query("SELECT * FROM playback_state WHERE id = 1 LIMIT 1")
    fun getPlaybackState(): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM playback_state WHERE id = 1 LIMIT 1")
    suspend fun getPlaybackStateSync(): PlaybackStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackState(state: PlaybackStateEntity)

    @Update
    suspend fun updatePlaybackState(state: PlaybackStateEntity)

    @Query("UPDATE playback_state SET isPlaying = :isPlaying WHERE id = 1")
    suspend fun updatePlayPause(isPlaying: Boolean)

    @Query("UPDATE playback_state SET currentTrackId = :trackId WHERE id = 1")
    suspend fun updateCurrentTrack(trackId: String)

    @Query("UPDATE playback_state SET progressMs = :progressMs WHERE id = 1")
    suspend fun updateProgress(progressMs: Long)

    @Query("UPDATE playback_state SET activeServiceId = :serviceId, currentTrackId = :trackId WHERE id = 1")
    suspend fun updateActiveServiceAndTrack(serviceId: String, trackId: String)
}
