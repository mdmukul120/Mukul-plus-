package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExtractorPost
import com.example.data.model.TvChannel
import com.example.data.repository.AuthRepository
import com.example.data.repository.MediaRepository
import com.example.ui.components.MukulPlusLogo
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String, val icon: ImageVector) {
    HOME("হোম", Icons.Default.Home),
    MOVIES("মুভিজ", Icons.Default.Movie),
    LIVE_TV("লাইভ টিভি", Icons.Default.Tv),
    EXTRACTOR("ডাউনলোড", Icons.Default.CloudDownload),
    PROFILE("প্রোফাইল", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MukulPlusApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MukulPlusApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authRepository = remember { AuthRepository(context) }
    val mediaRepository = remember { MediaRepository(context) }

    val currentUser by authRepository.currentUser.collectAsState()

    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var selectedMovieId by remember { mutableStateOf<Long?>(null) }
    var selectedExtractorPost by remember { mutableStateOf<ExtractorPost?>(null) }
    var showPaintingScreen by remember { mutableStateOf(false) }
    var showAuthScreen by remember { mutableStateOf(false) }

    // Handle Android system back button
    BackHandler(
        enabled = selectedMovieId != null || selectedExtractorPost != null || showPaintingScreen || showAuthScreen
    ) {
        when {
            showAuthScreen -> showAuthScreen = false
            showPaintingScreen -> showPaintingScreen = false
            selectedMovieId != null -> selectedMovieId = null
            selectedExtractorPost != null -> selectedExtractorPost = null
        }
    }

    // If detail screen is open (second page)
    if (selectedMovieId != null || selectedExtractorPost != null) {
        MovieDetailScreen(
            movieId = selectedMovieId,
            extractorLink = selectedExtractorPost?.link,
            extractorProvider = selectedExtractorPost?.provider,
            initialTitle = selectedExtractorPost?.title,
            initialPoster = selectedExtractorPost?.image,
            mediaRepository = mediaRepository,
            onBackClick = {
                selectedMovieId = null
                selectedExtractorPost = null
            },
            onSelectMovie = { newId ->
                selectedMovieId = newId
                selectedExtractorPost = null
            }
        )
        return
    }

    // If painting screen is open (requested sidebar option)
    if (showPaintingScreen) {
        PaintingScreen(
            onBack = { showPaintingScreen = false }
        )
        return
    }

    // Modal Drawer for Sidebar
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CinemaSurface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Brand Header
                    MukulPlusLogo(iconSize = 36, textSize = 22)
                    Spacer(modifier = Modifier.height(14.dp))

                    // User Info Card in Drawer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CinemaSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                currentTab = ScreenTab.PROFILE
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = BrandRed,
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser?.displayName?.firstOrNull()?.uppercase() ?: "U",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = currentUser?.displayName ?: "Guest User",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (currentUser?.isGuest == true) "অতিথি (Guest)" else "VIP Member",
                                    color = CyanAccent,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = CinemaBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = if (currentTab == ScreenTab.HOME) BrandRed else TextSecondary) },
                        label = { Text("হোম পেজ (Home)") },
                        selected = currentTab == ScreenTab.HOME,
                        onClick = {
                            currentTab = ScreenTab.HOME
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = drawerItemColors()
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Movie, contentDescription = null, tint = if (currentTab == ScreenTab.MOVIES) BrandRed else TextSecondary) },
                        label = { Text("মুভি ও সিরিজ (Movies & Filter)") },
                        selected = currentTab == ScreenTab.MOVIES,
                        onClick = {
                            currentTab = ScreenTab.MOVIES
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = drawerItemColors()
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Tv, contentDescription = null, tint = if (currentTab == ScreenTab.LIVE_TV) BrandRed else TextSecondary) },
                        label = { Text("লাইভ টিভি চ্যানেল (Live TV)") },
                        selected = currentTab == ScreenTab.LIVE_TV,
                        onClick = {
                            currentTab = ScreenTab.LIVE_TV
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = drawerItemColors()
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = if (currentTab == ScreenTab.EXTRACTOR) BrandRed else TextSecondary) },
                        label = { Text("ডাউনলোড লিংক ও প্রোভাইডার") },
                        selected = currentTab == ScreenTab.EXTRACTOR,
                        onClick = {
                            currentTab = ScreenTab.EXTRACTOR
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = drawerItemColors()
                    )

                    // Sidebar Painting Option
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Brush, contentDescription = null, tint = GoldRating) },
                        label = { Text("পেইন্টিং অপশন (Painting & Sketch)", color = GoldRating, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showPaintingScreen = true
                        },
                        colors = drawerItemColors()
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (currentTab == ScreenTab.PROFILE) BrandRed else TextSecondary) },
                        label = { Text("প্রোফাইল ও ওয়াচলিস্ট (Account)") },
                        selected = currentTab == ScreenTab.PROFILE,
                        onClick = {
                            currentTab = ScreenTab.PROFILE
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = drawerItemColors()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Drawer Action (Sign In or Logout)
                    HorizontalDivider(color = CinemaBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser != null && !currentUser!!.isGuest) {
                        Surface(
                            onClick = {
                                authRepository.logout()
                                coroutineScope.launch { drawerState.close() }
                            },
                            color = CinemaSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF5252))
                                Text("লগআউট (Log Out)", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showAuthScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("লগইন / রেজিস্টার করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = CinemaBackground,
            topBar = {
                TopAppBar(
                    title = {
                        MukulPlusLogo(iconSize = 30, textSize = 18)
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Sidebar", tint = Color.White)
                        }
                    },
                    actions = {
                        // Quick Painting button in TopBar
                        IconButton(onClick = { showPaintingScreen = true }) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = "Painting", tint = GoldRating)
                        }

                        // Profile / Auth Avatar
                        IconButton(onClick = {
                            if (currentUser == null) {
                                showAuthScreen = true
                            } else {
                                currentTab = ScreenTab.PROFILE
                            }
                        }) {
                            Surface(
                                color = BrandRed,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaSurface)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = CinemaSurface,
                    tonalElevation = 8.dp
                ) {
                    ScreenTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) BrandRed else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) BrandRedLight else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CinemaSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    ScreenTab.HOME -> {
                        HomeScreen(
                            mediaRepository = mediaRepository,
                            onSelectMovie = { id -> selectedMovieId = id },
                            onSelectPost = { post -> selectedExtractorPost = post },
                            onSelectChannel = { channel -> currentTab = ScreenTab.LIVE_TV },
                            onNavigateToMovies = { currentTab = ScreenTab.MOVIES },
                            onNavigateToLiveTv = { currentTab = ScreenTab.LIVE_TV },
                            onNavigateToExtractor = { currentTab = ScreenTab.EXTRACTOR }
                        )
                    }
                    ScreenTab.MOVIES -> {
                        MoviesScreen(
                            mediaRepository = mediaRepository,
                            onSelectMovie = { id -> selectedMovieId = id }
                        )
                    }
                    ScreenTab.LIVE_TV -> {
                        LiveTvScreen(
                            mediaRepository = mediaRepository
                        )
                    }
                    ScreenTab.EXTRACTOR -> {
                        ExtractorScreen(
                            onSelectPost = { post -> selectedExtractorPost = post }
                        )
                    }
                    ScreenTab.PROFILE -> {
                        if (currentUser != null) {
                            ProfileScreen(
                                user = currentUser!!,
                                authRepository = authRepository,
                                mediaRepository = mediaRepository,
                                onSelectMovie = { id -> selectedMovieId = id },
                                onLogout = { currentTab = ScreenTab.HOME }
                            )
                        } else {
                            AuthScreen(
                                authRepository = authRepository,
                                onAuthSuccess = { currentTab = ScreenTab.HOME }
                            )
                        }
                    }
                }

                // Auth Screen Overlay if opened from button
                if (showAuthScreen) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AuthScreen(
                            authRepository = authRepository,
                            onAuthSuccess = { showAuthScreen = false }
                        )
                        IconButton(
                            onClick = { showAuthScreen = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun drawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = CinemaSurfaceVariant,
    unselectedContainerColor = Color.Transparent,
    selectedTextColor = BrandRedLight,
    unselectedTextColor = TextSecondary
)
