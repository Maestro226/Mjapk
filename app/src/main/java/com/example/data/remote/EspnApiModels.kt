package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EspnScoreboardResponse(
    val leagues: List<EspnLeague>? = null,
    val events: List<EspnEvent>? = null
)

@JsonClass(generateAdapter = true)
data class EspnLeague(
    val id: String? = null,
    val name: String? = null,
    val abbreviation: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnEvent(
    val id: String = "",
    val date: String = "",
    val name: String = "",
    val shortName: String? = null,
    val competitions: List<EspnCompetition>? = null,
    val status: EspnStatus? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetition(
    val id: String? = null,
    val date: String? = null,
    val competitors: List<EspnCompetitor>? = null,
    val odds: List<EspnOdds>? = null,
    val status: EspnStatus? = null
)

@JsonClass(generateAdapter = true)
data class EspnCompetitor(
    val id: String? = null,
    val homeAway: String? = null,
    val score: String? = null,
    val form: String? = null,
    val team: EspnTeam? = null,
    val records: List<EspnRecord>? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeam(
    val id: String? = null,
    val displayName: String? = null,
    val shortDisplayName: String? = null,
    val abbreviation: String? = null,
    val logo: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnRecord(
    val name: String? = null,
    val type: String? = null,
    val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class EspnOdds(
    val details: String? = null,
    val overUnder: Double? = null,
    val spread: Double? = null,
    val homeTeamOdds: EspnTeamOdds? = null,
    val awayTeamOdds: EspnTeamOdds? = null,
    val drawOdds: EspnDrawOdds? = null
)

@JsonClass(generateAdapter = true)
data class EspnTeamOdds(
    val moneyLine: Double? = null,
    val favorite: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class EspnDrawOdds(
    val moneyLine: Double? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatus(
    val type: EspnStatusType? = null
)

@JsonClass(generateAdapter = true)
data class EspnStatusType(
    val state: String? = null,
    val completed: Boolean? = null,
    val shortDetail: String? = null
)
