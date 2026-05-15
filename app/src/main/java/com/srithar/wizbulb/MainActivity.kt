package com.srithar.wizbulb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WizScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizScreen() {
    var bulbIp by remember {
        mutableStateOf("192.168.1.201")
    }

    var statusInfo by remember {
        mutableStateOf("No status yet")
    }

    var bulbState by remember { mutableStateOf<WizState?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }

    var showColorDialog by remember { mutableStateOf(false) }
    var showSceneDialog by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var discoveredBulbs by remember { mutableStateOf<List<String>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshStatus(delayMs: Long = 0) {
        scope.launch {
            isLoading = true
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            val result = WizBulbController.getStatus(bulbIp)
            bulbState = result
            if (result != null) {
                statusInfo = "State: ${if (result.isOn) "ON" else "OFF"}\n" +
                        "Brightness: ${result.brightness}\n" +
                        "Color: R=${result.r}, G=${result.g}, B=${result.b}\n" +
                        "Temp : ${result.temp}" +
                        (if (result.sceneId > 0) "\nScene ID: ${result.sceneId}" else "")
            } else {
                statusInfo = "Could not connect to wiz light."
                snackbarHostState.showSnackbar("Network error: Could not reach the bulb.")
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("WizBulb")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Created By SritharBoss",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bulbIp,
                    onValueChange = {
                        bulbIp = it
                    },
                    label = {
                        Text("Bulb IP Address")
                    },
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isDiscovering = true
                            val discoveryJob = launch {
                                discoveredBulbs = WizBulbController.discoverBulbs()
                            }
                            discoveryJob.join()
                            isDiscovering = false
                            if (discoveredBulbs.isEmpty()) {
                                snackbarHostState.showSnackbar("No bulbs found on network.")
                            } else {
                                showDiscoveryDialog = true
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Fetch")
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val success = WizBulbController.turnOn(bulbIp)
                        if (success) {
                            refreshStatus(300)
                        } else {
                            snackbarHostState.showSnackbar("Failed to turn ON the bulb. Check network.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Turn ON")
            }

            Button(
                onClick = {
                    scope.launch {
                        val success = WizBulbController.turnOff(bulbIp)
                        if (success) {
                            refreshStatus(300)
                        } else {
                            snackbarHostState.showSnackbar("Failed to turn OFF the bulb. Check network.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Turn OFF")
            }

            Button(
                onClick = {
                    showColorDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Color")
            }

            Button(
                onClick = {
                    showSceneDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scenes")
            }

            if (showColorDialog) {
                AlertDialog(
                    onDismissRequest = { showColorDialog = false },
                    title = { Text("Select Color") },
                    text = {
                        Column {
                            val colors = listOf(
                                "Red" to Triple(255, 0, 0),
                                "Green" to Triple(0, 255, 0),
                                "Blue" to Triple(0, 0, 255),
                                "Yellow" to Triple(255, 255, 0),
                                "Cyan" to Triple(0, 255, 255),
                                "Magenta" to Triple(255, 0, 255),
                                "White" to Triple(255, 255, 255)
                            )

                            colors.forEach { (name, rgb) ->
                                TextButton(
                                    onClick = {
                                        showColorDialog = false
                                        scope.launch {
                                            val success = WizBulbController.setColor(bulbIp, rgb.first, rgb.second, rgb.third)
                                            if (success) {
                                                refreshStatus(500)
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to set color. Check network.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showColorDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showSceneDialog) {
                AlertDialog(
                    onDismissRequest = { showSceneDialog = false },
                    title = { Text("Select Scene") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            val scenes = listOf(
                                "Relax" to 16,
                                "Cozy" to 6,
                                "Daylight" to 12,
                                "Warm White" to 11,
                                "Cool White" to 13,
                                "TV Time" to 18,
                                "Night Light" to 14,
                                "Focus" to 15,
                                "Party" to 4,
                                "Fireplace" to 5,
                                "Ocean" to 1,
                                "Forest" to 7,
                                "Romance" to 2,
                                "Sunrise" to 9,
                                "Sunset" to 3,
                                "Flash / Color Cycle" to 8
                            )

                            scenes.forEach { (name, id) ->
                                TextButton(
                                    onClick = {
                                        showSceneDialog = false
                                        scope.launch {
                                            val success = WizBulbController.setScene(bulbIp, id)
                                            if (success) {
                                                refreshStatus(500)
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to set scene. Check network.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSceneDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showDiscoveryDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscoveryDialog = false },
                    title = { Text("Discovered Bulbs") },
                    text = {
                        Column {
                            discoveredBulbs.forEach { ip ->
                                TextButton(
                                    onClick = {
                                        bulbIp = ip
                                        showDiscoveryDialog = false
                                        refreshStatus()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(ip)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDiscoveryDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Button(
                onClick = {
                    refreshStatus()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Status")
            }

            if (isLoading) {
                StatusSkeleton()
            } else {
                Text(
                    text = statusInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                bulbState?.let { state ->
                    val brightnessFactor = state.brightness / 100f

                    val infiniteTransition = rememberInfiniteTransition(label = "bulbEffects")

                    // Color rotation for dynamic scenes like Party
                    val animatedHue by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "hue"
                    )

                    // Flickering effect for Fireplace
                    val flicker by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "flicker"
                    )

                    val baseColor = if (state.isOn) {
                        when {
                            state.sceneId == 4 || state.sceneId == 8 -> {
                                // Party / Color Cycle: Cycle through hues
                                Color.hsv(animatedHue, 0.7f, 1f).copy(alpha = brightnessFactor.coerceAtLeast(0.3f))
                            }
                            state.sceneId == 5 -> {
                                // Fireplace: Use the scene color with a flickering alpha
                                getSceneColor(5).copy(alpha = (brightnessFactor * flicker).coerceAtLeast(0.3f))
                            }
                            state.sceneId > 0 -> {
                                // Static scenes
                                getSceneColor(state.sceneId).copy(alpha = brightnessFactor.coerceAtLeast(0.3f))
                            }
                            state.r == 0 && state.g == 0 && state.b == 0 -> {
                                // White modes: Use Kelvin-to-Color mapping
                                kelvinToColor(state.temp).copy(alpha = brightnessFactor.coerceAtLeast(0.3f))
                            }
                            else -> {
                                // Manual RGB color
                                Color(state.r, state.g, state.b).copy(alpha = brightnessFactor.coerceAtLeast(0.3f))
                            }
                        }
                    } else {
                        Color.DarkGray.copy(alpha = 0.2f)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isOn) {
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(pulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(baseColor.copy(alpha = 0.3f), Color.Transparent)
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(baseColor)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 3) 0.6f else 1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Maps Kelvin temperature to a representable UI color.
 */
fun kelvinToColor(kelvin: Int): Color {
    return when {
        kelvin <= 2000 -> Color(0xFFFF7000)
        kelvin <= 2700 -> Color(0xFFFF9500)
        kelvin <= 3000 -> Color(0xFFFFB35C)
        kelvin <= 4000 -> Color(0xFFFFE4CE)
        kelvin <= 5000 -> Color(0xFFE3EBFF)
        else -> Color(0xFFD1E1FF)
    }
}

/**
 * Maps Wiz Scene IDs to characteristic colors for simulation.
 */
fun getSceneColor(sceneId: Int): Color {
    return when (sceneId) {
        1 -> Color(0xFF00FFFF) // Ocean
        2 -> Color(0xFFFF00FF) // Romance
        3 -> Color(0xFFFF4500) // Sunset
        4 -> Color.Magenta     // Party
        5 -> Color(0xFFFF4500) // Fireplace
        6 -> Color(0xFFFFB35C) // Cozy
        7 -> Color.Green       // Forest
        8 -> Color.Red         // Color Cycle
        9 -> Color(0xFFFFD700) // Sunrise
        11 -> Color(0xFFFFB35C) // Warm White
        12 -> Color(0xFFD1E1FF) // Daylight
        13 -> Color(0xFFFFE4CE) // Cool White
        14 -> Color(0xFFFF8C00) // Night Light
        15 -> Color(0xFFE3EBFF) // Focus
        16 -> Color(0xFFFFB35C) // Relax
        18 -> Color(0xFF4169E1) // TV Time
        else -> Color(0xFFFFF4E5)
    }
}
