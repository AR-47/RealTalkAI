package com.example.realtalkai

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.Executors

object GoogleTTSHelper {
    data class VoiceRequest(val input: Input, val voice: VoiceConfig, val audioConfig: AudioConfig)
    data class Input(val text: String? = null, val ssml: String? = null)
    data class VoiceConfig(val languageCode: String, val name: String)
    data class AudioConfig(val audioEncoding: String, val speakingRate: Double, val pitch: Double)
    data class VoiceResponse(val audioContent: String)

    private val executor = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient()
    private const val apiKey = "AIzaSyDcuPPOmiLJAzdmvDFgmpTGQPLlDX3bsjo"

    // This is the simple speak function with no callback
    fun speak(context: Context, text: String) {
        executor.execute {
            try {
                val inputData = if (text.trim().startsWith("<speak>", ignoreCase = true)) {
                    Input(ssml = text)
                } else {
                    Input(text = text)
                }
                val voiceRequest = VoiceRequest(
                    input = inputData,
                    voice = VoiceConfig(languageCode = "en-US", name = "en-US-Studio-O"),
                    audioConfig = AudioConfig(audioEncoding = "MP3", speakingRate = 1.0, pitch = -2.0)
                )
                val json = Gson().toJson(voiceRequest)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                    .post(requestBody)
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("TTS_ERROR", "API Error: ${response.code} ${response.body?.string()}")
                    return@execute
                }
                val responseBody = response.body?.string()
                val audioContent = Gson().fromJson(responseBody, VoiceResponse::class.java).audioContent
                val audioBytes = Base64.decode(audioContent, Base64.DEFAULT)
                val tempFile = File.createTempFile("tts", ".mp3", context.cacheDir)
                tempFile.writeBytes(audioBytes)
                MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e("TTS_ERROR", "Failed to speak: ${e.message}", e)
            }
        }
    }
}