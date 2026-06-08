package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MusicAccount
import com.example.data.model.Track
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.MusicViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels { MusicViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read if launched from widget requesting login
        val serviceToLogin = intent?.getStringExtra("INITIATE_LOGIN_SERVICE")

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF07080B)
                ) { innerPadding ->
                    MainMusicWorkspace(
                        viewModel = viewModel,
                        initialLoginService = serviceToLogin,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMusicWorkspace(
    viewModel: MusicViewModel,
    initialLoginService: String?,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val state by viewModel.playbackState.collectAsStateWithLifecycle()
    val tracks by viewModel.activeTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.activeTrack.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAccountMenu by remember { mutableStateOf(false) }
    var selectedAuthService by remember { mutableStateOf<String?>(null) }
    val activeServiceId = state?.activeServiceId ?: "spotify"

    // Set initial login trigger if launched from widget
    LaunchedEffect(initialLoginService) {
        if (initialLoginService != null) {
            selectedAuthService = initialLoginService
        }
    }

    // Determine current brand accent colors dynamically according to active music account
    val brandAccent = when (activeServiceId) {
        "spotify" -> Color(0xFF1DB954)
        "youtube_music" -> Color(0xFFFF0000)
        "tidal" -> Color(0xFF00D2FF)
        else -> Color(0xFF1DB954)
    }

    val activeAccount = accounts.find { it.serviceId == activeServiceId }
    val isLoggedIn = activeAccount?.isLoggedIn == true

    Box(modifier = modifier) {
        // Immersive atmospheric blurred ambient gradient in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = brandAccent.copy(alpha = 0.08f),
                radius = 350.dp.toPx(),
                center = center.copy(y = center.y - 120.dp.toPx())
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = brandAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- TOP ACTION BAR ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Immersive persistent Account Switcher Drop-Pill on the Top-Left Corner
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF161921).copy(alpha = 0.85f))
                            .border(1.dp, brandAccent.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clickable { showAccountMenu = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small glowing color dot representing the active brand choice
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(brandAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = when (activeServiceId) {
                                "spotify" -> "Spotify"
                                "youtube_music" -> "YouTube Music"
                                else -> "Tidal HiFi"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Music Service",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Combined Hub branding logo or status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E212D).copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Syncing Connected State",
                            tint = brandAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hub Active",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- CENTER PERFORMANCE DISC CANVAS ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn) {
                        PlayerVinylVisualizer(
                            isPlaying = state?.isPlaying == true,
                            brandAccent = brandAccent,
                            coverStart = currentTrack?.coverGradientStart ?: "#1DB954",
                            coverEnd = currentTrack?.coverGradientEnd ?: "#115E2E"
                        )
                    } else {
                        // Shield Card telling user to connect account
                        LockedServicePlaceholder(
                            serviceName = when (activeServiceId) {
                                "spotify" -> "Spotify"
                                "youtube_music" -> "YouTube Music"
                                else -> "Tidal"
                            },
                            brandAccent = brandAccent,
                            onConnectClick = { selectedAuthService = activeServiceId }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- METADATA & CONTROL ROW ---
                if (isLoggedIn && currentTrack != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTrack?.title ?: "",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currentTrack?.artist ?: ""}  •  ${currentTrack?.album ?: ""}",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- PROGRESS TRACK SLIDER ---
                    val progress = state?.progressMs ?: 0L
                    val duration = currentTrack?.durationMs ?: 200000L
                    val progressPercentage = progress.toFloat() / duration.toFloat()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = progressPercentage,
                            onValueChange = { percent ->
                                val target = (percent * duration).toLong()
                                viewModel.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = brandAccent,
                                inactiveTrackColor = Color(0xFF2E313C),
                                thumbColor = brandAccent
                            ),
                            modifier = Modifier.height(12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(progress),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- TRANSPORT CONTROLS ROW ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle Playlist",
                                tint = if (state?.isShuffle == true) brandAccent else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.skipToPrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play Pause Sphere Bubble
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(brandAccent)
                                .clickable { viewModel.togglePlayPause() }
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Pause Button",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.skipToNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat Track",
                                tint = if (state?.isRepeat == true) brandAccent else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(120.dp))
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- SEAMLESS CATALOG LIST (BOTTOM SCROLL AREA) ---
                Text(
                    text = if (isLoggedIn) "Available Playlist Cache" else "Account Configuration Sync",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoggedIn) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(tracks) { track ->
                            val isPlayingThis = currentTrack?.id == track.id
                            TrackItemRow(
                                track = track,
                                isPlayingThis = isPlayingThis,
                                brandAccent = brandAccent,
                                onClick = { viewModel.selectTrack(track.id, activeServiceId) }
                            )
                        }
                    }
                } else {
                    // Empty or disconnected sync list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF13151D))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Login Locked",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Authentication Required",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please map account details to sync offline playback cache.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Immersive bottom sheet dialog for accounts configurations switcher
        if (showAccountMenu) {
            AccountSwitcherBottomSheet(
                accounts = accounts,
                activeServiceId = activeServiceId,
                onSwitchService = { serviceId ->
                    viewModel.switchActiveService(serviceId)
                    showAccountMenu = false
                },
                onLoginClick = { serviceId ->
                    selectedAuthService = serviceId
                    showAccountMenu = false
                },
                onLogoutClick = { serviceId ->
                    viewModel.simulateLogout(serviceId)
                },
                onDismiss = { showAccountMenu = false }
            )
        }

        // Immersive OAuth Dialog Simulator
        if (selectedAuthService != null) {
            OAuthAuthenticationDialog(
                serviceId = selectedAuthService!!,
                onSuccess = { username ->
                    viewModel.simulateLogin(selectedAuthService!!, username)
                    selectedAuthService = null
                },
                onDismiss = { selectedAuthService = null }
            )
        }
    }
}

// FORMAT TIME HELPER
fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / 60000) % 60
    return String.format("%02d:%02d", min, sec)
}

// LIST ITEM ROW COMPOSABLE
@Composable
fun TrackItemRow(
    track: Track,
    isPlayingThis: Boolean,
    brandAccent: Color,
    onClick: () -> Unit
) {
    val containerBg = if (isPlayingThis) Color(0xFF161921) else Color(0xFF0F1116)
    val borderStroke = if (isPlayingThis) brandAccent.copy(alpha = 0.25f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(1.dp, borderStroke, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Multi-color album indicator using custom geometric linear gradients
        val gradStart = Color(android.graphics.Color.parseColor(track.coverGradientStart))
        val gradEnd = Color(android.graphics.Color.parseColor(track.coverGradientEnd))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlayingThis) brandAccent else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isPlayingThis) {
            // Creative visualizer music wave playing indicator
            Box(
                modifier = Modifier
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Interactive dynamic equalizer micro animation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    repeat(3) { index ->
                        val infiniteTransition = rememberInfiniteTransition(label = "wave")
                        val heightMultiplier by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 400 + (index * 150), easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "height"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(heightMultiplier)
                                .clip(RoundedCornerShape(1.dp))
                                .background(brandAccent)
                        )
                    }
                }
            }
        } else {
            Text(
                text = formatTime(track.durationMs),
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// PLAYER ROTATING VINYL LAYOUT
@Composable
fun PlayerVinylVisualizer(
    isPlaying: Boolean,
    brandAccent: Color,
    coverStart: String,
    coverEnd: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotationAngle else 0f

    Box(
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer { rotationZ = currentRotation }
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF101015))
            .border(8.dp, Color(0xFF1C1E26), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Draw decorative concentric grooves of music vinyl
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rMax = size.minDimension / 2
            repeat(6) { idx ->
                val r = rMax - (30.dp.toPx() + (idx * 16.dp.toPx()))
                if (r > 0) {
                    drawCircle(
                        color = Color.DarkGray.copy(alpha = 0.15f),
                        radius = r,
                        style = Stroke(width = 1.5f.dp.toPx())
                    )
                }
            }
        }

        // Center album artwork
        val gradStart = Color(android.graphics.Color.parseColor(coverStart))
        val gradEnd = Color(android.graphics.Color.parseColor(coverEnd))
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
                .border(3.dp, Color(0xFF101015), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Innermost spindle hole
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF07080B))
                    .border(2.dp, brandAccent.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

// LOCKED SERVICE PLACEHOLDER
@Composable
fun LockedServicePlaceholder(
    serviceName: String,
    brandAccent: Color,
    onConnectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, Color(0xFF222530), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(brandAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = brandAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$serviceName Disconnected",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authenticate your account safely so you can load playlists, synchronize progress, and control via native widget switches.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Seamless Log In",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// IMMERSIVE BOTTOM SHEET ACCOUNT SWITCHER
@Composable
fun AccountSwitcherBottomSheet(
    accounts: List<MusicAccount>,
    activeServiceId: String,
    onSwitchService: (String) -> Unit,
    onLoginClick: (String) -> Unit,
    onLogoutClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Elegant background dim screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                onClick = onDismiss,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Immersive bottom drawer content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF12141A))
                .border(1.dp, Color(0xFF222633), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {} // Avoid click bubbling
                .padding(24.dp)
        ) {
            // Drag handle decorator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.DarkGray)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "My Combined Services",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Switch active players or sync login tokens securely.",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Load Accounts switch lists
            accounts.forEach { account ->
                val isSelected = account.serviceId == activeServiceId
                val serviceColor = when (account.serviceId) {
                    "spotify" -> Color(0xFF1DB954)
                    "youtube_music" -> Color(0xFFFF0000)
                    else -> Color(0xFF00D2FF)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Color(0xFF1B1E29) else Color(0xFF0C0E13)
                        )
                        .border(
                            1.dp,
                            if (isSelected) serviceColor.copy(alpha = 0.35f) else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (account.isLoggedIn) {
                                onSwitchService(account.serviceId)
                            } else {
                                onLoginClick(account.serviceId)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Logo visual
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(serviceColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (account.serviceId) {
                                "spotify" -> "SP"
                                "youtube_music" -> "YT"
                                else -> "TD"
                            },
                            color = serviceColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Middle details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (account.serviceId) {
                                "spotify" -> "Spotify Premium"
                                "youtube_music" -> "YouTube Music"
                                else -> "Tidal HiFi Lossless"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (account.isLoggedIn) account.username else "Lacks configuration token",
                            color = if (account.isLoggedIn) serviceColor else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Right operations
                    if (account.isLoggedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Text(
                                    text = "Active",
                                    color = serviceColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                            IconButton(onClick = { onLogoutClick(account.serviceId) }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Unlink Account",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onLoginClick(account.serviceId) },
                            colors = ButtonDefaults.buttonColors(containerColor = serviceColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Link",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// THERMED IMMERSIVE OAUTH DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthAuthenticationDialog(
    serviceId: String,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val serviceName = when (serviceId) {
        "spotify" -> "Spotify"
        "youtube_music" -> "Google / YouTube Music"
        else -> "Tidal Lossless"
    }
    val brandAccent = when (serviceId) {
        "spotify" -> Color(0xFF1DB954)
        "youtube_music" -> Color(0xFFFF0000)
        else -> Color(0xFF00D2FF)
    }

    var loginInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = brandAccent,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$serviceName Account Link",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Link your account to Harmony Music. We use encrypted OAuth callbacks without sharing your main passwords.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = loginInput,
                    onValueChange = { loginInput = it },
                    label = { Text("Client Email/Username", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandAccent,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = brandAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandAccent,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = brandAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = {},
                        colors = CheckboxDefaults.colors(checkedColor = brandAccent)
                    )
                    Text(
                        text = "I grant permissions to read playlists & sync active widget states.",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                if (isAuthenticating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = brandAccent, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verifying Client Key Exchange...", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    if (loginInput.isNotBlank()) {
                        isAuthenticating = true
                        scope.launch {
                            delay(1800) // Beautiful simulated loading
                            val formattedName = if (loginInput.contains("@")) "@${loginInput.substringBefore("@")}" else "@$loginInput"
                            onSuccess(formattedName)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = brandAccent),
                shape = RoundedCornerShape(8.dp),
                enabled = loginInput.isNotBlank() && !isAuthenticating
            ) {
                Text("Approve Secure Connection", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAuthenticating) {
                Text("Cancel", color = Color.Gray, fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF13151D),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0xFF2E313D), RoundedCornerShape(20.dp))
    )
}
