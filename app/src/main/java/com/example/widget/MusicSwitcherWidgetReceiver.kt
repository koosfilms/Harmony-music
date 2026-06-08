package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.MusicAccount
import com.example.data.model.PlaybackStateEntity
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MusicSwitcherWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.example.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.example.widget.ACTION_NEXT"
        const val ACTION_SWITCH_SERVICE = "com.example.widget.ACTION_SWITCH_SERVICE"
        const val EXTRA_SERVICE_ID = "com.example.widget.EXTRA_SERVICE_ID"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Run update query on a background thread using coroutines
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.musicDao()
                val state = dao.getPlaybackState().firstOrNull()
                val activeTrack = state?.currentTrackId?.let { dao.getTrackById(it) }
                val accounts = dao.getAllAccounts().firstOrNull() ?: emptyList()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_music_switcher)
                    updateWidgetUI(context, views, state, activeTrack, accounts)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("MusicWidget", "Error updating widgets: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.d("MusicWidget", "onReceive action triggered: $action")

        if (action == ACTION_PLAY_PAUSE || action == ACTION_PREV || action == ACTION_NEXT || action == ACTION_SWITCH_SERVICE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.musicDao()
                    val state = dao.getPlaybackStateSync() ?: return@launch

                    when (action) {
                        ACTION_PLAY_PAUSE -> {
                            dao.updatePlayPause(!state.isPlaying)
                        }
                        ACTION_PREV -> {
                            val tracks = dao.getTracksForServiceSync(state.activeServiceId)
                            if (tracks.isNotEmpty()) {
                                val currentIndex = tracks.indexOfFirst { it.id == state.currentTrackId }
                                val prevIndex = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
                                dao.updatePlaybackState(state.copy(
                                    currentTrackId = tracks[prevIndex].id,
                                    progressMs = 0L
                                ))
                            }
                        }
                        ACTION_NEXT -> {
                            val tracks = dao.getTracksForServiceSync(state.activeServiceId)
                            if (tracks.isNotEmpty()) {
                                val currentIndex = tracks.indexOfFirst { it.id == state.currentTrackId }
                                val nextIndex = (currentIndex + 1) % tracks.size
                                dao.updatePlaybackState(state.copy(
                                    currentTrackId = tracks[nextIndex].id,
                                    progressMs = 0L
                                ))
                            }
                        }
                        ACTION_SWITCH_SERVICE -> {
                            val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID) ?: "spotify"
                            val account = dao.getAccountById(serviceId)
                            
                            if (account != null && account.isLoggedIn) {
                                // Service is logged in, switch seamlessly
                                val serviceTracks = dao.getTracksForServiceSync(serviceId)
                                val firstTrackId = serviceTracks.firstOrNull()?.id ?: when (serviceId) {
                                    "spotify" -> "spotify_track_1"
                                    "youtube_music" -> "yt_track_1"
                                    else -> "tidal_track_1"
                                }
                                dao.updatePlaybackState(state.copy(
                                    activeServiceId = serviceId,
                                    currentTrackId = firstTrackId,
                                    progressMs = 0L,
                                    isPlaying = false
                                ))
                            } else {
                                // Service is not logged in / authenticated! Launch app main activity to log in
                                val launchIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("INITIATE_LOGIN_SERVICE", serviceId)
                                }
                                context.startActivity(launchIntent)
                            }
                        }
                    }

                    // Trigger widgets refresh
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, MusicSwitcherWidgetReceiver::class.java)
                    )
                    val updatedState = dao.getPlaybackStateSync()
                    val activeTrack = updatedState?.currentTrackId?.let { dao.getTrackById(it) }
                    val accounts = dao.getAllAccounts().firstOrNull() ?: emptyList()

                    for (appWidgetId in ids) {
                        val views = RemoteViews(context.packageName, R.layout.widget_music_switcher)
                        updateWidgetUI(context, views, updatedState, activeTrack, accounts)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }

                } catch (e: Exception) {
                    Log.e("MusicWidget", "Error handling broadcast: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun updateWidgetUI(
        context: Context,
        views: RemoteViews,
        state: PlaybackStateEntity?,
        activeTrack: Track?,
        accounts: List<MusicAccount>
    ) {
        val appName = "Harmony Switched"
        val activeServiceId = state?.activeServiceId ?: "spotify"
        
        // Find active service account
        val activeAccount = accounts.find { it.serviceId == activeServiceId }
        val serviceName = when (activeServiceId) {
            "spotify" -> "Spotify"
            "youtube_music" -> "YouTube Music"
            "tidal" -> "Tidal Lossless"
            else -> "Spotify"
        }

        // 1. Text & Metadata
        views.setTextViewText(R.id.widget_service_title, serviceName)
        
        if (activeTrack != null) {
            views.setTextViewText(R.id.widget_track_title, activeTrack.title)
            views.setTextViewText(R.id.widget_track_artist, activeTrack.artist)
        } else {
            views.setTextViewText(R.id.widget_track_title, "No active track")
            views.setTextViewText(R.id.widget_track_artist, "Choose service in app")
        }

        // 2. Play/Pause icon based on status
        val playPauseIcon = if (state?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play_arrow
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

        // 3. Service Indicator highlight state (Active service gets a filled back block, others are translucent)
        // Set Spotify Indicator highlight state
        val spotAcc = accounts.find { it.serviceId == "spotify" }
        if (spotAcc?.isLoggedIn == true) {
            views.setTextColor(R.id.widget_switch_spotify, Color.parseColor("#1DB954"))
            if (activeServiceId == "spotify") {
                views.setInt(R.id.widget_switch_spotify, "setBackgroundColor", Color.parseColor("#441DB954"))
            } else {
                views.setInt(R.id.widget_switch_spotify, "setBackgroundColor", Color.parseColor("#151DB954"))
            }
        } else {
            views.setTextColor(R.id.widget_switch_spotify, Color.parseColor("#5F6670"))
            views.setInt(R.id.widget_switch_spotify, "setBackgroundColor", Color.parseColor("#10222222"))
        }

        // Set YT Music Indicator highlight state
        val ytAcc = accounts.find { it.serviceId == "youtube_music" }
        if (ytAcc?.isLoggedIn == true) {
            views.setTextColor(R.id.widget_switch_yt, Color.parseColor("#FF0000"))
            if (activeServiceId == "youtube_music") {
                views.setInt(R.id.widget_switch_yt, "setBackgroundColor", Color.parseColor("#44FF0000"))
            } else {
                views.setInt(R.id.widget_switch_yt, "setBackgroundColor", Color.parseColor("#15FF0000"))
            }
        } else {
            views.setTextColor(R.id.widget_switch_yt, Color.parseColor("#5F6670"))
            views.setInt(R.id.widget_switch_yt, "setBackgroundColor", Color.parseColor("#10222222"))
        }

        // Set Tidal Indicator highlight state
        val tidAcc = accounts.find { it.serviceId == "tidal" }
        if (tidAcc?.isLoggedIn == true) {
            views.setTextColor(R.id.widget_switch_tidal, Color.parseColor("#00D2FF"))
            if (activeServiceId == "tidal") {
                views.setInt(R.id.widget_switch_tidal, "setBackgroundColor", Color.parseColor("#4400D2FF"))
            } else {
                views.setInt(R.id.widget_switch_tidal, "setBackgroundColor", Color.parseColor("#1500D2FF"))
            }
        } else {
            views.setTextColor(R.id.widget_switch_tidal, Color.parseColor("#5F6670"))
            views.setInt(R.id.widget_switch_tidal, "setBackgroundColor", Color.parseColor("#10222222"))
        }

        // 4. PendingIntent for buttons
        views.setOnClickPendingIntent(R.id.widget_btn_prev, getPendingSelfIntent(context, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, getPendingSelfIntent(context, ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.widget_btn_next, getPendingSelfIntent(context, ACTION_NEXT))

        views.setOnClickPendingIntent(R.id.widget_switch_spotify, getPendingSwitchIntent(context, "spotify"))
        views.setOnClickPendingIntent(R.id.widget_switch_yt, getPendingSwitchIntent(context, "youtube_music"))
        views.setOnClickPendingIntent(R.id.widget_switch_tidal, getPendingSwitchIntent(context, "tidal"))
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MusicSwitcherWidgetReceiver::class.java).apply {
            this.action = action
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
    }

    private fun getPendingSwitchIntent(context: Context, serviceId: String): PendingIntent {
        val intent = Intent(context, MusicSwitcherWidgetReceiver::class.java).apply {
            this.action = ACTION_SWITCH_SERVICE
            putExtra(EXTRA_SERVICE_ID, serviceId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, serviceId.hashCode(), intent, flags)
    }
}
