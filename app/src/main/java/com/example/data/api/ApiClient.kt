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
}
