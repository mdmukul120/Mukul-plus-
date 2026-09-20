package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.HeroSlider
import com.example.ui.components.MoviePosterCard
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    mediaRepository: MediaRepository,
    onSelectMovie: (Long) -> Unit,
    onSelectPost: (ExtractorPost) -> Unit,
    onSelectChannel: (TvChannel) -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToLiveTv: () -> Unit,
    onNavigateToExtractor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var trendingMovies by remember { mutableStateOf<List<CtgMovie>>(emptyList()) }
    var bollywoodMovies by remember { mutableStateOf<List<CtgMovie>>(emptyList()) }
    var banglaMovies by remember { mutableStateOf<List<CtgMovie>>(emptyList()) }
    var providerPosts by remember { mutableStateOf<List<ExtractorPost>>(emptyList()) }
    var liveChannels by remember { mutableStateOf<List<TvChannel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val favorites by mediaRepository.favorites.collectAsState()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Fetch trending
            val trendingRes = ApiClient.fetchCtgMovies(library = 1, page = 1, sort = "createdAt")
            trendingMovies = trendingRes.data

            // Fetch bollywood
            val bollywoodRes = ApiClient.fetchCtgMovies(library = 4, page = 1, sort = "createdAt")
            bollywoodMovies = bollywoodRes.data

            // Fetch bangla
            val banglaRes = ApiClient.fetchCtgMovies(library = 6, page = 1, sort = "createdAt")
            banglaMovies = banglaRes.data

            // Fetch extractor posts
            providerPosts = ApiClient.fetchExtractorPosts("moviesmod", page = 1)

            // Fetch TV channels
            liveChannels = mediaRepository.getChannels()
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        // 1. Hero Image Slider / Carousel
        item {
            HeroSlider(
                movies = trendingMovies,
                onMovieClick = { movie -> onSelectMovie(movie.id) },
                onWatchlistToggle = { movie -> mediaRepository.toggleFavorite(movie.id) },
                isFavorite = { id -> mediaRepository.isFavorite(id) }
            )
        }

        // 2. Trending & New Releases Section
        item {
            SectionHeader(
                title = "🔥 ট্রেন্ডিং ও নতুন রিলিজ (Trending)",
                subtitle = "CtgHall এক্সক্লুসিভ কালেকশন",
                onSeeAllClick = onNavigateToMovies
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(trendingMovies) { movie ->
                    MoviePosterCard(
                        movie = movie,
                        onClick = { onSelectMovie(movie.id) }
                    )
                }
            }
        }

        // 3. Live TV Highlights Section
        if (liveChannels.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "📺 লাইভ টিভি চ্যানেল (BDIX Live TV)",
                    subtitle = "স্পোর্টস, নিউজ ও এন্টারটেইনমেন্ট",
                    onSeeAllClick = onNavigateToLiveTv
                )
                val sportsAndNews = remember(liveChannels) {
                    liveChannels.filter { it.groupTitle.contains("Sports", ignoreCase = true) || it.groupTitle.contains("News", ignoreCase = true) }.take(10)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sportsAndNews) { channel ->
                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onSelectChannel(channel) },
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color.Black, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!channel.logo.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(channel.logo)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = channel.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "🔴 LIVE",
                                    color = BrandRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Bollywood & Bangla Movies Section
        if (bollywoodMovies.isNotEmpty() || banglaMovies.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "🍿 বলিউড ও বাংলা মুভি (Desi Hits)",
                    subtitle = "হিন্দি ও বাংলা এইচডি মুভি",
                    onSeeAllClick = onNavigateToMovies
                )
                val desiList = remember(bollywoodMovies, banglaMovies) {
                    (banglaMovies.take(5) + bollywoodMovies.take(8))
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(desiList) { movie ->
                        MoviePosterCard(
                            movie = movie,
                            onClick = { onSelectMovie(movie.id) }
                        )
                    }
                }
            }
        }

        // 5. Multi-Provider Extractor Posts Section
        if (providerPosts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "⚡ প্রোভাইডার ডাউনলোড লিংক (Downloads)",
                    subtitle = "MoviesMod, TopMovies, UHD ও অন্যান্য",
                    onSeeAllClick = onNavigateToExtractor
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(providerPosts.take(8)) { post ->
                        Card(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { onSelectPost(post) },
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(175.dp)
                                        .background(CinemaSurfaceVariant)
                                ) {
                                    if (!post.image.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(post.image)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = post.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Surface(
                                        color = BrandRed,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = post.provider.uppercase(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = post.title,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
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

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "সব দেখুন >",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
