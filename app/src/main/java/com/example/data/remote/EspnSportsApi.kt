package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface EspnSportsApi {
    @GET("apis/site/v2/sports/{sport}/{league}/scoreboard")
    suspend fun getScoreboard(
        @Path("sport") sport: String,
        @Path("league") league: String,
        @Query("dates") dates: String
    ): EspnScoreboardResponse
}

data class LeagueFeedConfig(
    val sport: String,
    val leagueSlug: String,
    val displayName: String,
    val sportCategory: SportCategory
)

enum class SportCategory(val label: String) {
    FOOTBALL("Football"),
    BASKETBALL("Basketball"),
    HOCKEY("Hockey")
}

object SportsApiClient {
    private const val BASE_URL = "https://site.api.espn.com/"

    val supportedLeagues = listOf(
        LeagueFeedConfig("soccer", "uefa.champions", "Ligue des Champions", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "uefa.europa", "Ligue Europa", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "fra.1", "Ligue 1", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "eng.1", "Premier League", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "esp.1", "La Liga", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "ita.1", "Serie A", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "ger.1", "Bundesliga", SportCategory.FOOTBALL),
        LeagueFeedConfig("soccer", "por.1", "Liga Portugal", SportCategory.FOOTBALL),
        LeagueFeedConfig("basketball", "nba", "NBA", SportCategory.BASKETBALL),
        LeagueFeedConfig("hockey", "nhl", "NHL", SportCategory.HOCKEY)
    )

    val api: EspnSportsApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EspnSportsApi::class.java)
    }
}
