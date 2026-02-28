package com.toelve.doas.model

import com.google.gson.annotations.SerializedName

data class AuthCheckResponse(
    @SerializedName("status") val status: Any?,
    @SerializedName("dashboard") val dashboard: DashboardData?
)

data class DashboardData(
    @SerializedName("tahunAktif") val tahunAktif: Int?,
    @SerializedName("bulanAwal") val bulanAwal: Int?,
    @SerializedName("summarySubdit") val summarySubdit: Map<String, SummarySubditData>?,
    @SerializedName("dataMap") val dataMap: Map<String, Map<String, Map<String, String>>>?,
    @SerializedName("userTerakhir") val userTerakhir: List<UserTerakhir>?
)

data class SummarySubditData(
    @SerializedName("diajukan") val diajukan: Double?,
    @SerializedName("terserap") val terserap: Double?,
    @SerializedName("persen") val persen: Double?
)

data class UserTerakhir(
    @SerializedName("pangkat") val pangkat: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("jabatan") val jabatan: String?,
    @SerializedName("subdit") val subdit: String?,
    @SerializedName("createdDtm") val createdDtm: String?
)
