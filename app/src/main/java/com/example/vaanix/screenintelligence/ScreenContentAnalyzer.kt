package com.example.vaanix.screenintelligence

import com.example.vaanix.language.SupportedLanguage

enum class ScreenSafetyAlert {
    NONE,
    OTP_OR_SCAM_CAUTION,
    PERMISSION_EXPLANATION,
    APP_UPDATE_INFO
}

data class ScreenExplanation(
    val title: String,
    val simpleExplanation: String,
    val suggestedNextStep: String,
    val safetyAlert: ScreenSafetyAlert = ScreenSafetyAlert.NONE
)

/**
 * Privacy-first, user-initiated Screen & Message Intelligence analyzer.
 * Operates strictly on text that the user explicitly pastes or shares with VaaniX.
 * Never silently monitors notifications, screens, or passwords in the background.
 */
object ScreenContentAnalyzer {

    private val otpRegex = Regex("\\b(otp|one[- ]?time password|cvv|pin|verification code)\\b", RegexOption.IGNORE_CASE)
    private val bankAlertRegex = Regex("\\b(debited|credited|a/c|account|upi|rs\\.?|inr|₹|bank|balance)\\b", RegexOption.IGNORE_CASE)
    private val permissionRegex = Regex("\\b(allow|deny|permission|access your|camera|location|contacts|microphone|notifications)\\b", RegexOption.IGNORE_CASE)
    private val updateRegex = Regex("\\b(update available|new version|storage full|low storage|battery low|restart)\\b", RegexOption.IGNORE_CASE)
    private val deliveryRegex = Regex("\\b(out for delivery|delivered|order|parcel|courier|tracking)\\b", RegexOption.IGNORE_CASE)
    private val missedCallRegex = Regex("\\b(missed call|called you|voicemail)\\b", RegexOption.IGNORE_CASE)

    fun analyzeUserProvidedText(
        rawText: String,
        preferredLanguage: SupportedLanguage
    ): ScreenExplanation {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) {
            return ScreenExplanation(
                title = "No text provided yet",
                simpleExplanation = "Please paste a message or share text from another app. VaaniX only reads text you explicitly share—never in the background.",
                suggestedNextStep = "Tap \"Try Sample Message\" or paste any message from SMS or WhatsApp."
            )
        }

        val langName = preferredLanguage.englishName
        val localPrefix = when (preferredLanguage.bcp47Tag) {
            "hi-IN" -> "आसान हिंदी में मतलब: "
            "te-IN" -> "సులభమైన తెలుగులో అర్థం: "
            "ta-IN" -> "எளிய தமிழில் விளக்கம்: "
            "kn-IN" -> "ಸರಳ ಕನ್ನಡದಲ್ಲಿ ಅರ್ಥ: "
            "ml-IN" -> "ലളിതമായ മലയാളത്തിൽ അർത്ഥം: "
            "mr-IN" -> "सोप्या मराठीत अर्थ: "
            "bn-IN" -> "সহজ বাংলায় অর্থ: "
            "gu-IN" -> "સરળ ગુજરાતીમાં અર્થ: "
            "pa-IN" -> "ਸੌਖੀ ਪੰਜਾਬੀ ਵਿੱਚ ਮਤਲਬ: "
            "or-IN" -> "ସହଜ ଓଡ଼ିଆରେ ଅର୍ଥ: "
            "as-IN" -> "সহজ অসমীয়াত অৰ্থ: "
            "ur-IN" -> "آسان اردو میں مطلب: "
            else -> ""
        }

        if (otpRegex.containsMatchIn(cleaned)) {
            val localizedWarning = when (preferredLanguage.bcp47Tag) {
                "hi-IN" -> "${localPrefix}यह एक OTP या गुप्त कोड का संदेश है। इसे किसी अजनबी या फ़ोन कॉल पर कभी न बताएँ।"
                "te-IN" -> "${localPrefix}ఇది ఒక OTP లేదా రహస్య కోడ్ మెసేజ్. ఫోన్ కాల్‌లో ఎవరికీ ఈ కోడ్ చెప్పకండి."
                "ta-IN" -> "${localPrefix}இது ஒரு OTP அல்லது ரகசிய எண். யாராவது போனில் கேட்டால் இதை பகிர வேண்டாம்."
                "kn-IN" -> "${localPrefix}ಇದು OTP ಅಥವಾ ರಹಸ್ಯ ಕೋಡ್ ಸಂದೇಶ. ಫೋನ್‌ನಲ್ಲಿ ಯಾರಿಗೂ ಇದನ್ನು ಹೇಳಬೇಡಿ."
                "mr-IN" -> "${localPrefix}हा OTP किंवा गुप्त कोडचा मेसेज आहे. फोनवर कोणालाही हा कोड सांगू नका."
                else -> "${localPrefix}This message contains a secret OTP or security code. Never read or share this number with anyone on a phone call."
            }
            return ScreenExplanation(
                title = "Security Alert: OTP / Secret Code",
                simpleExplanation = localizedWarning,
                suggestedNextStep = "Only enter this code yourself if you just requested it on a trusted app. Otherwise ignore and close the message.",
                safetyAlert = ScreenSafetyAlert.OTP_OR_SCAM_CAUTION
            )
        }

        if (permissionRegex.containsMatchIn(cleaned)) {
            val explanation = when (preferredLanguage.bcp47Tag) {
                "hi-IN" -> "${localPrefix}यह ऐप आपसे फ़ोन के किसी हिस्से (जैसे माइक, कैमरा या लोकेशन) की अनुमति मांग रहा है। अगर आप इस ऐप पर भरोसा करते हैं तभी 'Allow' दबाएँ, वरना 'Don't allow' चुनें।"
                "te-IN" -> "${localPrefix}ఈ యాప్ మీ ఫోన్‌లోని మైక్, కెమెరా లేదా కాంటాక్ట్స్ కోసం అనుమతి అడుగుతోంది. మీకు నమ్మకం ఉన్న యాప్ అయితేనే 'Allow' నొక్కండి."
                else -> "${localPrefix}An app is asking for permission to use a feature on your phone (such as camera, microphone, or contacts). Tap 'Allow' only if you trust the app and need that feature."
            }
            return ScreenExplanation(
                title = "Phone Permission Prompt",
                simpleExplanation = explanation,
                suggestedNextStep = "If you are unsure why this popped up, it is always safe to tap 'Don't allow' or 'Cancel'.",
                safetyAlert = ScreenSafetyAlert.PERMISSION_EXPLANATION
            )
        }

        if (bankAlertRegex.containsMatchIn(cleaned)) {
            val explanation = when (preferredLanguage.bcp47Tag) {
                "hi-IN" -> "${localPrefix}यह बैंक या UPI लेन-देन की सूचना है। रकम और तारीख ध्यान से देखें। किसी अनजान लिंक पर क्लिक न करें।"
                "te-IN" -> "${localPrefix}ఇది బ్యాంక్ లేదా UPI లావాదేవీ సమాచారం. మొత్తం మరియు తేదీని జాగ్రత్తగా చూడండి. తెలియని లింక్‌లను నొక్కకండి."
                else -> "${localPrefix}This looks like a bank or UPI transaction alert. Check the amount carefully, and do not tap unfamiliar links inside SMS messages."
            }
            return ScreenExplanation(
                title = "Bank or Payment Notification",
                simpleExplanation = explanation,
                suggestedNextStep = "If you did not make this transaction, call your bank's official helpline or ask a trusted family member.",
                safetyAlert = ScreenSafetyAlert.NONE
            )
        }

        if (updateRegex.containsMatchIn(cleaned)) {
            val explanation = when (preferredLanguage.bcp47Tag) {
                "hi-IN" -> "${localPrefix}यह फ़ोन के अपडेट, बैटरी या स्टोरेज की जानकारी है। घबराने की ज़रूरत नहीं है।"
                "te-IN" -> "${localPrefix}ఇది ఫోన్ అప్‌డేట్, బ్యాటరీ లేదా స్టోరేజ్ గురించిన సందేశం. ఆందోళన పడాల్సిన అవసరం లేదు."
                else -> "${localPrefix}This is a regular phone status notice about an app update, storage space, or battery level. Nothing is broken."
            }
            return ScreenExplanation(
                title = "Phone System Notice",
                simpleExplanation = explanation,
                suggestedNextStep = "You can tap 'Later' or 'OK' safely, or charge your phone / connect to Wi-Fi before updating.",
                safetyAlert = ScreenSafetyAlert.APP_UPDATE_INFO
            )
        }

        if (deliveryRegex.containsMatchIn(cleaned)) {
            return ScreenExplanation(
                title = "Order or Delivery Update",
                simpleExplanation = "${localPrefix}This message is an update about a package or delivery order.",
                suggestedNextStep = "Check if you or a family member ordered something. Do not pay extra fees via unknown links."
            )
        }

        if (missedCallRegex.containsMatchIn(cleaned)) {
            return ScreenExplanation(
                title = "Missed Call Notice",
                simpleExplanation = "${localPrefix}Someone tried to call your phone earlier.",
                suggestedNextStep = "If you recognize the name or number, you can ask VaaniX to open the Phone Dialer to call them back."
            )
        }

        val preview = if (cleaned.length > 140) cleaned.take(140) + "..." else cleaned
        return ScreenExplanation(
            title = "Message Summary ($langName)",
            simpleExplanation = "${localPrefix}Here is what the message says: \"$preview\". Note: Full free-form offline translation is limited in zero-cost mode, so VaaniX reads the text clearly for you.",
            suggestedNextStep = "If this message asks for money, passwords, or clicking a strange link, check with a family member before tapping anything."
        )
    }

    val sampleMessages: List<Pair<String, String>> = listOf(
        "OTP Safety Alert" to "Your SBI OTP for login is 482910. Valid for 5 minutes. Do not share with anyone.",
        "App Permission Popup" to "Allow WhatsApp to access your microphone and contacts? Tap Allow or Don't allow.",
        "Wi-Fi / Update Notice" to "A new system update is available. Connect to Wi-Fi and keep battery above 50% to install."
    )
}
