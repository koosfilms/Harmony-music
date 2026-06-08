package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.MusicAccount
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Observe Room dynamic entities
    val accounts: StateFlow<List<MusicAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackStateEntity?> = repository.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dynamic tracks flow depending on active service
    val activeTracks: StateFlow<List<Track>> = playbackState
        .flatMapLatest { state ->
            val svc = state?.activeServiceId ?: "spotify"
            repository.getTracksForService(svc)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active track details
    val activeTrack: StateFlow<Track?> = combine(playbackState, activeTracks) { state, tracks ->
        tracks.find { it.id == state?.currentTrackId } ?: tracks.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Simulated progress timer job
    private var progressJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Initial seed
            repository.seedDatabaseIfNeeded()
            _isLoading.value = false
            
            // Monitor playback changes to start/stop the ticker
            playbackState.collectLatest { state ->
                if (state?.isPlaying == true) {
                    startProgressTicker()
                } else {
                    stopProgressTicker()
                }
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000L)
                val state = playbackState.value ?: continue
                val track = activeTrack.value ?: continue
                
                val nextProgress = state.progressMs + 1000L
                if (nextProgress >= track.durationMs) {
                    // Track finished - skip to next!
                    repository.skipToNext()
                } else {
                    repository.updateProgress(nextProgress)
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    // --- User Actions ---
    
    fun togglePlayPause() {
        viewModelScope.launch(Dispatchers.IO) {
            val playing = playbackState.value?.isPlaying ?: false
            repository.updatePlayPause(!playing)
        }
    }

    fun skipToNext() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.skipToNext()
        }
    }

    fun skipToPrevious() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.skipToPrevious()
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleShuffle()
        }
    }

    fun toggleRepeat() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleRepeat()
        }
    }

    fun selectTrack(trackId: String, serviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.playTrack(trackId, serviceId)
        }
    }

    fun switchActiveService(serviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.changeActiveService(serviceId)
        }
    }

    fun simulateLogin(serviceId: String, username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val displayTitle = when (serviceId) {
                "spotify" -> "Spotify Premium"
                "youtube_music" -> "YouTube Music Red"
                else -> "Tidal Lossless HiFi"
            }
            repository.updateLoginState(serviceId, true, username)
            // Logged in! Auto-switch active service to this newly connected account.
            repository.changeActiveService(serviceId)
        }
    }

    fun simulateLogout(serviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLoginState(serviceId, false, "")
        }
    }

    fun seekTo(progressMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProgress(progressMs)
            repository.notifyWidgetUpdate()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTicker()
    }
}

// ViewModel Factory
class MusicViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = MusicRepository(database.musicDao(), application)
            return MusicViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
