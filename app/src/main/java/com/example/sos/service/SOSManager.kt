package com.example.sos.service

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SOSManager {
    private var recording: Recording? = null

    suspend fun triggerSOS(context: Context) {
        try {
            // Get location (either from GPS or IP)
            val location = try {
                getCurrentLocation(context)
            } catch (e: Exception) {
                Log.e("SOSManager", "Error getting GPS location: ${e.message}")
                // Fallback to IP geolocation
                val ipAddress = getPublicIpAddress()
                val geoLocation = ipAddress?.let { getGeolocation(it) }

                if (geoLocation != null) {
                    Location("ip-geolocation").apply {
                        latitude = geoLocation.getDouble("lat")
                        longitude = geoLocation.getDouble("lon")
                    }
                } else {
                    null
                }
            }

            // Send SOS request with location
            sendSOSRequest(context, location)

            // Start recording video and audio
            startRecording(context)

        } catch (e: Exception) {
            Log.e("SOSManager", "Error triggering SOS: ${e.message}")
        }
    }

    private suspend fun getCurrentLocation(context: Context): Location = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(location)
            } else {
                continuation.resumeWithException(Exception("Location is null"))
            }
        }.addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
    }

    private suspend fun getPublicIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getGeolocation(ip: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/$ip")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                val response = reader.readText()
                JSONObject(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun sendSOSRequest(context: Context, location: Location?) = withContext(Dispatchers.IO) {
        try {
            // In a real app, this would use Retrofit to call your backend API
            // For now, we'll just log it
            val locationStr = if (location != null) {
                "${location.latitude},${location.longitude}"
            } else {
                "Unknown location"
            }

            Log.d("SOSManager", "SOS request sent with location: $locationStr")

            // Here's where you would make the actual API call:
            /*
            val apiService = RetrofitClient.create()
            val sosRequest = SOSRequest(
                userId = getUserId(context),
                location = locationStr,
                timestamp = System.currentTimeMillis()
            )
            val response = apiService.sendSOS(sosRequest)
            */
        } catch (e: Exception) {
            Log.e("SOSManager", "Error sending SOS request: ${e.message}")
        }
    }

    private fun startRecording(context: Context) {
        // This is a simplified version - in a real app, you'd implement proper video recording
        // using CameraX. The implementation would require a lifecycle owner and UI surfaces.
        Log.d("SOSManager", "Video recording started")

        // In a real implementation, you would:
        // 1. Initialize CameraX
        // 2. Configure video capture use case
        // 3. Start recording
        // 4. Set up a way to send the video to your backend
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }
}