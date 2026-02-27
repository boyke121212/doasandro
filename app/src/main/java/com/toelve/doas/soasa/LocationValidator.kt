package com.toelve.doas.soasa

import kotlin.math.*

data class LocationValidationResult(
    val isValid: Boolean,
    val distanceMeter: Double = 0.0,
    val message: String = ""
)

object LocationValidator {

    private const val EARTH_RADIUS_METER = 6371000.0

    fun validateAbsensiLocation(
        userLat: Double?,
        userLon: Double?,
        accuracy: Double?,
        officeLat: Double?,
        officeLon: Double?,
        maxAccuracyMeter: Double = 30.0,
        officeRadiusMeter: Double = 100.0
    ): LocationValidationResult {

        // 1️⃣ Validasi data user
        if (userLat == null || userLon == null || accuracy == null) {
            return LocationValidationResult(
                isValid = false,
                message = "Lokasi pengguna belum tersedia"
            )
        }

        // 2️⃣ Validasi data kantor
        if (officeLat == null || officeLon == null) {
            return LocationValidationResult(
                isValid = false,
                message = "Lokasi kantor belum siap"
            )
        }

        // 3️⃣ Validasi akurasi GPS
        if (accuracy > maxAccuracyMeter) {
            return LocationValidationResult(
                isValid = false,
                message = "Akurasi GPS rendah (${accuracy.toInt()} m). Pindah ke area terbuka."
            )
        }

        // 4️⃣ Hitung jarak (Haversine)
        val distance = haversineDistance(
            officeLat,
            officeLon,
            userLat,
            userLon
        )

        // 5️⃣ Validasi radius kantor
        if (distance > officeRadiusMeter) {
            return LocationValidationResult(
                isValid = false,
                distanceMeter = distance,
                message = "Anda berada di luar radius kantor (${distance.toInt()} m)"
            )
        }

        // ✅ LOLOS SEMUA VALIDASI
        return LocationValidationResult(
            isValid = true,
            distanceMeter = distance
        )
    }

    private fun haversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METER * c
    }
}
