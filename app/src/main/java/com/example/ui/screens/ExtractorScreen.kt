package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.ExtractorPost
import com.example.data.model.ExtractorProvider
import com.example.ui.components.FilterChipItem
import com.example.ui.theme.*

@Composable
fun ExtractorScreen(
    onSelectPost: (ExtractorPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var providers by remember { mutableStateOf<List<ExtractorProvider>>(emptyList()) }
    var selectedProvider by remember { mutableStateOf("moviesmod") }
    var posts by remember { mutableStateOf<List<ExtractorPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }

    // Load Providers list
    LaunchedEffect(Unit) {
        providers = ApiClient.fetchExtractorProviders()
        if (providers.isNotEmpty() && selectedProvider.isEmpty()) {
            selectedProvider = providers.first().id
        }
    }

    // Load Posts for selected provider
    LaunchedEffect(selectedProvider, currentPage, searchQuery) {
        isLoading = true
        posts = ApiClient.fetchExtractorPosts(
            provider = selectedProvider,
            page = currentPage,
            filter = searchQuery
        )
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "মুভি এক্সট্রাক্টর ও ডাউনলোড প্রোভাইডার",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "বিভিন্ন প্রোভাইডার থেকে সরাসরি হাই-স্পিড ডাউনলোড ও স্ট্রিম লিংক",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search within provider
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    currentPage = 1
                },
                placeholder = { Text("প্রোভাইডারে খুঁজুন (Search Title)", color = TextMuted, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; currentPage = 1 }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CinemaSurface,
                    unfocusedContainerColor = CinemaSurface,
                    focusedBorderColor = BrandRed,
                    unfocusedBorderColor = CinemaBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Providers horizontal scroll
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(providers) { provider ->
                    FilterChipItem(
                        label = provider.name,
                        selected = selectedProvider == provider.id,
                        onClick = {
                            selectedProvider = provider.id
                            currentPage = 1
                        }
                    )
                }
            }
        }

        // Posts Grid
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandRed)
            }
        } else if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("কোনো মুভি পাওয়া যায়নি", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            currentPage = 1
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Text("পুনরায় চেষ্টা করুন")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPost(post) },
                        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column {
                            // Poster image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
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

                                // Provider tag badge
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

                                // Download & Stream icon button
                                Surface(
                                    color = Color(0xCC000000),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = CyanAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "LINKS",
                                            color = CyanAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Post Title
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = post.title,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Pagination Row
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentPage > 1) {
                            OutlinedButton(
                                onClick = { currentPage -= 1 },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Previous Page")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = "Page $currentPage",
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { currentPage += 1 },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) {
                            Text("Next Page")
                        }
                    }
                }
            }
        }
    }
}
