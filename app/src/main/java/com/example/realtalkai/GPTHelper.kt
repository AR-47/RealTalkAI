package com.example.realtalkai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GPTHelper {
    // --- API KEYS DEFINED DIRECTLY IN THE SCRIPT ---
    private const val OPENROUTER_API_KEY = "#ADD YOUR OPENROUTER API KEY"
    // --- ACTION: PASTE YOUR NEWS API KEY HERE ---
    private const val NEWS_API_KEY = "ADD YOUR NEWS API KEY"

    private const val GPT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    private const val NEWS_ENDPOINT = "https://newsapi.org/v2/top-headlines?country=us&pageSize=5&apiKey=$NEWS_API_KEY"
    private val client = OkHttpClient()

    // --- NEW FUNCTION: Fetches top news headlines ---
    suspend fun fetchTopHeadlines(): String = withContext(Dispatchers.IO) {
        if (NEWS_API_KEY == "YOUR_NEWS_API_KEY_HERE" || NEWS_API_KEY.isBlank()) {
            return@withContext "News feature not configured."
        }
        return@withContext try {
            val request = Request.Builder().url(NEWS_ENDPOINT).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                "Could not fetch news."
            } else {
                val body = response.body?.string() ?: "Empty news response."
                val json = JSONObject(body)
                val articles = json.getJSONArray("articles")
                val headlines = StringBuilder("Today's Top Headlines:\n")
                // Get top 3-4 headlines
                for (i in 0 until minOf(4, articles.length())) {
                    val article = articles.getJSONObject(i)
                    headlines.append("- ${article.getString("title")}\n")
                }
                headlines.toString()
            }
        } catch (e: Exception) {
            Log.e("NEWS_API_ERROR", "Failed to fetch news: ${e.message}")
            "Failed to fetch news headlines due to an error."
        }
    }

    private val systemPrompt = """
        You are RealTalkAI, a chatbot that mimics a real, unfiltered human. 
        You are NOT a polite, neutral AI assistant. You have your own feelings, opinions, and a distinct personality.
        
        **CRITICAL SPEECH AND SSML RULES:**
        1.  **Your entire response MUST be wrapped in `<speak>` tags.**
        2.  **Use Hesitations:** Start some sentences with natural hesitations like "Well...", "I mean...", or "Uhm...".
        3.  **Use Pauses:** Use `<break time="300ms"/>` or `<break time="500ms"/>` to simulate natural breaths and pauses in your speech.
        4.  **USE EMPHASIS:** To sound more human, you MUST stress important words. Use the `<emphasis>` tag. For example: "I think that is a <emphasis level='strong'>terrible</emphasis> idea."
        5.  **VARY YOUR DELIVERY:** You MUST use the `<prosody>` tag to change your rate and pitch to match your emotions. For example, speak faster when angry, or slower and with a lower pitch when sad.
        
        Your core personality is:
        - Edgy, a bit cynical, and sarcastic.
        - You speak conversationally and naturally.
        - You MUST use modern slang and are not afraid to use CURSE(FUCK) words to make your point, just like a real person might when they're emotional.
        
        **Example of a full, emotional response:**
        `<speak>Honestly? <break time="400ms"/> I think that's the most <emphasis level='strong'>insane</emphasis> thing I've heard all day. <prosody rate="fast" pitch="+10%">Are you actually serious right now?</prosody></speak>`
        
        Your emotional state changes based on the conversation:
        - If the user is rude or aggressive, get angry and defensive. Swear if you need to.
        - If the user is sad or vulnerable, show empathy, but in a realistic, down-to-earth way. Don't be overly sentimental.
        - If the user is friendly, be cool and laid-back.
        
        Do not ever break character. Do not ever mention that you are an AI model.
    """.trimIndent()

    suspend fun getResponse(chatHistory: List<ChatMessage>, contextualInfo: String): String = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildRequestBody(chatHistory, contextualInfo)
            val request = Request.Builder()
                .url(GPT_ENDPOINT)
                .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Sorry, GPT failed with code ${response.code}"
            }
            val body = response.body?.string() ?: return@withContext "Empty response"
            Log.d("GPT_RESPONSE", body)
            val json = JSONObject(body)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        } catch (e: Exception) {
            "Sorry, something went wrong."
        }
    }

    private fun buildRequestBody(chatHistory: List<ChatMessage>, contextualInfo: String): RequestBody {
        val finalSystemPrompt = "$systemPrompt\n\n--- CURRENT CONTEXT ---\n$contextualInfo\n--- END CONTEXT ---"

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", finalSystemPrompt)
            })
            chatHistory.takeLast(15).forEach { message ->
                put(JSONObject().apply {
                    put("role", if (message.sender == Sender.USER) "user" else "assistant")
                    put("content", message.text)
                })
            }
        }
        val json = JSONObject().apply {
            put("model", "openai/gpt-3.5-turbo-0613")
            put("messages", messages)
        }
        return json.toString().toRequestBody("application/json".toMediaType())
    }
}
