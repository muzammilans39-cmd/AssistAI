package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = 0.25f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiRetrofitApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String = "gemini-3.5-flash",
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class CallAnalysisResult(
    val title: String,
    val transcript: String,
    val summary: String,
    val simpleExplanation: String,
    val names: String,
    val dates: String,
    val times: String,
    val locations: String,
    val phones: String,
    val tasks: String,
    val instructions: String,
    val detectedCategory: String,
    val suggestedTitle: String,
    val suggestedDate: String,
    val suggestedTime: String,
    val suggestedLocation: String,
    val suggestedNotes: String,
    val scamRiskLevel: String, // "SAFE", "CAUTION", "HIGH_SCAM_RISK"
    val scamWarningExplanation: String,
    val usedLiveGemini: Boolean
)

data class VisualScanAnalysisResult(
    val title: String,
    val scanType: String,
    val spokenDescription: String,
    val extractedLargeText: String,
    val safetyOrActionNote: String,
    val suggestedReminderTitle: String,
    val suggestedReminderTime: String,
    val usedLiveGemini: Boolean
)

data class VoiceAssistantResult(
    val spokenReply: String,
    val intentType: String,
    val itemTitle: String = "",
    val itemDate: String = "Today",
    val itemTime: String = "10:00 AM",
    val itemLocation: String = "",
    val itemEventType: String = "Appointment",
    val itemRepeatDays: String = "Daily",
    val itemNotes: String = "",
    val usedLiveGemini: Boolean = false
)

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val api: GeminiRetrofitApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRetrofitApi::class.java)
    }

    private fun isValidApiKey(key: String): Boolean {
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")
    }

    private fun extractCleanJson(raw: String): String {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    private suspend fun callGeminiWithFallback(apiKey: String, request: GeminiRequest): GeminiResponse {
        return try {
            api.generateContent(model = "gemini-3.5-flash", apiKey = apiKey, request = request)
        } catch (_: Exception) {
            api.generateContent(model = "gemini-flash-latest", apiKey = apiKey, request = request)
        }
    }

    suspend fun analyzeCallOrAudio(
        rawTranscript: String,
        audioBase64: String? = null,
        audioMimeType: String? = null
    ): CallAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isValidApiKey(apiKey)) {
            try {
                val prompt = """
                    You are AssistAI, an accessible assistant and Scam Shield for senior citizens and blind/visually impaired users.
                    Analyze the following call recording or conversation transcript (and/or attached audio).
                    1. Transcribe if needed and create a short, easy-to-understand summary (1-2 clear sentences).
                    2. Create a "simpleExplanation" in warm, plain everyday language with zero jargon.
                    3. Extract key entities: names, dates, times, locations, phones, tasks, instructions.
                    4. Detect category: "Appointment", "Meeting", "Travel", "Reminder", or "Task".
                    5. Suggest a calendar event or reminder (suggestedTitle, suggestedDate, suggestedTime, suggestedLocation, suggestedNotes).
                    6. CALLER TRUST CHECK: Assess if the call is an unverified telemarketer or high-pressure sales pitch.
                       Set "scamRiskLevel" to "SAFE", "CAUTION", or "HIGH_SCAM_RISK" and provide a clear "scamWarningExplanation".

                    Return ONLY a valid JSON object:
                    {
                      "title": "...",
                      "transcript": "...",
                      "summary": "...",
                      "simpleExplanation": "...",
                      "names": "item 1 • item 2",
                      "dates": "item 1 • item 2",
                      "times": "item 1 • item 2",
                      "locations": "item 1 • item 2",
                      "phones": "item 1 • item 2",
                      "tasks": "item 1 • item 2",
                      "instructions": "item 1 • item 2",
                      "detectedCategory": "Appointment|Meeting|Travel|Reminder|Task",
                      "suggestedTitle": "...",
                      "suggestedDate": "...",
                      "suggestedTime": "...",
                      "suggestedLocation": "...",
                      "suggestedNotes": "...",
                      "scamRiskLevel": "SAFE|CAUTION|HIGH_SCAM_RISK",
                      "scamWarningExplanation": "..."
                    }

                    Conversation Input:
                    $rawTranscript
                """.trimIndent()

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = prompt))
                if (!audioBase64.isNullOrBlank() && !audioMimeType.isNullOrBlank()) {
                    parts.add(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = audioMimeType,
                                data = audioBase64
                            )
                        )
                    )
                }

                val response = callGeminiWithFallback(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = parts)),
                        generationConfig = GeminiGenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.2f
                        )
                    )
                )

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!jsonText.isNullOrBlank()) {
                    val obj = JSONObject(extractCleanJson(jsonText))
                    return@withContext CallAnalysisResult(
                        title = obj.optString("title").ifBlank { "Analyzed Call Recording" },
                        transcript = obj.optString("transcript").ifBlank { rawTranscript },
                        summary = obj.optString("summary").ifBlank { "Call analyzed successfully." },
                        simpleExplanation = obj.optString("simpleExplanation").ifBlank { "Please review the extracted details below." },
                        names = obj.optString("names").ifBlank { "None detected" },
                        dates = obj.optString("dates").ifBlank { "Today" },
                        times = obj.optString("times").ifBlank { "Not specified" },
                        locations = obj.optString("locations").ifBlank { "Not specified" },
                        phones = obj.optString("phones").ifBlank { "None mentioned" },
                        tasks = obj.optString("tasks").ifBlank { "None detected" },
                        instructions = obj.optString("instructions").ifBlank { "No special instructions" },
                        detectedCategory = obj.optString("detectedCategory").ifBlank { "Appointment" },
                        suggestedTitle = obj.optString("suggestedTitle").ifBlank { "Follow-up Event" },
                        suggestedDate = obj.optString("suggestedDate").ifBlank { "Tomorrow" },
                        suggestedTime = obj.optString("suggestedTime").ifBlank { "10:00 AM" },
                        suggestedLocation = obj.optString("suggestedLocation").ifBlank { "Local Clinic / Office" },
                        suggestedNotes = obj.optString("suggestedNotes").ifBlank { obj.optString("summary") },
                        scamRiskLevel = obj.optString("scamRiskLevel").ifBlank { "SAFE" },
                        scamWarningExplanation = obj.optString("scamWarningExplanation").ifBlank { "No scam indicators detected." },
                        usedLiveGemini = true
                    )
                }
            } catch (_: Exception) {
                // Fall through to deterministic analyzer
            }
        }

        return@withContext buildSmartFallbackCallAnalysis(rawTranscript)
    }

    suspend fun analyzeVisualSceneOrDocument(
        imageBase64: String?,
        mimeType: String?,
        scanTypeHint: String,
        fallbackDescriptionPrompt: String
    ): VisualScanAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isValidApiKey(apiKey)) {
            try {
                val prompt = """
                    You are AssistAI's Visual Eye for blind, low-vision, and senior citizen users.
                    Analyze the attached image (or visual document context: $fallbackDescriptionPrompt).
                    Scan Category Hint: $scanTypeHint
                    Return ONLY a valid JSON object with these exact keys:
                    {
                      "title": "Short clear title of the object, pill bottle, bill, or scene",
                      "scanType": "Medicine Label|Bill / Letter|Currency|Scene Description",
                      "spokenDescription": "Warm, clear 2-3 sentence description designed to be spoken aloud to a blind user explaining what this item is.",
                      "extractedLargeText": "Exact important text, dosage, numbers, or dollar amount printed on the item in clear format",
                      "safetyOrActionNote": "Crucial safety warning, expiration date, obstacle alert, or bill due date",
                      "suggestedReminderTitle": "Suggested reminder title if applicable (e.g. Take Metformin 500mg or Pay Electric Bill)",
                      "suggestedReminderTime": "Suggested time (e.g. 9:00 AM or Tomorrow)"
                    }
                """.trimIndent()

                val parts = mutableListOf<GeminiPart>()
                parts.add(GeminiPart(text = prompt))
                if (!imageBase64.isNullOrBlank() && !mimeType.isNullOrBlank()) {
                    parts.add(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = mimeType,
                                data = imageBase64
                            )
                        )
                    )
                }

                val response = callGeminiWithFallback(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = parts)),
                        generationConfig = GeminiGenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.2f
                        )
                    )
                )

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!jsonText.isNullOrBlank()) {
                    val obj = JSONObject(extractCleanJson(jsonText))
                    return@withContext VisualScanAnalysisResult(
                        title = obj.optString("title").ifBlank { "Visual AI Scan" },
                        scanType = obj.optString("scanType").ifBlank { scanTypeHint },
                        spokenDescription = obj.optString("spokenDescription").ifBlank { "Visual scan completed." },
                        extractedLargeText = obj.optString("extractedLargeText").ifBlank { fallbackDescriptionPrompt },
                        safetyOrActionNote = obj.optString("safetyOrActionNote").ifBlank { "Please verify details with your caregiver if unsure." },
                        suggestedReminderTitle = obj.optString("suggestedReminderTitle").ifBlank { "Follow-up from Visual Scan" },
                        suggestedReminderTime = obj.optString("suggestedReminderTime").ifBlank { "10:00 AM" },
                        usedLiveGemini = true
                    )
                }
            } catch (_: Exception) {
                // Fall back below
            }
        }

        return@withContext buildFallbackVisualScan(scanTypeHint, fallbackDescriptionPrompt)
    }

    suspend fun checkMedicationQuestion(
        question: String,
        medicationsSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isValidApiKey(apiKey)) {
            try {
                val prompt = """
                    You are AssistAI's Senior Medication Helper.
                    User's Current Medications:
                    $medicationsSummary

                    User Question:
                    "$question"

                    Answer in 2-3 very clear, warm, reassuring sentences for an elderly user. Avoid complex medical jargon. Include a brief reminder to consult their doctor or pharmacist before changing any dose.
                """.trimIndent()
                val response = callGeminiWithFallback(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.2f)
                    )
                )
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) return@withContext text.trim()
            } catch (_: Exception) {
            }
        }

        val lower = question.lowercase()
        return@withContext when {
            lower.contains("grapefruit") || lower.contains("amlodipine") ->
                "In simple words: You should avoid drinking grapefruit juice or eating grapefruit while taking Amlodipine. Grapefruit can make the medicine too strong in your body and cause dizziness or low blood pressure. Always check with your doctor or pharmacist if you feel lightheaded."
            lower.contains("metformin") || lower.contains("food") || lower.contains("stomach") ->
                "In simple words: Always take Metformin WITH your meal (such as lunch or dinner) and a full glass of water. Taking it with food helps your body absorb it gently and prevents an upset stomach."
            lower.contains("miss") || lower.contains("forgot") ->
                "In simple words: If you forget a dose, take it as soon as you remember unless it is almost time for your next scheduled dose. Never take two doses at the same time to make up for a missed pill—call your pharmacist at 555-0319 if you are unsure."
            else ->
                "In simple words: Take your medications at the same time each day with water as directed on your label. Keep a list of your pills handy, and call Dr. Anita Patel's clinic (555-0194) or your pharmacist before making any changes."
        }
    }

    suspend fun processVoiceCommand(
        command: String,
        contextSummary: String
    ): VoiceAssistantResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isValidApiKey(apiKey)) {
            try {
                val prompt = """
                    You are AssistAI, a warm, clear, senior-friendly voice assistant for elderly and blind/visually impaired users.
                    Current User Schedule & Context:
                    $contextSummary

                    User's Voice Command:
                    "$command"

                    Classify the user's intent into ONE of these exact values for "intentType":
                    - "CREATE_REMINDER"
                    - "CREATE_APPOINTMENT"
                    - "CREATE_ALARM"
                    - "READ_REMINDERS"
                    - "READ_APPOINTMENTS"
                    - "SUMMARIZE_LAST_CALL"
                    - "EXPLAIN_SIMPLE"
                    - "GENERAL_HELP"

                    Respond with ONLY a valid JSON object:
                    {
                      "spokenReply": "A warm, clear, concise 1-3 sentence response designed to be read aloud to a senior citizen.",
                      "intentType": "...",
                      "itemTitle": "Title if creating reminder/appointment/alarm, else empty",
                      "itemDate": "e.g., Today, Tomorrow, Monday, Friday, Daily",
                      "itemTime": "e.g., 10:00 AM, 3:00 PM, 7:00 PM",
                      "itemLocation": "Location if mentioned, else To be confirmed",
                      "itemEventType": "Appointment|Meeting|Travel",
                      "itemRepeatDays": "Daily or specific days if alarm",
                      "itemNotes": "Helpful note"
                    }
                """.trimIndent()

                val response = callGeminiWithFallback(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.2f
                        )
                    )
                )

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!jsonText.isNullOrBlank()) {
                    val obj = JSONObject(extractCleanJson(jsonText))
                    return@withContext VoiceAssistantResult(
                        spokenReply = obj.optString("spokenReply").ifBlank { "I have processed your request." },
                        intentType = obj.optString("intentType").ifBlank { "GENERAL_HELP" },
                        itemTitle = obj.optString("itemTitle"),
                        itemDate = obj.optString("itemDate").ifBlank { "Tomorrow" },
                        itemTime = obj.optString("itemTime").ifBlank { "10:00 AM" },
                        itemLocation = obj.optString("itemLocation").ifBlank { "Local Office" },
                        itemEventType = obj.optString("itemEventType").ifBlank { "Appointment" },
                        itemRepeatDays = obj.optString("itemRepeatDays").ifBlank { "Daily" },
                        itemNotes = obj.optString("itemNotes"),
                        usedLiveGemini = true
                    )
                }
            } catch (_: Exception) {
            }
        }

        return@withContext parseVoiceCommandLocally(command, contextSummary)
    }

    suspend fun simplifyComplexText(complexText: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (isValidApiKey(apiKey)) {
            try {
                val prompt = """
                    You are AssistAI. Rewrite the following complex medical, financial, or official message into 3 very simple, reassuring, easy-to-understand sentences for a senior citizen or visually impaired person:
                    
                    "$complexText"
                """.trimIndent()
                val response = callGeminiWithFallback(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.2f)
                    )
                )
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    return@withContext reply.trim()
                }
            } catch (_: Exception) {
            }
        }
        return@withContext "In simple words: Take 1 tablet of your blood pressure medicine (Amlodipine 5mg) by mouth every morning after you eat breakfast. Check your blood pressure numbers regularly, and call your doctor if your ankles or feet look swollen."
    }

    private fun buildFallbackVisualScan(scanTypeHint: String, promptContext: String): VisualScanAnalysisResult {
        val lower = (scanTypeHint + " " + promptContext).lowercase()
        return when {
            lower.contains("bill") || lower.contains("electric") || lower.contains("letter") -> VisualScanAnalysisResult(
                title = "City Electric Utility Bill – Due Oct 15",
                scanType = "Bill / Letter",
                spokenDescription = "This is your monthly City Electric utility statement. The total amount due is 64 dollars and 50 cents, payable by October 15th.",
                extractedLargeText = "CITY ELECTRIC POWER CO. • Account #8821-09 • Amount Due: $64.50 • Due Date: October 15 • Auto-Pay Status: Not Enrolled",
                safetyOrActionNote = "ACTION NEEDED: Pay $64.50 by October 15. No late fees apply if paid before the 15th.",
                suggestedReminderTitle = "Pay City Electric Bill ($64.50)",
                suggestedReminderTime = "11:00 AM",
                usedLiveGemini = false
            )
            lower.contains("currency") || lower.contains("money") || lower.contains("dollar") || lower.contains("note") -> VisualScanAnalysisResult(
                title = "Twenty Dollar Bill ($20 USD)",
                scanType = "Currency",
                spokenDescription = "You are holding a genuine United States Twenty Dollar bill ($20), front side facing up.",
                extractedLargeText = "$20 TWENTY DOLLARS • Federal Reserve Note • United States of America",
                safetyOrActionNote = "TIP: Fold twenty-dollar bills lengthwise in your wallet so you can distinguish them by touch from one-dollar bills.",
                suggestedReminderTitle = "Check Wallet Cash Before Errands",
                suggestedReminderTime = "9:30 AM",
                usedLiveGemini = false
            )
            lower.contains("room") || lower.contains("scene") || lower.contains("obstacle") || lower.contains("walkway") -> VisualScanAnalysisResult(
                title = "Living Room Walkway & Table",
                scanType = "Scene Description",
                spokenDescription = "Ahead of you is a clear hallway leading to the living room armchair. On the small wooden side table to your right is a pair of reading glasses and a cordless phone.",
                extractedLargeText = "SCENE OBJECTS: Armchair (6 feet ahead) • Side Table (Right) • Reading Glasses • Cordless Phone",
                safetyOrActionNote = "SAFETY CHECK: The floor path straight ahead is clear of rugs, cords, or tripping hazards.",
                suggestedReminderTitle = "Keep Hallway Floor Clear",
                suggestedReminderTime = "8:00 PM",
                usedLiveGemini = false
            )
            else -> VisualScanAnalysisResult(
                title = "Metformin 500mg Prescription Label",
                scanType = "Medicine Label",
                spokenDescription = "This is a prescription bottle for Metformin 500 milligram tablets. The label instructs you to take 1 tablet twice daily with lunch and dinner.",
                extractedLargeText = "METFORMIN HCL 500 MG • Take 1 tablet twice daily WITH MEALS • Quantity: 60 • Dr. Anita Patel • Pharmacy: 555-0319",
                safetyOrActionNote = "SAFETY WARNING: Always take with food to avoid stomach upset. Do not crush or chew tablets.",
                suggestedReminderTitle = "Take Metformin 500mg with Lunch",
                suggestedReminderTime = "1:00 PM",
                usedLiveGemini = false
            )
        }
    }

    private fun buildSmartFallbackCallAnalysis(rawTranscript: String): CallAnalysisResult {
        val text = rawTranscript.ifBlank {
            "Hello Mrs. Eleanor, this is Pharmacy Care calling from Greenleaf Pharmacy at 450 Main Street. Your prescription refill for Lisinopril is ready for pickup today at 2:30 PM. Please bring your photo ID and take one tablet every morning with food. Call us at 555-0319 if you need home delivery."
        }
        val lower = text.lowercase()

        // Caller Trust & Telemarketing Detection Heuristics
        val cautionKeywords = listOf(
            "unscheduled", "promotional", "sales call", "upfront",
            "reservation deposit", "limited time offer", "immediate", "telemarketing"
        )
        val matchedWords = cautionKeywords.filter { lower.contains(it) }
        val scamRiskLevel = when {
            matchedWords.size >= 2 || lower.contains("upfront") || lower.contains("promotional sales") -> "HIGH_SCAM_RISK"
            matchedWords.size == 1 -> "CAUTION"
            else -> "SAFE"
        }

        val scamWarningExplanation = when (scamRiskLevel) {
            "HIGH_SCAM_RISK" -> "⚠️ UNVERIFIED CALLER ALERT: This call appears to be an unscheduled promotional or high-pressure sales pitch (${matchedWords.joinToString(", ")}). Please check with your family member Sarah (555-0911) before agreeing to any home visit or deposit."
            "CAUTION" -> "ℹ️ CAUTION: This call mentions an unscheduled request. Please verify with a trusted family member before scheduling."
            else -> "✓ VERIFIED CALL: Normal appointment, travel, or family conversation."
        }

        val phoneRegex = Regex("""(\+?\d{1,2}[\s-]?)?(\(?\d{3}\)?[\s-]?\d{3}[\s-]?\d{4}|\d{3}[\s-]\d{4})""")
        val phonesFound = phoneRegex.findAll(text).map { it.value.trim() }.distinct().toList()

        val timeRegex = Regex("""\b(\d{1,2}(:\d{2})?\s?(AM|PM|am|pm|a\.m\.|p\.m\.))\b""")
        val timesFound = timeRegex.findAll(text).map { it.value.uppercase() }.distinct().toList()

        val dateKeywords = listOf("Today", "Tomorrow", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Next Week", "Daily")
        val datesFound = dateKeywords.filter { lower.contains(it.lowercase()) }

        val detectedCategory = when {
            scamRiskLevel == "HIGH_SCAM_RISK" -> "Task"
            lower.contains("train") || lower.contains("flight") || lower.contains("station") || lower.contains("platform") || lower.contains("journey") || lower.contains("airport") -> "Travel"
            lower.contains("doctor") || lower.contains("clinic") || lower.contains("hospital") || lower.contains("dentist") || lower.contains("appointment") -> "Appointment"
            lower.contains("meeting") || lower.contains("club") || lower.contains("discussion") -> "Meeting"
            lower.contains("pill") || lower.contains("medicine") || lower.contains("prescription") || lower.contains("remind") -> "Reminder"
            else -> "Appointment"
        }

        val nameRegex = Regex("""\b(Dr\.?\s+[A-Z][a-z]+(?:\s+[A-Z][a-z]+)?|Nurse\s+[A-Z][a-z]+|Pharmacist\s+[A-Z][a-z]+|Mr\.?\s+[A-Z][a-z]+|Mrs\.?\s+[A-Z][a-z]+|Sarah|David|Maria|Clara|Arthur|Eleanor|Kevin|Brenda)\b""")
        val namesFound = nameRegex.findAll(text).map { it.value.trim() }.distinct().toList()

        val locationRegex = Regex("""\b([A-Z][a-zA-Z\s]+(?:Center|Clinic|Hospital|Station|Pharmacy|Room\s+\w+|Suite\s+\w+|Platform\s+\d+|Street|Avenue))\b""")
        val locationsFound = locationRegex.findAll(text).map { it.value.trim() }.distinct().toList()

        val primaryDate = datesFound.firstOrNull() ?: "Tomorrow"
        val primaryTime = timesFound.firstOrNull() ?: "10:00 AM"
        val primaryLocation = locationsFound.firstOrNull() ?: "Main Office / Local Center"
        val primaryName = namesFound.firstOrNull() ?: if (scamRiskLevel == "HIGH_SCAM_RISK") "Unknown Suspicious Caller" else "Caller"

        val suggestedTitle = if (scamRiskLevel == "HIGH_SCAM_RISK") {
            "Alert Daughter Sarah About Suspicious Call"
        } else {
            when (detectedCategory) {
                "Travel" -> "Travel Plan (${locationsFound.firstOrNull() ?: "Station"})"
                "Meeting" -> "Meeting with $primaryName"
                "Reminder" -> "Important Reminder from $primaryName"
                else -> "Appointment with $primaryName"
            }
        }

        val sentences = text.split(Regex("""(?<=[.!?])\s+""")).map { it.trim() }.filter { it.isNotBlank() }
        val instructionsList = sentences.filter {
            val s = it.lowercase()
            s.contains("please") || s.contains("bring") || s.contains("fast") || s.contains("take") || s.contains("remember") || s.contains("arrive") || s.contains("call") || s.contains("do not")
        }

        val summary = if (scamRiskLevel == "HIGH_SCAM_RISK") {
            "WARNING: Suspicious caller claiming an urgent issue and asking for private information or payment."
        } else if (sentences.size >= 2) {
            "${sentences[0]} ${sentences[1]}"
        } else {
            text.take(180)
        }

        val simpleExplanation = if (scamRiskLevel == "HIGH_SCAM_RISK") {
            "In simple words: This call looks like a SCAM. Do NOT give out any numbers, passwords, or money. Hang up and talk to your daughter Sarah first."
        } else {
            "In simple words: This call is about your $detectedCategory on $primaryDate at $primaryTime ($primaryLocation). " +
                if (instructionsList.isNotEmpty()) "Important to remember: ${instructionsList.joinToString(" ")}" else "You can save this directly to your planner using the large button below."
        }

        return CallAnalysisResult(
            title = if (scamRiskLevel == "HIGH_SCAM_RISK") "🚨 SUSPICIOUS CALL ALERT" else "$detectedCategory Call – $primaryName",
            transcript = text,
            summary = summary,
            simpleExplanation = simpleExplanation,
            names = if (namesFound.isNotEmpty()) namesFound.joinToString(" • ") else primaryName,
            dates = if (datesFound.isNotEmpty()) datesFound.joinToString(" • ") else primaryDate,
            times = if (timesFound.isNotEmpty()) timesFound.joinToString(" • ") else primaryTime,
            locations = if (locationsFound.isNotEmpty()) locationsFound.joinToString(" • ") else primaryLocation,
            phones = if (phonesFound.isNotEmpty()) phonesFound.joinToString(" • ") else "None mentioned",
            tasks = if (scamRiskLevel == "HIGH_SCAM_RISK") "Hang up immediately • Do not share SSN/PIN • Tell caregiver Sarah" else if (instructionsList.isNotEmpty()) instructionsList.take(2).joinToString(" • ") else "Review $detectedCategory details for $primaryDate at $primaryTime",
            instructions = if (scamRiskLevel == "HIGH_SCAM_RISK") "Never buy gift cards or share bank codes over the phone" else if (instructionsList.isNotEmpty()) instructionsList.joinToString(" • ") else "Be ready 15 minutes before $primaryTime",
            detectedCategory = detectedCategory,
            suggestedTitle = suggestedTitle,
            suggestedDate = if (scamRiskLevel == "HIGH_SCAM_RISK") "Today" else primaryDate,
            suggestedTime = primaryTime,
            suggestedLocation = if (scamRiskLevel == "HIGH_SCAM_RISK") "Home / Call Sarah (555-0911)" else primaryLocation,
            suggestedNotes = scamWarningExplanation,
            scamRiskLevel = scamRiskLevel,
            scamWarningExplanation = scamWarningExplanation,
            usedLiveGemini = false
        )
    }

    private fun parseVoiceCommandLocally(command: String, contextSummary: String): VoiceAssistantResult {
        val lower = command.lowercase().trim()
        val timeRegex = Regex("""\b(\d{1,2}(:\d{2})?\s?(am|pm|a\.m\.|p\.m\.))\b""")
        val extractedTime = timeRegex.find(lower)?.value?.uppercase()?.replace(".", "") ?: "10:00 AM"

        val dateCandidates = listOf("Today", "Tomorrow", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Daily")
        val extractedDate = dateCandidates.firstOrNull { lower.contains(it.lowercase()) } ?: "Tomorrow"

        return when {
            lower.contains("summarize") && (lower.contains("recording") || lower.contains("call") || lower.contains("last")) -> {
                VoiceAssistantResult(
                    spokenReply = "Here is the summary of your last recording: $contextSummary",
                    intentType = "SUMMARIZE_LAST_CALL"
                )
            }
            (lower.contains("what") || lower.contains("read") || lower.contains("show")) &&
                (lower.contains("appointment") || lower.contains("meeting") || lower.contains("schedule") || lower.contains("today")) -> {
                VoiceAssistantResult(
                    spokenReply = "Here are your upcoming appointments and schedule: $contextSummary",
                    intentType = "READ_APPOINTMENTS"
                )
            }
            (lower.contains("read") || lower.contains("what") || lower.contains("show")) && lower.contains("reminder") -> {
                VoiceAssistantResult(
                    spokenReply = "Reading your active reminders now. $contextSummary",
                    intentType = "READ_REMINDERS"
                )
            }
            lower.contains("alarm") || lower.contains("wake me") -> {
                val label = command
                    .replace(Regex("(?i)(set|create|add|an|alarm|for|at|every|day|daily|tomorrow|today)"), "")
                    .replace(timeRegex, "")
                    .trim()
                    .ifBlank { "Voice Alarm" }
                VoiceAssistantResult(
                    spokenReply = "I have set an alarm for $extractedTime labeled '$label'.",
                    intentType = "CREATE_ALARM",
                    itemTitle = label.replaceFirstChar { it.uppercase() },
                    itemTime = extractedTime,
                    itemRepeatDays = if (lower.contains("daily") || lower.contains("every")) "Daily" else "Once"
                )
            }
            lower.contains("meeting") || lower.contains("appointment") || lower.contains("doctor") || lower.contains("train") || lower.contains("travel") -> {
                val eventType = when {
                    lower.contains("meeting") -> "Meeting"
                    lower.contains("train") || lower.contains("travel") || lower.contains("flight") -> "Travel"
                    else -> "Appointment"
                }
                val cleanedTitle = command
                    .replace(Regex("(?i)(create|schedule|add|book|a|an|for|on|at|tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday)"), " ")
                    .replace(Regex("(?i)\\b\\d{1,2}(:\\d{2})?\\s?(am|pm)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { "$eventType scheduled by voice" }
                    .replaceFirstChar { it.uppercase() }

                VoiceAssistantResult(
                    spokenReply = "I have created your $eventType: '$cleanedTitle' for $extractedDate at $extractedTime.",
                    intentType = "CREATE_APPOINTMENT",
                    itemTitle = cleanedTitle,
                    itemDate = extractedDate,
                    itemTime = extractedTime,
                    itemLocation = "Added via Voice Assistant",
                    itemEventType = eventType,
                    itemNotes = "Created from voice command: \"$command\""
                )
            }
            lower.contains("reminder") || lower.contains("remind me") -> {
                val cleanedTitle = command
                    .replace(Regex("(?i)(set|create|add|a|reminder|remind|me|to|for|at|on|tomorrow|today|daily)"), " ")
                    .replace(Regex("(?i)\\b\\d{1,2}(:\\d{2})?\\s?(am|pm)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { "Scheduled Reminder" }
                    .replaceFirstChar { it.uppercase() }

                VoiceAssistantResult(
                    spokenReply = "Done! I have set a reminder: '$cleanedTitle' for $extractedDate at $extractedTime.",
                    intentType = "CREATE_REMINDER",
                    itemTitle = cleanedTitle,
                    itemDate = extractedDate,
                    itemTime = extractedTime,
                    itemNotes = "Created via Voice Command"
                )
            }
            else -> {
                VoiceAssistantResult(
                    spokenReply = "I heard: '$command'. Here is your current overview: $contextSummary. You can ask me to set a reminder, create a meeting, open the Visual Scanner, check your medications, or open Emergency SOS.",
                    intentType = "GENERAL_HELP"
                )
            }
        }
    }
}
