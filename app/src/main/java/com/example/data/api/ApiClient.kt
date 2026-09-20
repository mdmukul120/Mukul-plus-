package com.example.data.api

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"
    private const val TMDB_API_KEY = "0b3d17a4fe3dd52593a48d9a0dad4bd6"
    private const val IPTV_URL = "https://raw.githubusercontent.com/abusaeeidx/Ayna-BDIX-IPTV-Playlist/refs/heads/main/ayna-playlist.m3u"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // 1. CtgHall Movies List
    suspend fun fetchCtgMovies(
        library: Int = 1,
        page: Int = 1,
        sort: String = "createdAt",
        sortOrder: String = "DESC",
        search: String? = null,
        year: Int? = null,
        genre: String? = null
    ): CtgMoviesResponse = withContext(Dispatchers.IO) {
        try {
            var url = "https://www.ctghall.com/api/movies?library=$library&fields=id,title,original_title,year,poster_path,release_date,rating,online_rating&sort=$sort&sort_order=$sortOrder&page=$page"
            if (!search.isNullOrBlank()) {
                url += "&search=${java.net.URLEncoder.encode(search, "UTF-8")}"
            }
            if (year != null && year > 0) {
                url += "&year=$year"
            }
            if (!genre.isNullOrBlank()) {
                url += "&genre=${java.net.URLEncoder.encode(genre, "UTF-8")}"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MukulPlus-OTT/1.0")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext CtgMoviesResponse()
            val json = JSONObject(body)

            val total = json.optInt("total", 0)
            val pages = json.optInt("pages", 1)
            val currentPage = json.optInt("current_page", 1)
            val dataArray = json.optJSONArray("data") ?: JSONArray()

            val movies = mutableListOf<CtgMovie>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.optJSONObject(i) ?: continue
                val libObj = item.optJSONObject("Library")
                val lib = if (libObj != null) {
                    CtgLibrary(
                        id = libObj.optInt("id", library),
                        name = libObj.optString("name", "Movies"),
                        type = libObj.optString("type", "MOVIE")
                    )
                } else null

                movies.add(
                    CtgMovie(
                        id = item.optLong("id"),
                        title = item.optString("title", "Untitled"),
                        original_title = item.optString("original_title", null),
                        year = if (item.has("year") && !item.isNull("year")) item.optInt("year") else null,
                        poster_path = item.optString("poster_path", null),
                        backdrop_path = item.optString("backdrop_path", null),
                        release_date = item.optString("release_date", null),
                        online_rating = if (item.has("online_rating")) item.optDouble("online_rating") else null,
                        genre = item.optString("genre", null),
                        Library = lib
                    )
                )
            }
            CtgMoviesResponse(total = total, pages = pages, current_page = currentPage, data = movies)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching CtgMovies", e)
            CtgMoviesResponse()
        }
    }

    // 2. CtgHall Movie Detail
    suspend fun fetchCtgMovieDetail(id: Long): CtgMovie? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.ctghall.com/api/movies/$id"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MukulPlus-OTT/1.0")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val item = JSONObject(body)

            val libObj = item.optJSONObject("Library")
            val lib = if (libObj != null) {
                CtgLibrary(
                    id = libObj.optInt("id", 1),
                    name = libObj.optString("name", "Movies"),
                    type = libObj.optString("type", "MOVIE")
                )
            } else null

            CtgMovie(
                id = item.optLong("id", id),
                title = item.optString("title", "Untitled"),
                original_title = item.optString("original_title", null),
                year = if (item.has("year") && !item.isNull("year")) item.optInt("year") else null,
                poster_path = item.optString("poster_path", null),
                backdrop_path = item.optString("backdrop_path", null),
                release_date = item.optString("release_date", null),
                online_rating = if (item.has("online_rating")) item.optDouble("online_rating") else null,
                user_rating = if (item.has("user_rating")) item.optDouble("user_rating") else null,
                genre = item.optString("genre", null),
                casts = item.optString("casts", null),
                overview = item.optString("overview", null),
                trailers = item.optString("trailers", null),
                url = item.optString("url", null),
                file_path = item.optString("file_path", null),
                imdb_id = item.optString("imdb_id", null),
                tmdb_id = item.optString("tmdb_id", null),
                Library = lib
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching movie detail: $id", e)
            null
        }
    }

    // 3. CtgHall Menus & Filters
    suspend fun fetchCtgMenus(): CtgMenusData = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.ctghall.com/api/menus"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext CtgMenusData()
            val json = JSONObject(body)

            val movieCategories = mutableListOf<CtgCategoryItem>()
            val tvCategories = mutableListOf<CtgCategoryItem>()
            val years = mutableListOf<Int>()
            val movieGenres = mutableListOf<String>()
            val tvGenres = mutableListOf<String>()

            val categoriesObj = json.optJSONObject("categories")
            if (categoriesObj != null) {
                val movieArr = categoriesObj.optJSONArray("movie") ?: JSONArray()
                for (i in 0 until movieArr.length()) {
                    val cat = movieArr.optJSONObject(i) ?: continue
                    movieCategories.add(
                        CtgCategoryItem(
                            id = cat.optInt("id"),
                            name = cat.optString("name"),
                            type = cat.optString("type"),
                            parent = cat.optString("parent")
                        )
                    )
                }
                val tvArr = categoriesObj.optJSONArray("tv") ?: JSONArray()
                for (i in 0 until tvArr.length()) {
                    val cat = tvArr.optJSONObject(i) ?: continue
                    tvCategories.add(
                        CtgCategoryItem(
                            id = cat.optInt("id"),
                            name = cat.optString("name"),
                            type = cat.optString("type"),
                            parent = cat.optString("parent")
                        )
                    )
                }
            }

            val yearsObj = json.optJSONObject("years")
            if (yearsObj != null) {
                val yearsArr = yearsObj.optJSONArray("movie") ?: JSONArray()
                for (i in 0 until yearsArr.length()) {
                    years.add(yearsArr.optInt(i))
                }
            }

            val genresObj = json.optJSONObject("genres")
            if (genresObj != null) {
                val mgArr = genresObj.optJSONArray("movie") ?: JSONArray()
                for (i in 0 until mgArr.length()) {
                    movieGenres.add(mgArr.optString(i))
                }
                val tgArr = genresObj.optJSONArray("tv") ?: JSONArray()
                for (i in 0 until tgArr.length()) {
                    tvGenres.add(tgArr.optString(i))
                }
            }

            CtgMenusData(
                movieCategories = movieCategories,
                tvCategories = tvCategories,
                years = years,
                movieGenres = movieGenres,
                tvGenres = tvGenres
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching menus", e)
            CtgMenusData(
                movieCategories = listOf(
                    CtgCategoryItem(1, "English Movies", "MOVIE"),
                    CtgCategoryItem(4, "Bollywood Movies", "MOVIE"),
                    CtgCategoryItem(5, "Asian & Anime", "MOVIE"),
                    CtgCategoryItem(6, "Bangla Movies", "MOVIE"),
                    CtgCategoryItem(7, "South Indian", "MOVIE")
                ),
                years = (2026 downTo 2010).toList(),
                movieGenres = listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Drama", "Fantasy", "Horror", "Romance", "Sci-Fi", "Thriller")
            )
        }
    }

    // 4. SorryBroRewards Extractor Providers
    suspend fun fetchExtractorProviders(): List<ExtractorProvider> = withContext(Dispatchers.IO) {
        val defaultList = listOf(
            ExtractorProvider("moviesmod", "MoviesMod"),
            ExtractorProvider("topmovies", "TopMovies"),
            ExtractorProvider("uhd", "UHD Movies"),
            ExtractorProvider("moviesdrive", "MoviesDrive"),
            ExtractorProvider("fourkhd", "4K HD"),
            ExtractorProvider("hdhub4u", "HDHub4U"),
            ExtractorProvider("filmyfly", "FilmyFly"),
            ExtractorProvider("kat", "Kat Movie"),
            ExtractorProvider("showbox", "Showbox"),
            ExtractorProvider("castle", "Castle TV"),
            ExtractorProvider("allmovieland", "AllMovieLand")
        )
        try {
            val url = "https://api.sorrybrorewards.com/v2/extractor/api/providers"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext defaultList
            val json = JSONObject(body)
            if (json.optBoolean("success")) {
                val arr = json.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<ExtractorProvider>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    list.add(
                        ExtractorProvider(
                            id = item.optString("id"),
                            name = item.optString("name")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
            defaultList
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching providers", e)
            defaultList
        }
    }

    // 5. Extractor Posts
    suspend fun fetchExtractorPosts(provider: String, page: Int = 1, filter: String = ""): List<ExtractorPost> = withContext(Dispatchers.IO) {
        try {
            val encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8")
            val url = "https://api.sorrybrorewards.com/v2/extractor/api/posts?provider=$provider&filter=$encodedFilter&page=$page"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val posts = mutableListOf<ExtractorPost>()
            if (json.optBoolean("success")) {
                val arr = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    posts.add(
                        ExtractorPost(
                            title = item.optString("title", "Untitled"),
                            link = item.optString("link", ""),
                            image = item.optString("image", null),
                            provider = provider
                        )
                    )
                }
            }
            posts
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extractor posts for $provider", e)
            emptyList()
        }
    }

    // 6. Extractor Movie Info & Download Links
    suspend fun fetchExtractorInfo(link: String, provider: String): ExtractorMovieInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.sorrybrorewards.com/v2/extractor/api/info"
            val payload = JSONObject().apply {
                put("link", link)
                put("provider", provider)
            }
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            if (!json.optBoolean("success")) return@withContext null

            val data = json.optJSONObject("data") ?: return@withContext null
            val title = data.optString("title", "Unknown")
            val synopsis = data.optString("synopsis", null)
            val image = data.optString("image", null)
            val imdbId = data.optString("imdbId", null)
            val type = data.optString("type", null)

            val downloads = mutableListOf<DownloadLink>()
            val streams = mutableListOf<DownloadLink>()

            val linkList = data.optJSONArray("linkList") ?: JSONArray()
            for (i in 0 until linkList.length()) {
                val item = linkList.optJSONObject(i) ?: continue
                val itemTitle = item.optString("title", "Quality Link")
                val quality = item.optString("quality", "HD")
                val episodesLink = item.optString("episodesLink", null)
                if (!episodesLink.isNullOrEmpty()) {
                    downloads.add(DownloadLink(title = itemTitle, link = episodesLink, type = "Episodes", quality = quality))
                }
                val directLinks = item.optJSONArray("directLinks") ?: JSONArray()
                for (j in 0 until directLinks.length()) {
                    val dLink = directLinks.optJSONObject(j) ?: continue
                    val dlTitle = dLink.optString("title", "Download $quality")
                    val dlUrl = dLink.optString("link", "")
                    val dlType = dLink.optString("type", "direct")
                    if (dlUrl.isNotEmpty()) {
                        downloads.add(DownloadLink(title = "$dlTitle ($quality)", link = dlUrl, type = dlType, quality = quality))
                        // Direct video / drive links can also be streamed
                        if (dlUrl.endsWith(".mp4") || dlUrl.endsWith(".mkv") || dlUrl.contains("stream") || dlUrl.contains("hubcloud") || dlUrl.contains("filepress")) {
                            streams.add(DownloadLink(title = "$quality Stream", link = dlUrl, type = dlType, quality = quality))
                        }
                    }
                }
            }

            ExtractorMovieInfo(
                title = title,
                synopsis = synopsis,
                image = image,
                imdbId = imdbId,
                type = type,
                downloadLinks = downloads,
                streamLinks = streams
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extractor info", e)
            null
        }
    }

    // 6b. Extractor Episodes List (POST /api/episodes)
    suspend fun fetchExtractorEpisodes(url: String): List<DownloadLink> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.sorrybrorewards.com/v2/extractor/api/episodes"
            val payload = JSONObject().apply { put("url", url) }
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(reqBody).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val list = mutableListOf<DownloadLink>()
            if (json.optBoolean("success")) {
                val arr = json.optJSONArray("data") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val title = item.optString("title", "Episode ${i + 1}")
                    val link = item.optString("link", "")
                    if (link.isNotEmpty()) {
                        list.add(DownloadLink(title = title, link = link, type = "episode", quality = "HD"))
                    }
                }
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extractor episodes", e)
            emptyList()
        }
    }

    // 6c. Extractor Direct Stream & Download Servers (POST /api/stream)
    suspend fun fetchExtractorStream(url: String): List<DownloadLink> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.sorrybrorewards.com/v2/extractor/api/stream"
            val payload = JSONObject().apply { put("url", url) }
            val reqBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpoint).post(reqBody).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val servers = mutableListOf<DownloadLink>()
            if (json.optBoolean("success")) {
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val serverArr = data.optJSONArray("servers") ?: JSONArray()
                for (i in 0 until serverArr.length()) {
                    val s = serverArr.optJSONObject(i) ?: continue
                    val serverName = s.optString("server", "Server ${i + 1}")
                    val link = s.optString("link", "")
                    val type = s.optString("type", "video")
                    if (link.isNotEmpty()) {
                        servers.add(DownloadLink(title = serverName, link = link, type = type, quality = "Fast DL"))
                    }
                }
            }
            servers
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extractor stream", e)
            emptyList()
        }
    }

    // 7. Ayna BDIX IPTV Channels
    suspend fun fetchIptvChannels(): List<TvChannel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(IPTV_URL).build()
            val response = client.newCall(request).execute()
            val content = response.body?.string() ?: return@withContext emptyList()

            val channels = mutableListOf<TvChannel>()
            val lines = content.lines()
            var currentChannelName = ""
            var currentLogo = ""
            var currentGroup = "General"
            var currentId = ""

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:")) {
                    // Parse tvg-id, tvg-name, tvg-logo, group-title, and channel title
                    val idMatch = Regex("""tvg-id="([^"]*)"""").find(trimmed)
                    currentId = idMatch?.groupValues?.get(1) ?: ""

                    val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmed)
                    currentLogo = logoMatch?.groupValues?.get(1) ?: ""

                    val groupMatch = Regex("""group-title="([^"]*)"""").find(trimmed)
                    currentGroup = groupMatch?.groupValues?.get(1) ?: "General"

                    val commaIndex = trimmed.lastIndexOf(',')
                    currentChannelName = if (commaIndex != -1 && commaIndex + 1 < trimmed.length) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        val nameMatch = Regex("""tvg-name="([^"]*)"""").find(trimmed)
                        nameMatch?.groupValues?.get(1) ?: "Channel"
                    }
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    if (currentChannelName.isNotEmpty()) {
                        channels.add(
                            TvChannel(
                                id = if (currentId.isNotEmpty()) currentId else "ch_${channels.size}",
                                name = currentChannelName,
                                logo = currentLogo.ifEmpty { null },
                                groupTitle = currentGroup.ifEmpty { "General" },
                                streamUrl = trimmed
                            )
                        )
                        currentChannelName = ""
                        currentLogo = ""
                        currentGroup = "General"
                        currentId = ""
                    }
                }
            }
            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IPTV channels", e)
            emptyList()
        }
    }

    // 8. TMDB Multi Search
    suspend fun searchTmdb(query: String): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://api.themoviedb.org/3/search/multi?api_key=$TMDB_API_KEY&language=en-US&query=$encoded&page=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: JSONArray()
            val list = mutableListOf<TmdbSearchResult>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                list.add(
                    TmdbSearchResult(
                        id = item.optLong("id"),
                        title = item.optString("title", null),
                        name = item.optString("name", null),
                        overview = item.optString("overview", null),
                        poster_path = item.optString("poster_path", null),
                        backdrop_path = item.optString("backdrop_path", null),
                        release_date = item.optString("release_date", null),
                        vote_average = if (item.has("vote_average")) item.optDouble("vote_average") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error searching TMDB", e)
            emptyList()
        }
    }

    // 9. TMDB Movie Videos & Trailers
    suspend fun fetchTmdbVideos(movieId: Long): List<TmdbVideo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.themoviedb.org/3/movie/$movieId/videos?api_key=$TMDB_API_KEY&language=en-US"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: JSONArray()
            val list = mutableListOf<TmdbVideo>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                list.add(
                    TmdbVideo(
                        id = item.optString("id"),
                        key = item.optString("key"),
                        name = item.optString("name", "Trailer"),
                        site = item.optString("site", "YouTube"),
                        type = item.optString("type", "Trailer")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB videos", e)
            emptyList()
        }
    }

    // 10. TMDB Recommendations
    suspend fun fetchTmdbRecommendations(movieId: Long): List<TmdbSearchResult> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.themoviedb.org/3/movie/$movieId/recommendations?api_key=$TMDB_API_KEY&language=en-US"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: JSONArray()
            val list = mutableListOf<TmdbSearchResult>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                list.add(
                    TmdbSearchResult(
                        id = item.optLong("id"),
                        title = item.optString("title", null),
                        name = item.optString("name", null),
                        overview = item.optString("overview", null),
                        poster_path = item.optString("poster_path", null),
                        backdrop_path = item.optString("backdrop_path", null),
                        release_date = item.optString("release_date", null),
                        vote_average = if (item.has("vote_average")) item.optDouble("vote_average") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB recommendations", e)
            emptyList()
        }
    }

    // 11. Bongo BD Movies, Web Series & Natoks
    private val bongoMoviesCache = java.util.concurrent.ConcurrentHashMap<Long, CtgMovie>()

    suspend fun getBongoMovieById(id: Long): CtgMovie? {
        if (bongoMoviesCache.containsKey(id)) {
            return bongoMoviesCache[id]
        }
        val list = fetchBongoVideos()
        return list.find { it.id == id }
    }

    suspend fun fetchBongoVideos(): List<CtgMovie> = withContext(Dispatchers.IO) {
        if (bongoMoviesCache.isNotEmpty()) {
            return@withContext bongoMoviesCache.values.toList()
        }

        // Base curated Bongo BD Originals & Hit Web Series with verified streaming links
        val curatedBongoList = mutableListOf(
            CtgMovie(
                id = -90001L,
                title = "Female 4 (2024)",
                original_title = "Female 4 Bongo Original",
                year = 2024,
                poster_path = "https://i.ibb.co/nyfwvrg/20250619-074646.jpg",
                backdrop_path = "https://i.ibb.co/nyfwvrg/20250619-074646.jpg",
                release_date = "2024-04-11",
                online_rating = 9.2,
                user_rating = 9.4,
                genre = "Bongo Original, Comedy, Natok",
                casts = "Ziaul Faruq Apurba, Marzuk Russell, Ziaul Hoque Polash, Saraf Ahmed Zibon",
                overview = "The most popular Bangladeshi comedy franchise from Kajal Arefin Ome and Bongo BD. The Battery Goli boys return with new hilarious misadventures and chaos.",
                url = "https://pub-01c40ff5657b429fa6c8cb74903caf8e.r2.dev/CINEFREAK.NET%20-%20Female%204%20(2024)%20WEB-DL%20[Bengali]%20BongoBD%20720p.mkv",
                imdb_id = "tt32185610"
            ),
            CtgMovie(
                id = -90002L,
                title = "Paap (2023) Web Series",
                original_title = "Paap - Bongo Original Series",
                year = 2023,
                poster_path = "https://cdn.bongo-solutions.com/919f93a7-400e-4149-a70d-204beb589074/content/fedf967e-f5e1-4c72-b394-6cf4229b67aa/2f2a30d5-7f4e-4e13-a378-0c62c3d10ee2.jpg",
                backdrop_path = "https://cdn.bongo-solutions.com/919f93a7-400e-4149-a70d-204beb589074/content/fedf967e-f5e1-4c72-b394-6cf4229b67aa/2f2a30d5-7f4e-4e13-a378-0c62c3d10ee2.jpg",
                release_date = "2023-04-20",
                online_rating = 8.7,
                user_rating = 8.9,
                genre = "Bongo Original, Crime, Mystery, Thriller",
                casts = "Puja Cherry, Zakia Bari Mamo, Aman Reza",
                overview = "A murder mystery set during a lavish family puja celebration. Dark family secrets unfold as the police investigator uncovers shocking betrayal.",
                url = "https://pixeldrain.dev/api/file/jWwZ88kD?download"
            ),
            CtgMovie(
                id = -90003L,
                title = "How Sweet (2025)",
                original_title = "How Sweet - Bongo BD Original",
                year = 2025,
                poster_path = "https://i.ibb.co/Nn3BsvW5/20250601-131751.jpg",
                backdrop_path = "https://i.ibb.co/Nn3BsvW5/20250601-131751.jpg",
                release_date = "2025-01-14",
                online_rating = 8.8,
                user_rating = 9.0,
                genre = "Bongo BD, Romance, Drama",
                casts = "Ziaul Faruq Apurba, Tasnia Farin",
                overview = "A romantic journey of two contrasting souls discovering unexpected chemistry through sweet and bittersweet moments in modern Dhaka.",
                url = "https://pub-01c40ff5657b429fa6c8cb74903caf8e.r2.dev/TG:%20@SR_PREMIUM%20-%20How%20Sweet%20(2025)%20WEB-DL%20[Bengali]%20BongoBD%202160p.mkv"
            ),
            CtgMovie(
                id = -90004L,
                title = "Female 3 (2023)",
                original_title = "Female 3 Bongo Original",
                year = 2023,
                poster_path = "https://i.ibb.co/tTXCYTky/20250619-074623.jpg",
                backdrop_path = "https://i.ibb.co/tTXCYTky/20250619-074623.jpg",
                release_date = "2023-06-29",
                online_rating = 9.1,
                user_rating = 9.3,
                genre = "Bongo Original, Comedy, Natok",
                casts = "Marzuk Russell, Mishu Sabbir, Ziaul Hoque Polash, Chashi Alam",
                overview = "The Battery Goli residents deal with the grand arrival of an unexpected marriage proposal and street gang rivalry.",
                url = "https://pixeldrain.dev/api/file/ncAaX1fE?download"
            ),
            CtgMovie(
                id = -90005L,
                title = "Shit Happens - Kapjhap (2024)",
                original_title = "Kapjhap - Bongo Web Series",
                year = 2024,
                poster_path = "https://i.ibb.co.com/MpkLG6n/Shit-Happens-Kapjhap-2024-S01-E01-03-Bengali-Dubbed-ORG-Bongo-WEB-DL-H264-AAC-1080p-720p-480p-Downlo.webp.webp",
                backdrop_path = "https://i.ibb.co.com/MpkLG6n/Shit-Happens-Kapjhap-2024-S01-E01-03-Bengali-Dubbed-ORG-Bongo-WEB-DL-H264-AAC-1080p-720p-480p-Downlo.webp.webp",
                release_date = "2024-05-18",
                online_rating = 8.5,
                user_rating = 8.6,
                genre = "Bongo Series, Comedy, Drama",
                casts = "Shamol Mawla, Nazia Haque Ova",
                overview = "Bongo exclusive youth series packed with rapid comic turns, relationship dilemmas, and contemporary city life mishaps.",
                url = "https://pixeldrain.dev/api/file/zsgTirVb?download"
            ),
            CtgMovie(
                id = -90006L,
                title = "Surongo (2023)",
                original_title = "Surongo - Superhit Movie",
                year = 2023,
                poster_path = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT7UkHJogQ1QOihEwmK67-zQz9Pk3ie1408anxnX7aekiaWq9VhpJV1MiW2&s=10",
                backdrop_path = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT7UkHJogQ1QOihEwmK67-zQz9Pk3ie1408anxnX7aekiaWq9VhpJV1MiW2&s=10",
                release_date = "2023-06-29",
                online_rating = 9.3,
                user_rating = 9.5,
                genre = "Bangla Movie, Crime, Thriller, Heist",
                casts = "Afran Nisho, Toma Mirza, Mostafa Monwar",
                overview = "Masud, an electrician, goes to extreme lengths to satisfy his wife's ambitions, orchestrating an audacious bank tunnel heist that grips the nation.",
                url = "https://pixeldrain.dev/api/file/XLydnsWL?download"
            ),
            CtgMovie(
                id = -90007L,
                title = "Female 2 (2022)",
                original_title = "Female 2 Bongo Hit",
                year = 2022,
                poster_path = "https://i.ibb.co/hFYFJbVw/20250619-074628.jpg",
                backdrop_path = "https://i.ibb.co/hFYFJbVw/20250619-074628.jpg",
                release_date = "2022-07-10",
                online_rating = 8.9,
                user_rating = 9.0,
                genre = "Bongo Original, Comedy",
                casts = "Marzuk Russell, Ziaul Hoque Polash, Chashi Alam",
                overview = "The second chapter of the hilarious Battery Goli story featuring crazy local election politics and wedding confusion.",
                url = "https://pixeldrain.dev/api/file/QKZZNfDc?download"
            ),
            CtgMovie(
                id = -90008L,
                title = "Female (2021)",
                original_title = "Female - The Beginning",
                year = 2021,
                poster_path = "https://i.ibb.co/9zb4T2M/20250619-074638.jpg",
                backdrop_path = "https://i.ibb.co/9zb4T2M/20250619-074638.jpg",
                release_date = "2021-07-21",
                online_rating = 9.0,
                user_rating = 9.2,
                genre = "Bongo Original, Comedy, Natok",
                casts = "Marzuk Russell, Polash, Mishu Sabbir",
                overview = "Where it all started! The sudden presence of an attractive newcomer shakes the bachelor neighborhood to its core.",
                url = "https://pixeldrain.dev/api/file/FDX9JfNn?download"
            ),
            CtgMovie(
                id = -90009L,
                title = "Meyeti Ekhon Kothay Jabe (2017)",
                original_title = "Meyeti Ekhon Kothay Jabe BongoBD",
                year = 2017,
                poster_path = "https://i.ibb.co/yT4Bxbg/20250531-202326.jpg",
                backdrop_path = "https://i.ibb.co/yT4Bxbg/20250531-202326.jpg",
                release_date = "2017-03-10",
                online_rating = 8.3,
                user_rating = 8.5,
                genre = "BongoBD, Drama, Romance",
                casts = "Shahriaz, Falguni Rahman Jolly, Raisul Islam Asad",
                overview = "An acclaimed Bangladeshi drama of love, society, and destiny in riverine coastal Bangladesh.",
                url = "https://pub-01c40ff5657b429fa6c8cb74903caf8e.r2.dev/CINEFREAK.TOP%20-%20Meyeti%20Ekhon%20Kothay%20Jabe%20(2017)%20WEB-DL%20[Bengali]%20BongoBD%201080p.mkv"
            ),
            CtgMovie(
                id = -90010L,
                title = "Paap Kahini (2025)",
                original_title = "Paap Kahini S01",
                year = 2025,
                poster_path = "https://i.ibb.co/7N6GQ399/20250610-085120.jpg",
                backdrop_path = "https://i.ibb.co/7N6GQ399/20250610-085120.jpg",
                release_date = "2025-02-01",
                online_rating = 8.6,
                user_rating = 8.8,
                genre = "Bongo BD, Crime, Mystery",
                casts = "Shohel Mondol, Nazifa Tushi",
                overview = "A dark psychological thriller unveiling deep rooted corruption and deceit behind an unsolved kidnapping.",
                url = "https://pixeldrain.dev/api/file/uXj8tKaq?download"
            ),
            CtgMovie(
                id = -90011L,
                title = "Bhanumathi & Ramakrishna",
                original_title = "Tukhor Premer Golpo - Bongo",
                year = 2024,
                poster_path = "https://i.ibb.co.com/12SMnv8/Bhanumathi-Ramakrishna-Tukhor-Premer-Golpo-2024-Bengali-Dubbed-ORG-Bongo-WEB-DL-H264-AAC-1080p-720p.webp",
                backdrop_path = "https://i.ibb.co.com/12SMnv8/Bhanumathi-Ramakrishna-Tukhor-Premer-Golpo-2024-Bengali-Dubbed-ORG-Bongo-WEB-DL-H264-AAC-1080p-720p.webp",
                release_date = "2024-03-01",
                online_rating = 8.4,
                user_rating = 8.5,
                genre = "Bongo Dubbed, Romance, Drama",
                casts = "Naveen Chandra, Salony Luthra",
                overview = "A sweet, mature love story of thirty-somethings tackling expectations, career pressures, and romance.",
                url = "https://pixeldrain.dev/api/file/zg8ibJLR?download"
            ),
            CtgMovie(
                id = -90012L,
                title = "Dramadol (2015)",
                original_title = "Dramadol Bangla Natok",
                year = 2015,
                poster_path = "https://i.ibb.co/Jw8h8jx2/20250527-180848.jpg",
                backdrop_path = "https://i.ibb.co/Jw8h8jx2/20250527-180848.jpg",
                release_date = "2015-08-12",
                online_rating = 8.7,
                user_rating = 8.9,
                genre = "Bangla Natok, Comedy",
                casts = "Mosharraf Karim, Robena Reza Jui",
                overview = "Mosharraf Karim's timeless comedy drama depicting theater troupe rivalries and quirky backstage relationships.",
                url = "https://pixeldrain.dev/api/file/D2yyrEVX?download"
            )
        )

        // Dynamically fetch and merge additional Bongo titles from live playlist
        try {
            val playlistUrl = "https://raw.githubusercontent.com/abusaeeidx/Movie-Playlist-Auto-update/main/Bangla_Movies.m3u"
            val req = Request.Builder().url(playlistUrl).build()
            val res = client.newCall(req).execute()
            val body = res.body?.string()
            if (!body.isNullOrEmpty()) {
                var nextDynamicId = -91000L
                var currentTitle = ""
                var currentLogo = ""
                for (line in body.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF")) {
                        if (trimmed.contains("tvg-logo=\"")) {
                            currentLogo = trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
                        }
                        val namePart = trimmed.substringAfterLast(",")
                        if (namePart.isNotBlank()) {
                            currentTitle = namePart.trim()
                        }
                    } else if (trimmed.startsWith("http")) {
                        if (currentTitle.isNotBlank() && (
                            currentTitle.contains("bongo", ignoreCase = true) ||
                            currentTitle.contains("female", ignoreCase = true) ||
                            currentTitle.contains("paap", ignoreCase = true) ||
                            trimmed.contains("bongo", ignoreCase = true)
                        )) {
                            if (curatedBongoList.none { it.title.equals(currentTitle, ignoreCase = true) }) {
                                curatedBongoList.add(
                                    CtgMovie(
                                        id = nextDynamicId--,
                                        title = currentTitle,
                                        original_title = "Bongo BD Video",
                                        year = 2024,
                                        poster_path = currentLogo.ifBlank { "https://i.ibb.co/nyfwvrg/20250619-074646.jpg" },
                                        backdrop_path = currentLogo.ifBlank { "https://i.ibb.co/nyfwvrg/20250619-074646.jpg" },
                                        online_rating = 8.5,
                                        genre = "Bongo BD, Bangla",
                                        url = trimmed,
                                        overview = "$currentTitle - Stream exclusively on Mukul Plus Bongo BD section."
                                    )
                                )
                            }
                        }
                        currentTitle = ""
                        currentLogo = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching dynamic Bongo playlist", e)
        }

        curatedBongoList.forEach { movie ->
            bongoMoviesCache[movie.id] = movie
        }

        curatedBongoList
    }
}
