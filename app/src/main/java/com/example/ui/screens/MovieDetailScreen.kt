package com.example.ui.screens

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.ApiClient
import com.example.data.model.*
import com.example.data.repository.MediaRepository
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Long?,
    extractorLink: String?,
    extractorProvider: String?,
    initialTitle: String?,
    initialPoster: String?,
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    onSelectMovie: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var ctgMovie by remember { mutableStateOf<CtgMovie?>(null) }
    var extractorInfo by remember { mutableStateOf<ExtractorMovieInfo?>(null) }
    var trailers by remember { mutableStateOf<List<TmdbVideo>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<TmdbSearchResult>>(emptyList()) }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPlayingInApp by remember { mutableStateOf(false) }

    val favorites by mediaRepository.favorites.collectAsState()
    val isFav = movieId != null && favorites.contains(movieId)

    // Fetch movie details from CtgHall, Extractor, or TMDB
    LaunchedEffect(movieId, extractorLink) {
        isLoading = true
        try {
            // 1. CtgHall Detail if ID is provided
            if (movieId != null && movieId > 0) {
                val movie = ApiClient.fetchCtgMovieDetail(movieId)
                ctgMovie = movie
                val streamUrl = movie?.getFullStreamUrl()
                if (!streamUrl.isNullOrEmpty()) {
                    activeStreamUrl = streamUrl
                }

                // If tmdb_id exists, fetch trailers & recommendations
                val tmdbIdLong = movie?.tmdb_id?.toLongOrNull()
                if (tmdbIdLong != null) {
                    trailers = ApiClient.fetchTmdbVideos(tmdbIdLong)
                    recommendations = ApiClient.fetchTmdbRecommendations(tmdbIdLong)
                }
            }

            // 2. Extractor info if link is provided
            if (!extractorLink.isNullOrEmpty() && !extractorProvider.isNullOrEmpty()) {
                val info = ApiClient.fetchExtractorInfo(extractorLink, extractorProvider)
                extractorInfo = info
                if (activeStreamUrl == null && !info?.streamLinks.isNullOrEmpty()) {
                    activeStreamUrl = info?.streamLinks?.firstOrNull()?.link
                }
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    val displayTitle = ctgMovie?.title ?: extractorInfo?.title ?: initialTitle ?: "Movie Details"
    val displayPoster = ctgMovie?.getFullPosterUrl() ?: extractorInfo?.image ?: initialPoster ?: ""
    val displayBackdrop = ctgMovie?.getFullBackdropUrl() ?: displayPoster
    val displayOverview = ctgMovie?.overview ?: extractorInfo?.synopsis ?: "No storyline available."

    Scaffold(
        containerColor = CinemaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (movieId != null) {
                        IconButton(onClick = { mediaRepository.toggleFavorite(movieId) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) BrandRed else Color.White
                            )
                        }
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "Watch $displayTitle on Mukul Plus OTT: ${activeStreamUrl ?: "https://www.ctghall.com/api/movies/$movieId"}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Movie"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Top Media Player or Hero Poster
            item {
                if (isPlayingInApp && !activeStreamUrl.isNullOrEmpty()) {
                    VideoPlayerView(
                        videoUrl = activeStreamUrl!!,
                        title = displayTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(displayBackdrop)
                                .crossfade(true)
                                .build(),
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0x77000000),
                                            CinemaBackground
                                        )
                                    )
                                )
                        )

                        // Play Button
                        Surface(
                            onClick = {
                                if (!activeStreamUrl.isNullOrEmpty()) {
                                    isPlayingInApp = true
                                } else if (!displayTitle.isNullOrEmpty()) {
                                    Toast.makeText(context, "Finding stream source...", Toast.LENGTH_SHORT).show()
                                    isPlayingInApp = true
                                }
                            },
                            shape = CircleShape,
                            color = BrandRed,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Movie",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Stream tag
                        Surface(
                            color = BrandRed,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "HD 1080P • HIGH QUALITY STREAM",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 2. Movie Title & Meta Information
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val altTitle = ctgMovie?.original_title
                    if (!altTitle.isNullOrEmpty() && altTitle != displayTitle) {
                        Text(
                            text = altTitle,
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rating = ctgMovie?.online_rating ?: ctgMovie?.user_rating
                        if (rating != null && rating > 0.0) {
                            Surface(
                                color = CinemaSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldRating,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", rating),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        ctgMovie?.year?.let { y ->
                            Surface(color = CinemaSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = y.toString(),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val genreStr = ctgMovie?.genre ?: ctgMovie?.Library?.name
                        if (!genreStr.isNullOrEmpty()) {
                            Surface(color = CinemaSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = genreStr,
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Buttons (Stream / Download / Browser)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isPlayingInApp = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("এখন চালান (Play)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val downloadUrl = activeStreamUrl ?: ctgMovie?.getFullStreamUrl()
                                if (!downloadUrl.isNullOrEmpty()) {
                                    startDownload(context, downloadUrl, displayTitle)
                                } else {
                                    Toast.makeText(context, "ডাউনলোড লিংক নিচে সিলেক্ট করুন", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডাউনলোড (Save)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Overview Storyline
                    Text(
                        text = "কাহিনী সংক্ষেপ (Storyline)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayOverview,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Casts if available
                    if (!ctgMovie?.casts.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "অভিনয়ে (Cast): ${ctgMovie?.casts}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 3. Provider Download & Streaming Links Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanAccent)
                        Text(
                            text = "প্রোভাইডার ডাউনলোড ও স্ট্রিম লিংক",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "নিচের যেকোনো রেজোলিউশনে ক্লিক করে সরাসরি চালান অথবা ডাউনলোড করুন",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    val allDownloads = extractorInfo?.downloadLinks ?: emptyList()
                    val ctgStream = ctgMovie?.getFullStreamUrl()

                    if (allDownloads.isEmpty() && ctgStream == null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GoldRating)
                                Text(
                                    text = "ডাউনলোড সার্ভার থেকে লিংক ফেচ করা হচ্ছে...",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // If CtgHall URL exists
                        if (ctgStream != null) {
                            DownloadLinkCard(
                                title = "${ctgMovie?.title ?: "Movie"} (Original HD Web-DL)",
                                quality = "1080p Full HD",
                                link = ctgStream,
                                onPlay = {
                                    activeStreamUrl = ctgStream
                                    isPlayingInApp = true
                                },
                                onDownload = {
                                    startDownload(context, ctgStream, "${ctgMovie?.title}.mp4")
                                },
                                onCopy = {
                                    copyToClipboard(context, ctgStream)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Extractor direct & episode links
                        allDownloads.forEach { dl ->
                            DownloadLinkCard(
                                title = dl.title,
                                quality = dl.quality ?: "HD",
                                link = dl.link,
                                onPlay = {
                                    activeStreamUrl = dl.link
                                    isPlayingInApp = true
                                },
                                onDownload = {
                                    startDownload(context, dl.link, "${displayTitle}_${dl.quality}.mp4")
                                },
                                onCopy = {
                                    copyToClipboard(context, dl.link)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 4. TMDB Trailers
            if (trailers.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "অফিসিয়াল ট্রেলার ও ভিডিও (Trailers)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(trailers) { trailer ->
                                Surface(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailer.youtubeUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    color = CinemaSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(180.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = BrandRed,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = trailer.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. TMDB Recommendations / More Like This
            if (recommendations.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "আরো পছন্দ হতে পারে (More Like This)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recommendations) { rec ->
                                Card(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .clickable { onSelectMovie(rec.id) },
                                    colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(rec.fullPosterUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = rec.displayTitle,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                        )
                                        Text(
                                            text = rec.displayTitle,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun DownloadLinkCard(
    title: String,
    quality: String,
    link: String,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = quality,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("স্ট্রিম (Stream)", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডাউনলোড (Save)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun startDownload(context: Context, url: String, fileName: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading from Mukul Plus OTT...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "ডাউনলোড শুরু হয়েছে! (Downloads ফোল্ডারে চেক করুন)", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        // Fallback to browser intent
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ডাউনলোড লিংক ব্রাউজারে খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Download Link", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "লিংক ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
}
