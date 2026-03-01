package com.toelve.doas.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.*
import com.toelve.doas.soasa.ImageProcessor
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class AbsensiManager(
    private val activity: FragmentActivity,
    private val previewView: PreviewView
) {

    var lat: Double? = null
    var lon: Double? = null
    var alamat: String = "Alamat tidak tersedia"
    var accuracy: Float? = null
    private var lastAddressLocation: Location? = null
    // CALLBACK KE UI
    var onGpsStatusChanged: ((Float?) -> Unit)? = null

    val capturedFiles = mutableListOf<File>()
    private val maxFoto = 2

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var locationCallback: LocationCallback? = null

    private val locationClient =
        LocationServices.getFusedLocationProviderClient(activity)

    /* ================= PERMISSION ================= */

    private val locationPermission =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                requestLocationInternal()
            } else {
                toast("Izin lokasi wajib")
            }
        }

    private val cameraPermission =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCameraInternal()
                ensureLocation()
            } else toast("Izin kamera wajib")
        }

    /* ================= BIOMETRIC ================= */

    fun authenticate(onSuccess: () -> Unit) {

        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onSuccess()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifikasi Sidik Jari")
            .setSubtitle("Konfirmasi absensi")
            .setNegativeButtonText("Batal")
            .build()

        prompt.authenticate(info)
    }

    /* ================= PREPARE ================= */

    fun prepare() {
        capturedFiles.clear()
        ensureCamera()
    }

    /* ================= CAPTURE ================= */

    fun capture(onPreview: (Uri, Int) -> Unit) {

        if (capturedFiles.size >= maxFoto) {
            toast("Maksimal 2 foto")
            return
        }

        val kamera = imageCapture ?: run {
            toast("Kamera belum siap")
            return
        }

        if (lat == null || lon == null) {
            toast("Lokasi belum tersedia")
            return
        }

        // ===== FILE AWAL DARI CAMERA =====
        val file = createImageFile()
        val options = ImageCapture.OutputFileOptions.Builder(file).build()

        kamera.takePicture(
            options,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {

                    // ===== PROSES + CONVERT WEBP =====
                    val processedFile =
                        ImageProcessor.processAndSave(
                            file = file,
                            isFrontCamera =
                                (lensFacing == CameraSelector.LENS_FACING_FRONT),
                            alamat = alamat,
                            lat = lat,
                            lon = lon
                        )

                    activity.runOnUiThread {

                        // ===== SIMPAN FILE FINAL =====
                        capturedFiles.add(processedFile)

                        // ===== PREVIEW =====
                        onPreview(
                            Uri.fromFile(processedFile),
                            capturedFiles.size
                        )
                    }
                }

                override fun onError(e: ImageCaptureException) {
                    activity.runOnUiThread {
                        toast("Gagal ambil foto")
                    }
                }
            }
        )
    }
    /* ================= LOCATION ================= */

    @SuppressLint("MissingPermission")
    private fun requestLocationInternal() {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).setMinUpdateIntervalMillis(1000)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                lat = loc.latitude
                lon = loc.longitude
                accuracy = loc.accuracy

                // kirim ke UI
                onGpsStatusChanged?.invoke(accuracy)

                val currentLoc = Location("current").apply {
                    latitude = lat!!
                    longitude = lon!!
                }

                val shouldUpdateAddress =
                    lastAddressLocation == null ||
                            lastAddressLocation!!.distanceTo(currentLoc) > 30f

                if (shouldUpdateAddress) {

                    lastAddressLocation = currentLoc

                    resolveAddressOnline(lat!!, lon!!) {
                        alamat = it
                    }
                }
            }
        }

        locationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            activity.mainLooper
        )
    }

    fun stop() {

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}

        try {
            locationCallback?.let {
                locationClient.removeLocationUpdates(it)
            }
        } catch (_: Exception) {}

        try {
            if (!cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
            }
        } catch (_: Exception) {}
    }

    private fun resolveAddressOnline(
        lat: Double,
        lon: Double,
        onResult: (String) -> Unit
    ) {

        val url =
            "https://nominatim.openstreetmap.org/reverse" +
                    "?format=jsonv2" +
                    "&lat=$lat&lon=$lon"

        val req = object :
            com.android.volley.toolbox.StringRequest(
                Method.GET,
                url,
                { response ->

                    try {
                        val obj = org.json.JSONObject(response)

                        val result =
                            obj.optString(
                                "display_name",
                                "Alamat tidak tersedia"
                            )

                        onResult(result)

                    } catch (_: Exception) {
                        onResult("Alamat tidak tersedia")
                    }
                },
                {
                    onResult("Alamat tidak tersedia")
                }
            ) {

            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "User-Agent" to "DOAS-App"
                )
            }
        }

        com.android.volley.toolbox.Volley
            .newRequestQueue(activity)
            .add(req)
    }
    private fun ensureLocation() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationInternal()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /* ================= CAMERA ================= */

    private fun startCameraInternal() {

        val future = ProcessCameraProvider.getInstance(activity)

        future.addListener({

            cameraProvider = future.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider?.unbindAll()

            cameraProvider?.bindToLifecycle(
                activity,
                selector,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(activity))
    }

    fun switchCamera() {
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK
            else CameraSelector.LENS_FACING_FRONT

        startCameraInternal()
    }

    private fun ensureCamera() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCameraInternal()
            ensureLocation()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun createImageFile(): File {
        val dir = getDoasDir(activity)
        return File(dir, "CameraX_${System.currentTimeMillis()}.webp")
    }

    private fun getDoasDir(context: Context): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "DOAS"
        )

        if (!dir.exists()) {
            dir.mkdirs()   // WAJIB supaya tidak ENOENT
        }

        return dir
    }


    private fun toast(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}
