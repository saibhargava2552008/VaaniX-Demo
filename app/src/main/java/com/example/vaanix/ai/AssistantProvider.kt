package com.example.vaanix.ai

import com.example.vaanix.core.VaaniConfig
import com.example.vaanix.language.LanguageCatalog
import com.example.vaanix.language.LanguageDetector
import com.example.vaanix.language.LocalizedStringsProvider
import com.example.vaanix.language.SupportedLanguage
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.phoneactions.PhoneGuideTopic
import com.example.vaanix.phoneactions.SettingsTarget
import com.example.vaanix.phoneactions.VolumeDirection

sealed class AssistantIntent {
    data class OpenApp(val appName: String) : AssistantIntent()
    data class SearchYouTube(val query: String) : AssistantIntent()
    data class AdjustVolume(val direction: VolumeDirection) : AssistantIntent()
    data class OpenSettings(val target: SettingsTarget) : AssistantIntent()
    data class PrepareWhatsAppMessage(val recipient: String, val draft: String) : AssistantIntent()
    data class PreparePhoneCall(val target: String) : AssistantIntent()
    data class SetAlarm(val hour24: Int, val minute: Int, val label: String) : AssistantIntent()
    data object ScreenHelp : AssistantIntent()
    data object CheckMessagesHelp : AssistantIntent()
    data class TeachPhone(val topic: PhoneGuideTopic) : AssistantIntent()
    data object Greeting : AssistantIntent()
    data object Unknown : AssistantIntent()
}

data class ConversationContext(
    val preferredLanguage: SupportedLanguage = LanguageCatalog.ENGLISH_INDIA,
    val lastOpenedAppKey: String? = null,
    val recentUserUtterances: List<String> = emptyList()
)

data class AssistantReply(
    val intent: AssistantIntent,
    val responseText: String,
    val responseLanguage: SupportedLanguage,
    val detectedInputLanguage: SupportedLanguage,
    val isMixedLanguageInput: Boolean,
    val proposedAction: PhoneAction? = null,
    val updatedLastAppKey: String? = null,
    val providerName: String,
    val isMockProvider: Boolean
)

/**
 * Replaceable AI Provider interface.
 * Future real backend/proxy LLM providers implement this exact contract
 * without modifying UI, audio coordination, or phone action execution.
 */
interface AssistantProvider {
    val providerId: String
    val isMock: Boolean
    suspend fun processRequest(
        userText: String,
        context: ConversationContext
    ): AssistantReply
}

/**
 * Clearly identified zero-cost deterministic multilingual provider (`LLM_PROVIDER=mock`).
 * Supports natural everyday requests across all 12 Indian languages + English,
 * mixed-language phrasing (Hinglish, Tenglish, etc.), and follow-up context
 * (e.g., "Open YouTube" followed by "Search devotional songs").
 * Never pretends to be a live cloud LLM.
 */
class MockMultilingualAssistantProvider : AssistantProvider {

    override val providerId: String = "mock-multilingual-zero-cost-v1"
    override val isMock: Boolean = true

    override suspend fun processRequest(
        userText: String,
        context: ConversationContext
    ): AssistantReply {
        val trimmed = userText.trim()
        val detection = LanguageDetector.detect(trimmed, context.preferredLanguage)
        val replyLang = context.preferredLanguage
        val normalized = trimmed.lowercase()

        // 1. Follow-up context: if user just opened YouTube and now says "search ..."
        if (context.lastOpenedAppKey == "youtube" && isSearchRequest(normalized) && !mentionsApp(normalized, "whatsapp")) {
            val query = extractSearchQuery(trimmed)
            val action = PhoneAction.SearchYouTube(query)
            return AssistantReply(
                intent = AssistantIntent.SearchYouTube(query),
                responseText = formatReply(
                    replyLang,
                    en = "Sure! Since we just opened YouTube, I will search for \"$query\" now.",
                    hi = "जी बिल्कुल! YouTube पर \"$query\" खोज रहा हूँ।",
                    te = "తప్పకుండా! YouTube లో \"$query\" కోసం వెతుకుతున్నాను.",
                    ta = "நிச்சயமாக! YouTube இல் \"$query\" தேடுகிறேன்.",
                    kn = "ಖಂಡಿತ! YouTube ನಲ್ಲಿ \"$query\" ಹುಡುಕುತ್ತಿದ್ದೇನೆ.",
                    ml = "തീർച്ചയായും! YouTube ൽ \"$query\" തിരയുന്നു.",
                    mr = "नक्कीच! YouTube वर \"$query\" शोधत आहे.",
                    bn = "নিশ্চয়ই! YouTube-এ \"$query\" খুঁজছি।",
                    gu = "ચોક્કસ! YouTube પર \"$query\" શોધી રહ્યો છું.",
                    pa = "ਜ਼ਰੂਰ! YouTube 'ਤੇ \"$query\" ਲੱਭ ਰਿਹਾ ਹਾਂ।",
                    or = "ନିଶ୍ଚୟ! YouTube ରେ \"$query\" ଖୋଜୁଛି।",
                    asText = "নিশ্চয়! YouTube ত \"$query\" বিচাৰি আছোঁ।",
                    ur = "جی ضرور! YouTube پر \"$query\" تلاش کر رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "youtube",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 2. YouTube search combined in one sentence (e.g., "YouTube open karke songs search karo")
        if (mentionsYouTube(normalized) && (isSearchRequest(normalized) || containsSongOrVideoHint(normalized))) {
            val query = extractSearchQuery(trimmed)
            val action = PhoneAction.SearchYouTube(query)
            return AssistantReply(
                intent = AssistantIntent.SearchYouTube(query),
                responseText = formatReply(
                    replyLang,
                    en = "Opening YouTube to search for \"$query\".",
                    hi = "YouTube खोलकर \"$query\" खोज रहा हूँ।",
                    te = "YouTube తెరిచి \"$query\" కోసం వెతుకుతున్నాను.",
                    ta = "YouTube திறந்து \"$query\" தேடுகிறேன்.",
                    kn = "YouTube ತೆರೆದು \"$query\" ಹುಡುಕುತ್ತಿದ್ದೇನೆ.",
                    ml = "YouTube തുറന്ന് \"$query\" തിരയുന്നു.",
                    mr = "YouTube उघडून \"$query\" शोधत आहे.",
                    bn = "YouTube খুলে \"$query\" খুঁজছি।",
                    gu = "YouTube ખોલીને \"$query\" શોધું છું.",
                    pa = "YouTube ਖੋਲ੍ਹ ਕੇ \"$query\" ਲੱਭ ਰਿਹਾ ਹਾਂ।",
                    or = "YouTube ଖୋଲି \"$query\" ଖୋଜୁଛି।",
                    asText = "YouTube খুলি \"$query\" বিচাৰি আছোঁ।",
                    ur = "YouTube کھول کر \"$query\" تلاش کر رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "youtube",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 3. Prepare WhatsApp message (SENSITIVE -> requires confirmation!)
        if (isSendMessageIntent(normalized)) {
            val recipient = extractRecipientHint(trimmed)
            val draft = buildDraftMessage(replyLang)
            val action = PhoneAction.PrepareWhatsAppMessage(recipient, draft)
            return AssistantReply(
                intent = AssistantIntent.PrepareWhatsAppMessage(recipient, draft),
                responseText = formatReply(
                    replyLang,
                    en = "I can prepare a WhatsApp message for $recipient. For your safety, please confirm below before I open WhatsApp.",
                    hi = "मैं $recipient के लिए WhatsApp संदेश तैयार कर सकता हूँ। आपकी सुरक्षा के लिए कृपया नीचे 'Yes, Continue' दबाकर पक्का करें।",
                    te = "$recipient కోసం WhatsApp మెసేజ్ సిద్ధం చేయగలను. మీ భద్రత కోసం కింద నిర్ధారించండి.",
                    ta = "$recipient க்கு WhatsApp செய்தி தயார் செய்யலாம். பாதுகாப்புக்காக கீழே உறுதிப்படுத்தவும்.",
                    kn = "$recipient ಅವರಿಗೆ WhatsApp ಸಂದೇಶ ಸಿದ್ಧಪಡಿಸಬಹುದು. ದಯವಿಟ್ಟು ಕೆಳಗೆ ದೃಢಪಡಿಸಿ.",
                    ml = "$recipient നായി WhatsApp സന്ദേശം തയ്യാറാക്കാം. സുരക്ഷയ്ക്കായി താഴെ ഉറപ്പാക്കൂ.",
                    mr = "$recipient साठी WhatsApp मेसेज तयार करू शकतो. कृपया खाली खात्री करा.",
                    bn = "$recipient-এর জন্য WhatsApp মেসেজ তৈরি করতে পারি। অনুগ্রহ করে নিচে নিশ্চিত করুন।",
                    gu = "$recipient માટે WhatsApp મેસેજ તૈયાર કરી શકું છું. કૃપા કરીને નીચે ખાતરી કરો.",
                    pa = "$recipient ਲਈ WhatsApp ਸੁਨੇਹਾ ਤਿਆਰ ਕਰ ਸਕਦਾ ਹਾਂ। ਕਿਰਪਾ ਕਰਕੇ ਹੇਠਾਂ ਪੱਕਾ ਕਰੋ।",
                    or = "$recipient ଙ୍କ ପାଇଁ WhatsApp ମେସେଜ୍ ପ୍ରସ୍ତୁତ କରିପାରିବି। ଦୟାକରି ତଳେ ନିଶ୍ଚିତ କରନ୍ତୁ।",
                    asText = "$recipient ৰ বাবে WhatsApp বাৰ্তা প্ৰস্তুত কৰিব পাৰোঁ। অনুগ্ৰহ কৰি তলত নিশ্চিত কৰক।",
                    ur = "میں $recipient کے لیے WhatsApp پیغام تیار کر سکتا ہوں۔ براہ کرم نیچے تصدیق کریں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "whatsapp",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 4. Make a phone call (SENSITIVE -> requires confirmation!)
        if (isPhoneCallIntent(normalized)) {
            val target = extractCallTarget(trimmed)
            val action = PhoneAction.PreparePhoneCall(target)
            return AssistantReply(
                intent = AssistantIntent.PreparePhoneCall(target),
                responseText = formatReply(
                    replyLang,
                    en = "I can open the Phone Dialer for $target. I will never place a call automatically without your confirmation. Tap Yes below to open the dialer.",
                    hi = "मैं $target के लिए फ़ोन डायलर खोल सकता हूँ। आपकी अनुमति के बिना कॉल नहीं लगेगी। कृपया नीचे पक्का करें।",
                    te = "$target కోసం ఫోన్ డయలర్ తెరవగలను. మీ అనుమతి లేకుండా కాల్ వెళ్ళదు. దయచేసి కింద నిర్ధారించండి.",
                    ta = "$target க்கு போன் டயலர் திறக்கலாம். உங்கள் அனுமதி இல்லாமல் அழைப்பு போகாது. கீழே உறுதிப்படுத்தவும்.",
                    kn = "$target ಗಾಗಿ ಫೋನ್ ಡಯಲರ್ ತೆರೆಯಬಹುದು. ದಯವಿಟ್ಟು ಕೆಳಗೆ ದೃಢಪಡಿಸಿ.",
                    ml = "$target നായി ഫോൺ ഡയലർ തുറക്കാം. ദയവായി താഴെ ഉറപ്പാക്കുക.",
                    mr = "$target साठी फोन डायलर उघडू शकतो. कृपया खाली खात्री करा.",
                    bn = "$target-এর জন্য ফোন ডায়ালার খুলতে পারি। অনুগ্রহ করে নিচে নিশ্চিত করুন।",
                    gu = "$target માટે ફોન ડાયલર ખોલી શકું છું. કૃપા કરીને નીચે ખાતરી કરો.",
                    pa = "$target ਲਈ ਫ਼ੋਨ ਡਾਇਲਰ ਖੋਲ੍ਹ ਸਕਦਾ ਹਾਂ। ਕਿਰਪਾ ਕਰਕੇ ਹੇਠਾਂ ਪੱਕਾ ਕਰੋ।",
                    or = "$target ଙ୍କ ପାଇଁ ଫୋନ୍ ଡାଏଲର୍ ଖୋଲିପାରିବି। ତଳେ ନିଶ୍ଚିତ କରନ୍ତୁ।",
                    asText = "$target ৰ বাবে ফোন ডায়েলাৰ খুলিব পাৰোঁ। তলত নিশ্চিত কৰক।",
                    ur = "میں $target کے لیے فون ڈائلر کھول سکتا ہوں۔ نیچے تصدیق کریں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "phone",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 5. Open WhatsApp
        if (mentionsApp(normalized, "whatsapp") || normalized.contains("వాట్సాప్") || normalized.contains("व्हाट्सएप") || normalized.contains("வாட்ஸ்அப்")) {
            val action = PhoneAction.OpenApp(
                appKey = "whatsapp",
                packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                label = "WhatsApp"
            )
            return AssistantReply(
                intent = AssistantIntent.OpenApp("WhatsApp"),
                responseText = formatReply(
                    replyLang,
                    en = "Trying to open WhatsApp on your phone now.",
                    hi = "आपके फ़ोन पर WhatsApp खोलने की कोशिश कर रहा हूँ।",
                    te = "మీ ఫోన్‌లో WhatsApp తెరవడానికి ప్రయత్నిస్తున్నాను.",
                    ta = "உங்கள் போனில் WhatsApp திறக்க முயற்சிக்கிறேன்.",
                    kn = "ನಿಮ್ಮ ಫೋನ್‌ನಲ್ಲಿ WhatsApp ತೆರೆಯಲು ಪ್ರಯತ್ನಿಸುತ್ತಿದ್ದೇನೆ.",
                    ml = "നിങ്ങളുടെ ഫോണിൽ WhatsApp തുറക്കാൻ ശ്രമിക്കുന്നു.",
                    mr = "तुमच्या फोनवर WhatsApp उघडण्याचा प्रयत्न करत आहे.",
                    bn = "আপনার ফোনে WhatsApp খোলার চেষ্টা করছি।",
                    gu = "તમારા ફોનમાં WhatsApp ખોલવાનો પ્રયાસ કરું છું.",
                    pa = "ਤੁਹਾਡੇ ਫ਼ੋਨ 'ਤੇ WhatsApp ਖੋਲ੍ਹਣ ਦੀ ਕੋਸ਼ਿਸ਼ ਕਰ ਰਿਹਾ ਹਾਂ।",
                    or = "ଆପଣଙ୍କ ଫୋନରେ WhatsApp ଖୋଲିବାକୁ ଚେଷ୍ଟା କରୁଛି।",
                    asText = "আপোনাৰ ফোনত WhatsApp খুলিবলৈ চেষ্টা কৰিছোঁ।",
                    ur = "آپ کے فون پر WhatsApp کھولنے کی کوشش کر رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "whatsapp",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 6. Open YouTube
        if (mentionsYouTube(normalized)) {
            val action = PhoneAction.OpenApp(
                appKey = "youtube",
                packageNames = listOf("com.google.android.youtube"),
                label = "YouTube"
            )
            return AssistantReply(
                intent = AssistantIntent.OpenApp("YouTube"),
                responseText = formatReply(
                    replyLang,
                    en = "Opening YouTube for you. Next, you can simply say \"Search devotional songs\" and I will help search.",
                    hi = "आपके लिए YouTube खोल रहा हूँ। इसके बाद आप कह सकते हैं 'भजन खोजो' और मैं मदद करूँगा।",
                    te = "మీ కోసం YouTube తెరుస్తున్నాను. ఆ తర్వాత 'భక్తి పాటలు వెతుకు' అని చెబితే నేను సహాయం చేస్తాను.",
                    ta = "உங்களுக்காக YouTube திறக்கிறேன். அடுத்து நீங்கள் பாடல்களை தேடச் சொல்லலாம்.",
                    kn = "ನಿಮಗಾಗಿ YouTube ತೆರೆಯುತ್ತಿದ್ದೇನೆ. ನಂತರ ಹಾಡುಗಳನ್ನು ಹುಡುಕಲು ಹೇಳಬಹುದು.",
                    ml = "നിങ്ങൾക്കായി YouTube തുറക്കുന്നു. തുടർന്ന് പാട്ടുകൾ തിരയാൻ പറയാം.",
                    mr = "तुमच्यासाठी YouTube उघडत आहे. त्यानंतर तुम्ही गाणी शोधायला सांगू शकता.",
                    bn = "আপনার জন্য YouTube খুলছি। এরপর গান খুঁজতে বলতে পারেন।",
                    gu = "તમારા માટે YouTube ખોલું છું. પછી તમે ભજન શોધવા કહી શકો છો.",
                    pa = "ਤੁਹਾਡੇ ਲਈ YouTube ਖੋਲ੍ਹ ਰਿਹਾ ਹਾਂ।",
                    or = "ଆପଣଙ୍କ ପାଇଁ YouTube ଖୋଲୁଛି।",
                    asText = "আপোনাৰ বাবে YouTube খুলি আছোঁ।",
                    ur = "آپ کے لیے YouTube کھول رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                updatedLastAppKey = "youtube",
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 7. Volume Up / Down
        if (isVolumeUpIntent(normalized)) {
            val action = PhoneAction.AdjustVolume(VolumeDirection.UP)
            return AssistantReply(
                intent = AssistantIntent.AdjustVolume(VolumeDirection.UP),
                responseText = formatReply(
                    replyLang,
                    en = "Increasing your phone's sound volume now.",
                    hi = "फ़ोन की आवाज़ बढ़ा रहा हूँ।",
                    te = "మీ ఫోన్ సౌండ్ వాల్యూమ్ పెంచుతున్నాను.",
                    ta = "போனின் ஒலியை அதிகரிக்கிறேன்.",
                    kn = "ಫೋನ್ ಶಬ್ದವನ್ನು ಹೆಚ್ಚಿಸುತ್ತಿದ್ದೇನೆ.",
                    ml = "ഫോണിന്റെ ശബ്ദം കൂട്ടുന്നു.",
                    mr = "फोनचा आवाज वाढवत आहे.",
                    bn = "ফোনের আওয়াজ বাড়াচ্ছি।",
                    gu = "ફોનનો અવાજ વધારું છું.",
                    pa = "ਫ਼ੋਨ ਦੀ ਆਵਾਜ਼ ਵਧਾ ਰਿਹਾ ਹਾਂ।",
                    or = "ଫୋନର ଶବ୍ଦ ବଢ଼ାଉଛି।",
                    asText = "ফোনৰ শব্দ বঢ়াই আছোঁ।",
                    ur = "فون کی آواز بڑھا رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        if (isVolumeDownIntent(normalized)) {
            val action = PhoneAction.AdjustVolume(VolumeDirection.DOWN)
            return AssistantReply(
                intent = AssistantIntent.AdjustVolume(VolumeDirection.DOWN),
                responseText = formatReply(
                    replyLang,
                    en = "Lowering your phone's sound volume now.",
                    hi = "फ़ोन की आवाज़ कम कर रहा हूँ।",
                    te = "మీ ఫోన్ సౌండ్ వాల్యూమ్ తగ్గిస్తున్నాను.",
                    ta = "போனின் ஒலியை குறைக்கிறேன்.",
                    kn = "ಫೋನ್ ಶಬ್ದವನ್ನು ಕಡಿಮೆ ಮಾಡುತ್ತಿದ್ದೇನೆ.",
                    ml = "ഫോണിന്റെ ശബ്ദം കുറയ്ക്കുന്നു.",
                    mr = "फोनचा आवाज कमी करत आहे.",
                    bn = "ফোনের আওয়াজ কমাচ্ছি।",
                    gu = "ફોનનો અવાજ ઘટાડું છું.",
                    pa = "ਫ਼ੋਨ ਦੀ ਆਵਾਜ਼ ਘਟਾ ਰਿਹਾ ਹਾਂ।",
                    or = "ଫୋନର ଶବ୍ଦ କମାଉଛି।",
                    asText = "ফোনৰ শব্দ কমাই আছোঁ।",
                    ur = "فون کی آواز کم کر رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 8. Wi-Fi or Phone Settings
        if (isWifiIntent(normalized)) {
            val action = PhoneAction.OpenSettings(SettingsTarget.WIFI)
            return AssistantReply(
                intent = AssistantIntent.OpenSettings(SettingsTarget.WIFI),
                responseText = formatReply(
                    replyLang,
                    en = "Opening Wi-Fi settings. Turn the top switch ON and tap your home Wi-Fi name.",
                    hi = "Wi-Fi सेटिंग्स खोल रहा हूँ। ऊपर वाला बटन चालू करें और अपने घर के Wi-Fi का नाम चुनें।",
                    te = "Wi-Fi సెట్టింగ్స్ తెరుస్తున్నాను. పైన ఉన్న స్విచ్ ఆన్ చేసి మీ ఇంటి Wi-Fi పేరును నొక్కండి.",
                    ta = "Wi-Fi அமைப்புகளை திறக்கிறேன். மேலே உள்ள பொத்தானை ஆன் செய்யுங்கள்.",
                    kn = "Wi-Fi ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯುತ್ತಿದ್ದೇನೆ. ಮೇಲಿನ ಬಟನ್ ಆನ್ ಮಾಡಿ.",
                    ml = "Wi-Fi സെറ്റിംഗ്സ് തുറക്കുന്നു.",
                    mr = "Wi-Fi सेटिंग्ज उघडत आहे.",
                    bn = "Wi-Fi সেটিংস খুলছি।",
                    gu = "Wi-Fi સેટિંગ્સ ખોલું છું.",
                    pa = "Wi-Fi ਸੈਟਿੰਗਾਂ ਖੋਲ੍ਹ ਰਿਹਾ ਹਾਂ।",
                    or = "Wi-Fi ସେଟିଂସ୍ ଖୋଲୁଛି।",
                    asText = "Wi-Fi ছেটিংছ খুলি আছোঁ।",
                    ur = "Wi-Fi سیٹنگز کھول رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        if (isSettingsIntent(normalized)) {
            val action = PhoneAction.OpenSettings(SettingsTarget.MAIN)
            return AssistantReply(
                intent = AssistantIntent.OpenSettings(SettingsTarget.MAIN),
                responseText = formatReply(
                    replyLang,
                    en = "Opening your phone's Settings screen. Tap the Back button anytime to come back to VaaniX.",
                    hi = "आपके फ़ोन की सेटिंग्स खोल रहा हूँ। वापस आने के लिए बैक बटन दबाएँ।",
                    te = "మీ ఫోన్ సెట్టింగ్స్ తెరుస్తున్నాను. తిరిగి రావడానికి బ్యాక్ బటన్ నొక్కండి.",
                    ta = "உங்கள் போன் அமைப்புகளை திறக்கிறேன்.",
                    kn = "ನಿಮ್ಮ ಫೋನ್ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯುತ್ತಿದ್ದೇನೆ.",
                    ml = "നിങ്ങളുടെ ഫോൺ സെറ്റിംഗ്സ് തുറക്കുന്നു.",
                    mr = "तुमच्या फोनची सेटिंग्ज उघडत आहे.",
                    bn = "আপনার ফোনের সেটিংস খুলছি।",
                    gu = "તમારા ફોનની સેટિંગ્સ ખોલું છું.",
                    pa = "ਤੁਹਾਡੇ ਫ਼ੋਨ ਦੀਆਂ ਸੈਟਿੰਗਾਂ ਖੋਲ੍ਹ ਰਿਹਾ ਹਾਂ।",
                    or = "ଆପଣଙ୍କ ଫୋନର ସେଟିଂସ୍ ଖୋଲୁଛି।",
                    asText = "আপোনাৰ ফোনৰ ছেটিংছ খুলি আছোঁ।",
                    ur = "آپ کے فون کی سیٹنگز کھول رہا ہوں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 9. Set Alarm
        if (isAlarmIntent(normalized)) {
            val (hour, minute) = extractAlarmTime(normalized)
            val action = PhoneAction.SetAlarm(hour, minute, "VaaniX Reminder")
            return AssistantReply(
                intent = AssistantIntent.SetAlarm(hour, minute, "VaaniX Reminder"),
                responseText = formatReply(
                    replyLang,
                    en = "Opening your Clock app to set an alarm for %02d:%02d.".format(hour, minute),
                    hi = "%02d:%02d का अलार्म लगाने के लिए घड़ी ऐप खोल रहा हूँ।".format(hour, minute),
                    te = "%02d:%02d కి అలారం పెట్టడానికి క్లాక్ యాప్ తెరుస్తున్నాను.".format(hour, minute),
                    ta = "%02d:%02d மணிக்கு அலாரம் வைக்க கடிகாரத்தை திறக்கிறேன்.".format(hour, minute),
                    kn = "%02d:%02d ಕ್ಕೆ ಅಲಾರಂ ಇಡಲು ಗಡಿಯಾರ ತೆರೆಯುತ್ತಿದ್ದೇನೆ.".format(hour, minute),
                    ml = "%02d:%02d ന് അലാറം വെക്കാൻ ക്ലോക്ക് തുറക്കുന്നു.".format(hour, minute),
                    mr = "%02d:%02d चा अलार्म लावण्यासाठी घड्याळ उघडत आहे.".format(hour, minute),
                    bn = "%02d:%02d-এর অ্যালার্ম দিতে ঘড়ি খুলছি।".format(hour, minute),
                    gu = "%02d:%02d નો એલાર્મ સેટ કરવા ઘડિયાળ ખોલું છું.".format(hour, minute),
                    pa = "%02d:%02d ਦਾ ਅਲਾਰਮ ਲਗਾਉਣ ਲਈ ਘੜੀ ਖੋਲ੍ਹ ਰਿਹਾ ਹਾਂ।".format(hour, minute),
                    or = "%02d:%02d ପାଇଁ ଆଲାର୍ମ ଦେବାକୁ ଘଣ୍ଟା ଖୋଲୁଛି।".format(hour, minute),
                    asText = "%02d:%02d ত এলাৰ্ম দিবলৈ ঘড়ী খুলি আছোঁ।".format(hour, minute),
                    ur = "%02d:%02d کا الارم لگانے کے لیے کلاک ایپ کھول رہا ہوں۔".format(hour, minute)
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 10. Incoming messages / notifications privacy-respecting guidance
        if (isIncomingMessageCheckIntent(normalized)) {
            return AssistantReply(
                intent = AssistantIntent.CheckMessagesHelp,
                responseText = formatReply(
                    replyLang,
                    en = "For your privacy, VaaniX does not secretly read your private SMS or notifications in the background. Open the \"Screen & Phone\" tab below and paste or share any message—I will read and explain it clearly.",
                    hi = "आपकी निजता (privacy) के लिए VaaniX चुपचाप आपके मैसेज नहीं पढ़ता। नीचे 'Screen & Phone' टैब में कोई भी मैसेज पेस्ट या शेयर करें—मैं उसे आसान भाषा में पढ़कर समझाऊँगा।",
                    te = "మీ గోప్యత కోసం VaaniX మీ మెసేజ్‌లను రహస్యంగా చదవదు. కింద ఉన్న 'Screen & Phone' ట్యాబ్‌లో ఏ మెసేజ్ అయినా పేస్ట్ చేయండి—నేను తెలుగులో వివరిస్తాను.",
                    ta = "உங்கள் தனியுரிமைக்காக VaaniX உங்கள் செய்திகளை ரகசியமாக படிக்காது. 'Screen & Phone' பகுதியில் செய்தியை ஒட்டுங்கள், நான் எளிதாக விளக்குகிறேன்.",
                    kn = "ನಿಮ್ಮ ಗೌಪ್ಯತೆಗಾಗಿ VaaniX ಸಂದೇಶಗಳನ್ನು ರಹಸ್ಯವಾಗಿ ಓದುವುದಿಲ್ಲ. 'Screen & Phone' ಟ್ಯಾಬ್‌ನಲ್ಲಿ ಸಂದೇಶ ಹಾಕಿ, ನಾನು ಸರಳವಾಗಿ ವಿವರಿಸುತ್ತೇನೆ.",
                    ml = "നിങ്ങളുടെ സ്വകാര്യതയ്ക്കായി VaaniX രഹസ്യമായി സന്ദേശങ്ങൾ വായിക്കില്ല. 'Screen & Phone' ടാബിൽ സന്ദേശം നൽകൂ, ഞാൻ വിശദീകരിക്കാം.",
                    mr = "तुमच्या गोपनीयतेसाठी VaaniX गुपचूप मेसेज वाचत नाही. 'Screen & Phone' टॅबमध्ये मेसेज पेस्ट करा, मी सोप्या भाषेत समजावून सांगेन.",
                    bn = "আপনার গোপনীয়তার জন্য VaaniX গোপনে মেসেজ পড়ে না। 'Screen & Phone' ট্যাবে মেসেজটি দিন, আমি সহজ বাংলায় বুঝিয়ে দেব।",
                    gu = "તમારી ગોપનીયતા માટે VaaniX છૂપી રીતે મેસેજ વાંચતું નથી. 'Screen & Phone' ટેબમાં મેસેજ મૂકો, હું સરળતાથી સમજાવીશ.",
                    pa = "ਤੁਹਾਡੀ ਨਿੱਜਤਾ ਲਈ VaaniX ਚੁੱਪ-ਚਾਪ ਸੁਨੇਹੇ ਨਹੀਂ ਪੜ੍ਹਦਾ। 'Screen & Phone' ਵਿੱਚ ਸੁਨੇਹਾ ਪਾਓ, ਮੈਂ ਆਸਾਨ ਭਾਸ਼ਾ ਵਿੱਚ ਸਮਝਾਵਾਂਗਾ।",
                    or = "ଆପଣଙ୍କ ଗୋପନୀୟତା ପାଇଁ VaaniX ଲୁଚି ମେସେଜ୍ ପଢ଼େ ନାହିଁ। 'Screen & Phone' ରେ ମେସେଜ୍ ଦିଅନ୍ତୁ, ମୁଁ ବୁଝାଇବି।",
                    asText = "আপোনাৰ গোপনীয়তাৰ বাবে VaaniX-এ মনে মনে বাৰ্তা নপঢ়ে। 'Screen & Phone' ত বাৰ্তা দিয়ক, মই বুজাই দিম।",
                    ur = "آپ کی پرائیویسی کے لیے VaaniX خفیہ طور پر پیغامات نہیں پڑھتا۔ نیچے 'Screen & Phone' میں پیغام پیسٹ کریں، میں آسان زبان میں سمجھاؤں گا۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 11. Screen explanation request
        if (isScreenExplainIntent(normalized)) {
            return AssistantReply(
                intent = AssistantIntent.ScreenHelp,
                responseText = formatReply(
                    replyLang,
                    en = "I can explain any confusing screen, popup, or message in simple words. Tap the \"Screen & Phone\" tab below, paste or share the text, and I will tell you what it means and whether it is safe.",
                    hi = "मैं किसी भी उलझन वाले स्क्रीन या मैसेज को आसान भाषा में समझा सकता हूँ। नीचे 'Screen & Phone' टैब खोलें और टेक्स्ट पेस्ट करें।",
                    te = "మీ స్క్రీన్ మీద ఉన్న ఏ సందేశాన్నైనా నేను సులభంగా వివరిస్తాను. కింద ఉన్న 'Screen & Phone' ట్యాబ్ తెరిచి టెక్స్ట్ పేస్ట్ చేయండి.",
                    ta = "திரையில் உள்ள எந்த செய்தியையும் எளிமையாக விளக்குவேன். கீழே உள்ள 'Screen & Phone' பகுதியை திறக்கவும்.",
                    kn = "ಸ್ಕ್ರೀನ್ ಮೇಲಿರುವ ಯಾವುದೇ ಸಂದೇಶವನ್ನು ಸರಳವಾಗಿ ವಿವರಿಸುತ್ತೇನೆ. ಕೆಳಗಿನ 'Screen & Phone' ಟ್ಯಾಬ್ ತೆರೆಯಿರಿ.",
                    ml = "സ്ക്രീനിലെ ഏത് സന്ദേശവും ലളിതമായി വിശദീകരിക്കാം. താഴെയുള്ള 'Screen & Phone' ടാബ് തുറക്കൂ.",
                    mr = "स्क्रीनवरील कोणताही मेसेज मी सोप्या भाषेत समजावेन. खालील 'Screen & Phone' टॅब उघडा.",
                    bn = "স্ক্রিনের যেকোনো লেখা আমি সহজ বাংলায় বুঝিয়ে দেব। নিচের 'Screen & Phone' ট্যাব খুলুন।",
                    gu = "સ્ક્રીન પરનો કોઈપણ મેસેજ હું સરળ ભાષામાં સમજાવીશ. નીચેનો 'Screen & Phone' ટેબ ખોલો.",
                    pa = "ਸਕ੍ਰੀਨ ਦਾ ਕੋਈ ਵੀ ਸੁਨੇਹਾ ਮੈਂ ਸੌਖੀ ਭਾਸ਼ਾ ਵਿੱਚ ਸਮਝਾਵਾਂਗਾ। ਹੇਠਾਂ 'Screen & Phone' ਖੋਲ੍ਹੋ।",
                    or = "ସ୍କ୍ରିନରେ ଥିବା ଯେକୌଣସି ଲେଖା ମୁଁ ସରଳ ଭାଷାରେ ବୁଝାଇବି। ତଳେ 'Screen & Phone' ଖୋଲନ୍ତୁ।",
                    asText = "স্ক্ৰীণত থকা যিকোনো লেখা মই সহজকৈ বুজাই দিম। তলৰ 'Screen & Phone' খোলক।",
                    ur = "میں اسکرین کے کسی بھی پیغام کو آسان زبان میں سمجھا سکتا ہوں۔ نیچے 'Screen & Phone' کھولیں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 12. Teach me how to use the phone
        if (isTeachPhoneIntent(normalized)) {
            val action = PhoneAction.ShowStepByStepGuide(PhoneGuideTopic.GENERAL_PHONE)
            return AssistantReply(
                intent = AssistantIntent.TeachPhone(PhoneGuideTopic.GENERAL_PHONE),
                responseText = formatReply(
                    replyLang,
                    en = "I would be happy to teach you step by step! Here are 3 easy basics: 1) Tap the big orange Mic button anytime to ask me to open WhatsApp or YouTube. 2) Tap the red Stop button if I speak too fast. 3) Never share an OTP code with anyone on a phone call. Open 'Screen & Phone' for more simple guides.",
                    hi = "मैं खुशी से आपको फ़ोन चलाना सिखाऊँगा! 3 आसान बातें: 1) WhatsApp या YouTube खोलने के लिए नीचे बड़ा माइक बटन दबाकर बोलें। 2) आवाज़ रोकने के लिए लाल 'रोकें' बटन दबाएँ। 3) किसी को भी फ़ोन पर OTP कभी न बताएँ।",
                    te = "మీకు ఫోన్ వాడటం నేర్పించడం నాకు చాలా సంతోషం! 3 సులభమైన విషయాలు: 1) WhatsApp లేదా YouTube కోసం కింద పెద్ద మైక్ నొక్కి చెప్పండి. 2) నేను చెప్పేది ఆపాలంటే ఎరుపు 'ఆపండి' బటన్ నొక్కండి. 3) ఎవరికీ OTP చెప్పకండి.",
                    ta = "உங்களுக்கு போன் பயன்படுத்த கற்றுக்கொடுப்பதில் மகிழ்ச்சி! 1) கீழே உள்ள மைக் பொத்தானை அழுத்தி WhatsApp அல்லது YouTube திறக்கச் சொல்லலாம். 2) நிறுத்த சிவப்பு பொத்தானை அழுத்தவும். 3) OTP எண்ணை யாரிடமும் பகிர வேண்டாம்.",
                    kn = "ನಿಮಗೆ ಫೋನ್ ಬಳಸಲು ಕಲಿಸಲು ನನಗೆ ಸಂತೋಷ! 1) ಕೆಳಗಿನ ಮೈಕ್ ಒತ್ತಿ WhatsApp ಅಥವಾ YouTube ತೆರೆಯಲು ಹೇಳಿ. 2) ನಿಲ್ಲಿಸಲು ಕೆಂಪು ಬಟನ್ ಒತ್ತಿ. 3) ಯಾರಿಗೂ OTP ಹೇಳಬೇಡಿ.",
                    ml = "ഫോൺ ഉപയോഗിക്കാൻ പഠിപ്പിക്കാൻ എനിക്ക് സന്തോഷമാണ്! 1) താഴെയുള്ള മൈക്ക് അമർത്തി WhatsApp അല്ലെങ്കിൽ YouTube തുറക്കാൻ പറയൂ. 2) നിർത്താൻ ചുവപ്പ് ബട്ടൺ അമർത്തുക. 3) OTP ആരോടും പറയരുത്.",
                    mr = "तुम्हाला फोन शिकवायला मला आनंद होईल! 1) खालील मोठा माइक दाबून WhatsApp किंवा YouTube उघडायला सांगा. 2) थांबण्यासाठी लाल बटण दाबा. 3) कोणालाही OTP सांगू नका.",
                    bn = "আপনাকে ফোন চালানো শেখাতে পেরে আমি খুশি! ১) নিচের বড় মাইক টিপে WhatsApp বা YouTube খুলতে বলুন। ২) থামাতে লাল বোতাম চাপুন। ৩) কাউকে কখনো OTP বলবেন না।",
                    gu = "તમને ફોન શીખવવામાં મને આનંદ થશે! ૧) નીચેનું મોટું માઈક દબાવી WhatsApp કે YouTube ખોલવા કહો. ૨) રોકવા માટે લાલ બટન દબાવો. ૩) કોઈને OTP ન કહેશો.",
                    pa = "ਤੁਹਾਨੂੰ ਫ਼ੋਨ ਸਿਖਾ ਕੇ ਮੈਨੂੰ ਖੁਸ਼ੀ ਹੋਵੇਗੀ! 1) ਹੇਠਾਂ ਮਾਈਕ ਦਬਾ ਕੇ WhatsApp ਜਾਂ YouTube ਖੋਲ੍ਹਣ ਲਈ ਕਹੋ। 2) ਰੋਕਣ ਲਈ ਲਾਲ ਬਟਨ ਦਬਾਓ। 3) ਕਿਸੇ ਨੂੰ OTP ਨਾ ਦੱਸੋ।",
                    or = "ଆପଣଙ୍କୁ ଫୋନ୍ ଶିଖାଇବାକୁ ମୋତେ ଖୁସି ଲାଗିବ! ୧) ତଳେ ମାଇକ୍ ଦବାଇ WhatsApp କିମ୍ବା YouTube ଖୋଲିବାକୁ କୁହନ୍ତୁ। ୨) ରୋକିବାକୁ ଲାଲ୍ ବଟନ୍ ଦବାନ୍ତୁ। ୩) କାହାକୁ OTP କୁହନ୍ତୁ ନାହିଁ।",
                    asText = "আপোনাক ফোন শিকাবলৈ পাই মই সুখী! ১) তলৰ মাইক টিপি WhatsApp বা YouTube খুলিবলৈ কওক। ২) ৰখাবলৈ ৰঙা বুটাম টিপক। ৩) কাকো OTP নক’ব।",
                    ur = "مجھے آپ کو فون سکھا کر خوشی ہوگی! 1) نیچے مائیک دبا کر WhatsApp یا YouTube کھولنے کو کہیں۔ 2) روکنے کے لیے سرخ بٹن دبائیں۔ 3) کسی کو بھی OTP نہ بتائیں۔"
                ),
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                proposedAction = action,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 13. Greeting
        if (isGreeting(normalized)) {
            val localized = LocalizedStringsProvider.forLanguage(replyLang)
            return AssistantReply(
                intent = AssistantIntent.Greeting,
                responseText = localized.greeting,
                responseLanguage = replyLang,
                detectedInputLanguage = detection.detectedLanguage,
                isMixedLanguageInput = detection.isMixedLanguage,
                providerName = providerId,
                isMockProvider = VaaniConfig.isMockProviderActive
            )
        }

        // 14. Honest fallback (never pretend mock provider understood an unsupported complex query)
        return AssistantReply(
            intent = AssistantIntent.Unknown,
            responseText = formatReply(
                replyLang,
                en = "I heard: \"$trimmed\". I am currently running in Offline Guided Mode (zero-cost mock assistant), so I can best help with opening WhatsApp, searching YouTube, changing volume, Wi-Fi or Phone settings, setting an alarm, preparing a call or message, or explaining a pasted message.",
                hi = "मैंने सुना: \"$trimmed\"। अभी मैं ऑफ़लाइन गाइडेड मोड में हूँ। आप मुझसे WhatsApp खोलने, YouTube पर वीडियो खोजने, आवाज़ बढ़ाने, Wi-Fi सेटिंग्स, अलार्म लगाने या फ़ोन सीखने के लिए कह सकते हैं।",
                te = "నేను విన్నది: \"$trimmed\". ప్రస్తుతం నేను ఆఫ్‌లైన్ గైడెడ్ మోడ్‌లో ఉన్నాను. WhatsApp తెరవడం, YouTube లో పాటలు వెతకడం, వాల్యూమ్ మార్చడం, Wi-Fi సెట్టింగ్స్ లేదా ఫోన్ నేర్చుకోవడం గురించి అడగండి.",
                ta = "நான் கேட்டது: \"$trimmed\". இப்போது ஆஃப்லைன் வழிகாட்டி நிலையில் உள்ளேன். WhatsApp திறக்க, YouTube தேட, ஒலி மாற்ற அல்லது போன் அமைப்புகளுக்கு கேளுங்கள்.",
                kn = "ನಾನು ಕೇಳಿಸಿಕೊಂಡಿದ್ದು: \"$trimmed\". WhatsApp ತೆರೆಯಲು, YouTube ಹುಡುಕಲು, ಶಬ್ದ ಬದಲಿಸಲು ಅಥವಾ ಫೋನ್ ಸೆಟ್ಟಿಂಗ್ಸ್ ಕೇಳಿ.",
                ml = "ഞാൻ കേട്ടത്: \"$trimmed\". WhatsApp തുറക്കാനോ YouTube തിരയാനോ ശബ്ദം മാറ്റാനോ ചോദിക്കാം.",
                mr = "मी ऐकले: \"$trimmed\". तुम्ही WhatsApp उघडणे, YouTube शोधणे, आवाज वाढवणे किंवा सेटिंग्ज बद्दल विचारू शकता.",
                bn = "আমি শুনেছি: \"$trimmed\"। আপনি WhatsApp খোলা, YouTube খোঁজা, আওয়াজ বাড়ানো বা সেটিংস সম্পর্কে বলতে পারেন।",
                gu = "મેં સાંભળ્યું: \"$trimmed\". તમે WhatsApp ખોલવા, YouTube શોધવા, અવાજ વધારવા અથવા સેટિંગ્સ માટે કહી શકો છો.",
                pa = "ਮੈਂ ਸੁਣਿਆ: \"$trimmed\"। ਤੁਸੀਂ WhatsApp ਖੋਲ੍ਹਣ, YouTube ਲੱਭਣ, ਆਵਾਜ਼ ਵਧਾਉਣ ਜਾਂ ਸੈਟਿੰਗਾਂ ਬਾਰੇ ਪੁੱਛ ਸਕਦੇ ਹੋ।",
                or = "ମୁଁ ଶୁଣିଲି: \"$trimmed\"। ଆପଣ WhatsApp ଖୋଲିବା, YouTube ଖୋଜିବା, ଶବ୍ଦ ବଢ଼ାଇବା କିମ୍ବା ସେଟିଂସ୍ ବିଷୟରେ ପଚାରିପାରିବେ।",
                asText = "মই শুনিলোঁ: \"$trimmed\"। আপুনি WhatsApp খোলা, YouTube বিচৰা, শব্দ বঢ়োৱা বা ছেটিংছৰ বিষয়ে সুধিব পাৰে।",
                ur = "میں نے سنا: \"$trimmed\"۔ آپ مجھ سے WhatsApp کھولنے، YouTube تلاش کرنے، آواز بڑھانے یا سیٹنگز کے بارے میں کہہ سکتے ہیں۔"
            ),
            responseLanguage = replyLang,
            detectedInputLanguage = detection.detectedLanguage,
            isMixedLanguageInput = detection.isMixedLanguage,
            providerName = providerId,
            isMockProvider = VaaniConfig.isMockProviderActive
        )
    }

    private fun mentionsApp(text: String, app: String): Boolean = text.contains(app)

    private fun mentionsYouTube(text: String): Boolean =
        listOf("youtube", "you tube", "యూట్యూబ్", "यूट्यूब", "யூடியூப்", "ಯೂಟ್ಯೂಬ್", "യൂട്യൂബ്", "ইউটিউব", "યુટ્યુબ", "یوٹیوب").any {
            text.contains(it)
        }

    private fun isSearchRequest(text: String): Boolean =
        listOf("search", "find", "play", "खोजो", "ढूंढो", "చూపించు", "వెతుకు", "தேடு", "ಹುಡುಕಿ", "തിരയൂ", "शोधा", "খুঁজুন", "શોધો", "ਲੱਭੋ", "ଖୋଜନ୍ତୁ", "বিচাৰক", "تلاش").any {
            text.contains(it)
        }

    private fun containsSongOrVideoHint(text: String): Boolean =
        listOf("song", "songs", "bhajan", "devotional", "video", "पाट", "పాటలు", "భక్తి", "भजन", "गाने", "பாடல்கள்", "ಗೀತೆ", "പാട്ടുകൾ", "गाणी", "গান", "ગીત", "ਗੀਤ", "ଗୀତ", "گانے").any {
            text.contains(it)
        }

    private fun isSendMessageIntent(text: String): Boolean {
        val sendWords = listOf(
            "send message", "send a message", "send whatsapp", "message bhej", "message bhejo",
            "message pampinchu", "మెసేజ్ పంపించు", "संदेश भेजो", "मैसेज भेजो", "செய்தி அனுப்பு",
            "ಸಂದೇಶ ಕಳುಹಿಸಿ", "സന്ദേശം അയക്കൂ", "मेसेज पाठवा", "মেসেজ পাঠাও", "મેસેજ મોકલો",
            "ਸੁਨੇਹਾ ਭੇਜੋ", "ମେସେଜ୍ ପଠାନ୍ତୁ", "বাৰ্তা পঠিয়াওক", "پیغام بھیجیں", "kodukuki message"
        )
        return sendWords.any { text.contains(it) }
    }

    private fun isPhoneCallIntent(text: String): Boolean {
        val callWords = listOf(
            "call ", "phone call", "dial ", "कॉल करो", "फोन लगाओ", "కాల్ చేయి", "ఫోన్ చేయి",
            "அழைப்பு", "ಕರೆ ಮಾಡಿ", "വിളിക്കൂ", "कॉल करा", "কল করো", "કોલ કરો", "ਕਾਲ ਕਰੋ", "କଲ୍ କରନ୍ତୁ", "فون کریں"
        )
        return callWords.any { text.contains(it) } && !text.contains("missed call")
    }

    private fun isVolumeUpIntent(text: String): Boolean {
        val hasVol = listOf("volume", "sound", "awaz", "आवाज़", "आवाज", "వాల్యూమ్", "సౌండ్", "ஒலி", "ಶಬ್ದ", "ശബ്ദം", "আওয়াজ", "અવાજ", "ਆਵਾਜ਼", "ଶବ୍ଦ", "শব্দ", "آواز").any { text.contains(it) }
        val hasUp = listOf("up", "increase", "louder", "raise", "high", "badhao", "बढ़ा", "बढा", "పెంచు", "அதிகரி", "ಜಾಸ್ತಿ", "കൂട്ടൂ", "वाढवा", "বাড়ান", "વધારો", "ਵਧਾਓ", "ବଢ଼ାନ୍ତୁ", "বঢ়াওক", "بڑھائیں").any { text.contains(it) }
        return hasVol && hasUp
    }

    private fun isVolumeDownIntent(text: String): Boolean {
        val hasVol = listOf("volume", "sound", "awaz", "आवाज़", "आवाज", "వాల్యూమ్", "సౌండ్", "ஒலி", "ಶಬ್ದ", "ശബ്ദം", "আওয়াজ", "અવાજ", "ਆਵਾਜ਼", "ଶବ୍ଦ", "শব্দ", "آواز").any { text.contains(it) }
        val hasDown = listOf("down", "decrease", "lower", "reduce", "quiet", "kam", "कम", "తగ్గించు", "குறை", "ಕಡಿಮೆ", "കുറയ്ക്കൂ", "कमी", "কমান", "ઘટાડો", "ਘਟਾਓ", "କମାନ୍ତୁ", "কমাওক", "کم کریں").any { text.contains(it) }
        return hasVol && hasDown
    }

    private fun isWifiIntent(text: String): Boolean =
        listOf("wi-fi", "wifi", "internet settings", "वाई-फाई", "వైఫై", "வைஃபை").any { text.contains(it) }

    private fun isSettingsIntent(text: String): Boolean =
        listOf("settings", "सेटिंग", "సెట్టింగ్స్", "அமைப்புகள்", "ಸೆಟ್ಟಿಂಗ್ಸ್", "സെറ്റിംഗ്സ്", "सेटिंग्ज", "সেটিংস", "સેટિંગ્સ", "ਸੈਟਿੰਗਾਂ", "ସେଟିଂସ୍", "ছেটিংছ", "سیٹنگز").any { text.contains(it) }

    private fun isAlarmIntent(text: String): Boolean =
        listOf("alarm", "अलार्म", "అలారం", "அலாரம்", "ಅಲಾರಂ", "അലാറം", "অ্যালার্ম", "એલાર્મ", "ਅਲਾਰਮ", "ଆଲାର୍ମ", "এলাৰ্ম", "الارم").any { text.contains(it) }

    private fun isIncomingMessageCheckIntent(text: String): Boolean =
        listOf(
            "kya message aaya", "क्या मैसेज आया", "what message", "read my messages", "new message",
            "notification", "ఏ మెసేజ్ వచ్చింది", "మెసేజ్ వచ్చిందా", "मेरे फोन में क्या मैसेज"
        ).any { text.contains(it) }

    private fun isScreenExplainIntent(text: String): Boolean =
        listOf(
            "on my screen", "written on my screen", "read this", "explain this", "which button",
            "स्क्रीन पर", "స్క్రీన్ మీద", "திரையில்", "ಸ್ಕ್ರೀನ್", "स्क्रीनवर"
        ).any { text.contains(it) }

    private fun isTeachPhoneIntent(text: String): Boolean =
        listOf(
            "teach me", "sikhao", "सिखाओ", "नेर्पिंचु", "నేర్పించు", "ఫోన్ వాడటం",
            "கற்றுக்கொடு", "ಹೇಗೆ ತಿಳಿಸಿ", "പഠിപ്പിക്കൂ", "शिकवा", "শেখান", "શીખવો",
            "ਸਿਖਾਓ", "ଶିଖାନ୍ତୁ", "শিকাওক", "سکھائیں", "how to use phone", "use karna"
        ).any { text.contains(it) }

    private fun isGreeting(text: String): Boolean =
        listOf("hello", "hi", "namaste", "namaskaram", "vanakkam", "namaskara", "नमस्ते", "నమస్కారం", "வணக்கம்", "ನಮಸ್ಕಾರ", "നമസ്കാരം", "নমস্কার", "નમસ્તે", "ਸਤ ਸ੍ਰੀ ਅਕਾਲ", "ନମସ୍କାର", "নমস্কাৰ", "السلام علیکم").any {
            text.contains(it)
        }

    private fun extractSearchQuery(raw: String): String {
        val cleaned = raw
            .replace(Regex("(?i)\\b(open|youtube|you tube|karke|and|search|find|play|karo|for|on|in|me|mein|par|per)\\b"), " ")
            .replace(Regex("(లో|पर|இல்|ನಲ್ಲಿ|ൽ|वर|এ|પર|ਤੇ|ରେ|ত)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length >= 2) cleaned else "devotional songs"
    }

    private fun extractRecipientHint(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("kodukuki") || lower.contains("కొడుకు") || lower.contains("son") || lower.contains("bete") -> "Son"
            lower.contains("daughter") || lower.contains("beti") || lower.contains("కూతురు") -> "Daughter"
            lower.contains("doctor") -> "Doctor"
            else -> {
                val afterTo = Regex("(?i)(?:to|for)\\s+([a-zA-Z0-9 +]{2,24})").find(raw)?.groupValues?.getOrNull(1)?.trim()
                afterTo ?: "your contact"
            }
        }
    }

    private fun extractCallTarget(raw: String): String {
        val number = Regex("\\+?\\d[\\d\\s-]{4,14}").find(raw)?.value?.trim()
        if (!number.isNullOrBlank()) return number
        val afterCall = Regex("(?i)(?:call|dial)\\s+([a-zA-Z ]{2,24})").find(raw)?.groupValues?.getOrNull(1)?.trim()
        return afterCall ?: "your contact"
    }

    private fun extractAlarmTime(lower: String): Pair<Int, Int> {
        val match = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(lower)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 7
            val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val amPm = match.groupValues[3]
            if (amPm == "pm" && hour in 1..11) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return hour to minute
        }
        return 7 to 0
    }

    private fun buildDraftMessage(lang: SupportedLanguage): String = when (lang.bcp47Tag) {
        "te-IN" -> "నమస్కారం, ఎలా ఉన్నారు? వీలైనప్పుడు నాకు కాల్ చేయండి."
        "hi-IN" -> "नमस्ते, आप कैसे हैं? समय मिलने पर कृपया मुझे कॉल करें।"
        "ta-IN" -> "வணக்கம், நலமா? நேரம் கிடைக்கும் போது அழைக்கவும்."
        "kn-IN" -> "ನಮಸ್ಕಾರ, ಹೇಗಿದ್ದೀರಿ? ಸಮಯ ಸಿಕ್ಕಾಗ ದಯವಿಟ್ಟು ಕರೆ ಮಾಡಿ."
        "ml-IN" -> "നമസ്കാരം, സുഖമാണോ? സമയം കിട്ടുമ്പോൾ വിളിക്കൂ."
        "mr-IN" -> "नमस्कार, कसे आहात? वेळ मिळाल्यावर मला कॉल करा."
        "bn-IN" -> "নমস্কার, কেমন আছেন? সময় পেলে আমাকে কল করবেন।"
        "gu-IN" -> "નમસ્તે, કેમ છો? સમય મળે ત્યારે મને ફોન કરજો."
        "pa-IN" -> "ਸਤ ਸ੍ਰੀ ਅਕਾਲ, ਤੁਸੀਂ ਕਿਵੇਂ ਹੋ? ਸਮਾਂ ਮਿਲਣ ਤੇ ਕਾਲ ਕਰਿਓ।"
        "or-IN" -> "ନମସ୍କାର, କେମିତି ଅଛନ୍ତି? ସମୟ ମିଳିଲେ କଲ୍ କରିବେ।"
        "as-IN" -> "নমস্কাৰ, কেনে আছে? সময় পালে মোক ফোন কৰিব।"
        "ur-IN" -> "السلام علیکم، آپ کیسے ہیں؟ وقت ملنے پر مجھے کال کریں۔"
        else -> "Hello, how are you? Please call me when you are free."
    }

    private fun formatReply(
        lang: SupportedLanguage,
        en: String,
        hi: String,
        te: String,
        ta: String,
        kn: String,
        ml: String,
        mr: String,
        bn: String,
        gu: String,
        pa: String,
        or: String,
        asText: String,
        ur: String
    ): String = when (lang.bcp47Tag) {
        "hi-IN" -> hi
        "te-IN" -> te
        "ta-IN" -> ta
        "kn-IN" -> kn
        "ml-IN" -> ml
        "mr-IN" -> mr
        "bn-IN" -> bn
        "gu-IN" -> gu
        "pa-IN" -> pa
        "or-IN" -> or
        "as-IN" -> asText
        "ur-IN" -> ur
        else -> en
    }
}
