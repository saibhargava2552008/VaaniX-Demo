package com.example.vaanix.language

data class LocalizedUiCopy(
    val greeting: String,
    val tapToSpeak: String,
    val listeningNow: String,
    val thinkingNow: String,
    val speakingNow: String,
    val stopLabel: String,
    val readyHint: String,
    val samplePrompts: List<String>,
    val askConfirmationPrefix: String,
    val cancelledMessage: String,
    val phoneGuideIntro: String,
    val screenHelpPrompt: String,
    val micPermissionNeeded: String
)

/**
 * Genuine, warm, simple everyday phrasing across all 12 major Indian languages + English (India).
 * Avoids technical jargon so elderly users feel welcomed and guided.
 */
object LocalizedStringsProvider {

    fun forLanguage(language: SupportedLanguage): LocalizedUiCopy = when (language.bcp47Tag) {
        "te-IN" -> LocalizedUiCopy(
            greeting = "నమస్కారం! నేను VaaniX. మీ ఫోన్ వాడటంలో సహాయం చేస్తాను. కింద ఉన్న మైక్ నొక్కి మాట్లాడండి.",
            tapToSpeak = "మాట్లాడటానికి నొక్కండి",
            listeningNow = "వింటున్నాను... నిదానంగా మాట్లాడండి",
            thinkingNow = "అర్థం చేసుకుంటున్నాను...",
            speakingNow = "చెబుతున్నాను...",
            stopLabel = "ఆపండి",
            readyHint = "WhatsApp, YouTube, వాల్యూమ్ లేదా ఫోన్ సెట్టింగ్స్ గురించి అడగండి.",
            samplePrompts = listOf(
                "WhatsApp తెరువు",
                "YouTube లో భక్తి పాటలు చూపించు",
                "నా కొడుకుకి మెసేజ్ పంపించు",
                "ఫోన్ వాడటం నేర్పించు"
            ),
            askConfirmationPrefix = "దయచేసి నిర్ధారించండి: ",
            cancelledMessage = "సరే, ఆ పని ఆపేశాను. మీకు ఇంకేమైనా సహాయం కావాలా?",
            phoneGuideIntro = "మీ ఫోన్ సులభంగా వాడటానికి నేను ఒక్కో స్టెప్ చెబుతాను.",
            screenHelpPrompt = "మీ స్క్రీన్ మీద ఉన్న మెసేజ్ ఇక్కడ పేస్ట్ చేయండి, నేను తెలుగులో వివరిస్తాను.",
            micPermissionNeeded = "మీ మాట వినడానికి మైక్రోఫోన్ అనుమతి అవసరం."
        )

        "hi-IN" -> LocalizedUiCopy(
            greeting = "नमस्ते! मैं VaaniX हूँ। मैं आपका फ़ोन चलाने में मदद करूँगा। नीचे माइक दबाकर आराम से बोलिए।",
            tapToSpeak = "बोलने के लिए दबाएँ",
            listeningNow = "सुन रहा हूँ... आराम से बोलिए",
            thinkingNow = "समझ रहा हूँ...",
            speakingNow = "बोल रहा हूँ...",
            stopLabel = "रोकें",
            readyHint = "WhatsApp, YouTube, आवाज़ बढ़ाने या फ़ोन सीखने के लिए बोलें।",
            samplePrompts = listOf(
                "WhatsApp खोलो",
                "YouTube पर भजन खोजो",
                "मेरे फोन में क्या मैसेज आया है?",
                "मुझे फोन चलाना सिखाओ"
            ),
            askConfirmationPrefix = "कृपया पक्का करें: ",
            cancelledMessage = "ठीक है, यह काम रोक दिया है। क्या मैं कुछ और मदद करूँ?",
            phoneGuideIntro = "मैं आपको आसान भाषा में फ़ोन चलाना सिखाऊँगा।",
            screenHelpPrompt = "अपना मैसेज या स्क्रीन का टेक्स्ट यहाँ डालें, मैं आसान हिंदी में समझाऊँगा।",
            micPermissionNeeded = "आपकी आवाज़ सुनने के लिए माइक की अनुमति ज़रूरी है।"
        )

        "ta-IN" -> LocalizedUiCopy(
            greeting = "வணக்கம்! நான் VaaniX. உங்கள் போனை எளிதாக பயன்படுத்த உதவுவேன். கீழே உள்ள மைக்கை அழுத்தி பேசுங்கள்.",
            tapToSpeak = "பேச அழுத்தவும்",
            listeningNow = "கேட்கிறேன்... மெதுவாக பேசுங்கள்",
            thinkingNow = "புரிந்து கொள்கிறேன்...",
            speakingNow = "பேசுகிறேன்...",
            stopLabel = "நிறுத்து",
            readyHint = "WhatsApp, YouTube, சத்தம் அல்லது போன் அமைப்புகள் பற்றி கேளுங்கள்.",
            samplePrompts = listOf(
                "WhatsApp திறக்கவும்",
                "YouTube இல் பக்தி பாடல்கள் தேடு",
                "போன் பயன்படுத்த கற்றுக்கொடு",
                "ஒலியை அதிகரிக்கவும்"
            ),
            askConfirmationPrefix = "தயவுசெய்து உறுதிப்படுத்தவும்: ",
            cancelledMessage = "சரி, அதை நிறுத்திவிட்டேன். வேறு ஏதாவது உதவி வேண்டுமா?",
            phoneGuideIntro = "உங்கள் போனை எளிதாக பயன்படுத்த படிப்படியாக சொல்கிறேன்.",
            screenHelpPrompt = "உங்கள் மெசேஜை இங்கே ஒட்டவும், எளிய தமிழில் விளக்குகிறேன்.",
            micPermissionNeeded = "நீங்கள் பேசுவதை கேட்க மைக் அனுமதி தேவை."
        )

        "kn-IN" -> LocalizedUiCopy(
            greeting = "ನಮಸ್ಕಾರ! ನಾನು VaaniX. ನಿಮ್ಮ ಫೋನ್ ಬಳಸಲು ನಾನು ಸಹಾಯ ಮಾಡುತ್ತೇನೆ. ಕೆಳಗಿನ ಮೈಕ್ ಒತ್ತಿ ಮಾತನಾಡಿ.",
            tapToSpeak = "ಮಾತನಾಡಲು ಒತ್ತಿ",
            listeningNow = "ಕೇಳಿಸಿಕೊಳ್ಳುತ್ತಿದ್ದೇನೆ... ನಿಧಾನವಾಗಿ ಮಾತನಾಡಿ",
            thinkingNow = "ಅರ್ಥಮಾಡಿಕೊಳ್ಳುತ್ತಿದ್ದೇನೆ...",
            speakingNow = "ಹೇಳುತ್ತಿದ್ದೇನೆ...",
            stopLabel = "ನಿಲ್ಲಿಸಿ",
            readyHint = "WhatsApp, YouTube, ಶಬ್ದ ಅಥವಾ ಫೋನ್ ಸೆಟ್ಟಿಂಗ್ಸ್ ಬಗ್ಗೆ ಕೇಳಿ.",
            samplePrompts = listOf(
                "WhatsApp ತೆರೆಯಿರಿ",
                "YouTube ನಲ್ಲಿ ಭಕ್ತಿ ಗೀತೆ ಹುಡುಕಿ",
                "ಫೋನ್ ಬಳಸುವುದು ಹೇಗೆ ತಿಳಿಸಿ",
                "ಶಬ್ದ ಜಾಸ್ತಿ ಮಾಡಿ"
            ),
            askConfirmationPrefix = "ದಯವಿಟ್ಟು ದೃಢಪಡಿಸಿ: ",
            cancelledMessage = "ಸರಿ, ಆ ಕೆಲಸ ನಿಲ್ಲಿಸಿದ್ದೇನೆ. ಬೇರೆ ಏನಾದರೂ ಸಹಾಯ ಬೇಕಾ?",
            phoneGuideIntro = "ನಿಮ್ಮ ಫೋನ್ ಸುಲಭವಾಗಿ ಬಳಸಲು ಹಂತ ಹಂತವಾಗಿ ಹೇಳುತ್ತೇನೆ.",
            screenHelpPrompt = "ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ಇಲ್ಲಿ ಹಾಕಿ, ಸರಳ ಕನ್ನಡದಲ್ಲಿ ವಿವರಿಸುತ್ತೇನೆ.",
            micPermissionNeeded = "ನಿಮ್ಮ ಧ್ವನಿ ಕೇಳಲು ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಬೇಕು."
        )

        "ml-IN" -> LocalizedUiCopy(
            greeting = "നമസ്കാരം! ഞാൻ VaaniX. നിങ്ങളുടെ ഫോൺ എളുപ്പത്തിൽ ഉപയോഗിക്കാൻ ഞാൻ സഹായിക്കാം. താഴെയുള്ള മൈക്ക് അമർത്തി സംസാരിക്കൂ.",
            tapToSpeak = "സംസാരിക്കാൻ അമർത്തുക",
            listeningNow = "കേൾക്കുന്നു... സാവധാനം സംസാരിക്കൂ",
            thinkingNow = "മനസ്സിലാക്കുന്നു...",
            speakingNow = "പറയുന്നു...",
            stopLabel = "നിർത്തുക",
            readyHint = "WhatsApp, YouTube, ശബ്ദം, ഫോൺ സെറ്റിംഗ്സ് എന്നിവ ചോദിക്കാം.",
            samplePrompts = listOf(
                "WhatsApp തുറക്കൂ",
                "YouTube ൽ പാട്ടുകൾ തിരയൂ",
                "ഫോൺ ഉപയോഗിക്കാൻ പഠിപ്പിക്കൂ",
                "ശബ്ദം കൂട്ടൂ"
            ),
            askConfirmationPrefix = "ദയവായി ഉറപ്പാക്കുക: ",
            cancelledMessage = "ശരി, അത് നിർത്തി. മറ്റെന്തെങ്കിലും സഹായം വേണോ?",
            phoneGuideIntro = "ഫോൺ എളുപ്പത്തിൽ ഉപയോഗിക്കാൻ ഞാൻ ഓരോ ഘട്ടമായി പറയാം.",
            screenHelpPrompt = "സന്ദേശം ഇവിടെ നൽകൂ, ലളിതമായ മലയാളത്തിൽ വിശദീകരിക്കാം.",
            micPermissionNeeded = "നിങ്ങളുടെ ശബ്ദം കേൾക്കാൻ മൈക്ക് അനുമതി ആവശ്യമാണ്."
        )

        "mr-IN" -> LocalizedUiCopy(
            greeting = "नमस्कार! मी VaaniX आहे. तुमचा फोन वापरण्यात मी मदत करेन. खालील माइक दाबून आरामात बोला.",
            tapToSpeak = "बोलण्यासाठी दाबा",
            listeningNow = "ऐकत आहे... आरामात बोला",
            thinkingNow = "समजून घेत आहे...",
            speakingNow = "सांगत आहे...",
            stopLabel = "थांबवा",
            readyHint = "WhatsApp, YouTube, आवाज वाढवणे किंवा फोन शिकण्यासाठी बोला.",
            samplePrompts = listOf(
                "WhatsApp उघडा",
                "YouTube वर गाणी शोधा",
                "मला फोन वापरायला शिकवा",
                "आवाज वाढवा"
            ),
            askConfirmationPrefix = "कृपया खात्री करा: ",
            cancelledMessage = "ठीक आहे, ते काम थांबवले आहे. अजून काही मदत हवी का?",
            phoneGuideIntro = "मी तुम्हाला सोप्या मराठीत फोन वापरायला शिकवेन.",
            screenHelpPrompt = "तुमचा मेसेज येथे पेस्ट करा, मी सोप्या मराठीत समजावून सांगेन.",
            micPermissionNeeded = "तुमचा आवाज ऐकण्यासाठी माइकची परवानगी आवश्यक आहे."
        )

        "bn-IN" -> LocalizedUiCopy(
            greeting = "নমস্কার! আমি VaaniX। আপনার ফোন ব্যবহার করতে আমি সাহায্য করব। নিচের মাইক টিপে ধীরে বলুন।",
            tapToSpeak = "বলতে এখানে চাপুন",
            listeningNow = "শুনছি... ধীরে বলুন",
            thinkingNow = "বুঝছি...",
            speakingNow = "বলছি...",
            stopLabel = "থামান",
            readyHint = "WhatsApp, YouTube, সাউন্ড বা ফোনের সেটিংস সম্পর্কে বলুন।",
            samplePrompts = listOf(
                "WhatsApp খুলুন",
                "YouTube এ গান খুঁজুন",
                "আমাকে ফোন চালানো শেখান",
                "আওয়াজ বাড়ান"
            ),
            askConfirmationPrefix = "অনুগ্রহ করে নিশ্চিত করুন: ",
            cancelledMessage = "ঠিক আছে, কাজটি বাতিল করা হলো। আর কোনো সাহায্য লাগবে?",
            phoneGuideIntro = "সহজ বাংলায় ধাপে ধাপে ফোন ব্যবহার শেখাব।",
            screenHelpPrompt = "মেসেজটি এখানে দিন, আমি সহজ বাংলায় বুঝিয়ে দেব।",
            micPermissionNeeded = "আপনার কথা শোনার জন্য মাইক্রোফোন অনুমতি প্রয়োজন।"
        )

        "gu-IN" -> LocalizedUiCopy(
            greeting = "નમસ્તે! હું VaaniX છું. તમારો ફોન વાપરવામાં હું મદદ કરીશ. નીચેનું માઈક દબાવીને આરામથી બોલો.",
            tapToSpeak = "બોલવા માટે દબાવો",
            listeningNow = "સાંભળું છું... આરામથી બોલો",
            thinkingNow = "સમજું છું...",
            speakingNow = "બોલું છું...",
            stopLabel = "રોકો",
            readyHint = "WhatsApp, YouTube, અવાજ અથવા ફોન સેટિંગ્સ વિશે પૂછો.",
            samplePrompts = listOf(
                "WhatsApp ખોલો",
                "YouTube પર ભજન શોધો",
                "મને ફોન વાપરતા શીખવો",
                "અવાજ વધારો"
            ),
            askConfirmationPrefix = "કૃપા કરીને ખાતરી કરો: ",
            cancelledMessage = "બરાબર, આ કામ રદ કર્યું છે. બીજી કોઈ મદદ જોઈએ છે?",
            phoneGuideIntro = "હું તમને સરળ ગુજરાતીમાં ફોન વાપરતા શીખવીશ.",
            screenHelpPrompt = "તમારો મેસેજ અહીં મૂકો, હું સરળ ગુજરાતીમાં સમજાવીશ.",
            micPermissionNeeded = "તમારો અવાજ સાંભળવા માટે માઈકની પરવાનગી જરૂરી છે."
        )

        "pa-IN" -> LocalizedUiCopy(
            greeting = "ਸਤ ਸ੍ਰੀ ਅਕਾਲ! ਮੈਂ VaaniX ਹਾਂ। ਮੈਂ ਤੁਹਾਡਾ ਫ਼ੋਨ ਚਲਾਉਣ ਵਿੱਚ ਮਦਦ ਕਰਾਂਗਾ। ਹੇਠਾਂ ਮਾਈਕ ਦਬਾ ਕੇ ਆਰਾਮ ਨਾਲ ਬੋਲੋ।",
            tapToSpeak = "ਬੋਲਣ ਲਈ ਦਬਾਓ",
            listeningNow = "ਸੁਣ ਰਿਹਾ ਹਾਂ... ਆਰਾਮ ਨਾਲ ਬੋਲੋ",
            thinkingNow = "ਸਮਝ ਰਿਹਾ ਹਾਂ...",
            speakingNow = "ਬੋਲ ਰਿਹਾ ਹਾਂ...",
            stopLabel = "ਰੋਕੋ",
            readyHint = "WhatsApp, YouTube, ਆਵਾਜ਼ ਜਾਂ ਫ਼ੋਨ ਸੈਟਿੰਗਾਂ ਬਾਰੇ ਪੁੱਛੋ।",
            samplePrompts = listOf(
                "WhatsApp ਖੋਲ੍ਹੋ",
                "YouTube ਤੇ ਗੀਤ ਲੱਭੋ",
                "ਮੈਨੂੰ ਫ਼ੋਨ ਚਲਾਉਣਾ ਸਿਖਾਓ",
                "ਆਵਾਜ਼ ਵਧਾਓ"
            ),
            askConfirmationPrefix = "ਕਿਰਪਾ ਕਰਕੇ ਪੱਕਾ ਕਰੋ: ",
            cancelledMessage = "ਠੀਕ ਹੈ, ਇਹ ਕੰਮ ਰੋਕ ਦਿੱਤਾ ਹੈ। ਕੀ ਹੋਰ ਕੋਈ ਮਦਦ ਚਾਹੀਦੀ ਹੈ?",
            phoneGuideIntro = "ਮੈਂ ਤੁਹਾਨੂੰ ਸੌਖੀ ਪੰਜਾਬੀ ਵਿੱਚ ਫ਼ੋਨ ਚਲਾਉਣਾ ਸਿਖਾਵਾਂਗਾ।",
            screenHelpPrompt = "ਆਪਣਾ ਸੁਨੇਹਾ ਇੱਥੇ ਪਾਓ, ਮੈਂ ਸੌਖੀ ਪੰਜਾਬੀ ਵਿੱਚ ਸਮਝਾਵਾਂਗਾ।",
            micPermissionNeeded = "ਤੁਹਾਡੀ ਆਵਾਜ਼ ਸੁਣਨ ਲਈ ਮਾਈਕ ਦੀ ਇਜਾਜ਼ਤ ਲੋੜੀਂਦੀ ਹੈ।"
        )

        "or-IN" -> LocalizedUiCopy(
            greeting = "ନମସ୍କାର! ମୁଁ VaaniX। ଆପଣଙ୍କ ଫୋନ୍ ଚଳାଇବାରେ ମୁଁ ସାହାଯ୍ୟ କରିବି। ତଳେ ଥିବା ମାଇକ୍ ଦବାଇ ଧୀରେ କୁହନ୍ତୁ।",
            tapToSpeak = "କହିବା ପାଇଁ ଦବାନ୍ତୁ",
            listeningNow = "ଶୁଣୁଛି... ଧୀରେ କୁହନ୍ତୁ",
            thinkingNow = "ବୁଝୁଛି...",
            speakingNow = "କହୁଛି...",
            stopLabel = "ରୋକନ୍ତୁ",
            readyHint = "WhatsApp, YouTube, ଶବ୍ଦ କିମ୍ବା ଫୋନ୍ ସେଟିଂସ୍ ବିଷୟରେ ପଚାରନ୍ତୁ।",
            samplePrompts = listOf(
                "WhatsApp ଖୋଲନ୍ତୁ",
                "YouTube ରେ ଗୀତ ଖୋଜନ୍ତୁ",
                "ମୋତେ ଫୋନ୍ ଚଳାଇବା ଶିଖାନ୍ତୁ",
                "ଶବ୍ଦ ବଢ଼ାନ୍ତୁ"
            ),
            askConfirmationPrefix = "ଦୟାକରି ନିଶ୍ଚିତ କରନ୍ତୁ: ",
            cancelledMessage = "ଠିକ୍ ଅଛି, ସେହି କାମ ବନ୍ଦ କରାଗଲା। ଆଉ କିଛି ସାହାଯ୍ୟ ଦରକାର କି?",
            phoneGuideIntro = "ସହଜ ଓଡ଼ିଆରେ ମୁଁ ଆପଣଙ୍କୁ ଫୋନ୍ ବ୍ୟବହାର ଶିଖାଇବି।",
            screenHelpPrompt = "ଆପଣଙ୍କ ମେସେଜ୍ ଏଠାରେ ରଖନ୍ତୁ, ମୁଁ ସରଳ ଓଡ଼ିଆରେ ବୁଝାଇବି।",
            micPermissionNeeded = "ଆପଣଙ୍କ କଥା ଶୁଣିବା ପାଇଁ ମାଇକ୍ରୋଫୋନ୍ ଅନୁମତି ଦରକାର।"
        )

        "as-IN" -> LocalizedUiCopy(
            greeting = "নমস্কাৰ! মই VaaniX। আপোনাৰ ফোন ব্যৱহাৰ কৰাত মই সহায় কৰিম। তলৰ মাইক টিপি লাহে লাহে কওক।",
            tapToSpeak = "ক’বলৈ টিপক",
            listeningNow = "শুনি আছোঁ... লাহে লাহে কওক",
            thinkingNow = "বুজি আছোঁ...",
            speakingNow = "কৈ আছোঁ...",
            stopLabel = "ৰখোৱা",
            readyHint = "WhatsApp, YouTube, শব্দ বা ফোন ছেটিংছৰ বিষয়ে সুধিব পাৰে।",
            samplePrompts = listOf(
                "WhatsApp খোলক",
                "YouTube ত গান বিচাৰক",
                "মোক ফোন চলাবলৈ শিকাওক",
                "শব্দ বঢ়াওক"
            ),
            askConfirmationPrefix = "অনুগ্ৰহ কৰি নিশ্চিত কৰক: ",
            cancelledMessage = "ঠিক আছে, কামটো বন্ধ কৰিলোঁ। আন কিবা সহায় লাগিব নেকি?",
            phoneGuideIntro = "সহজ অসমীয়াত মই আপোনাক ফোন চলোৱা শিকাম।",
            screenHelpPrompt = "আপোনাৰ বাৰ্তা ইয়াত দিয়ক, মই সহজ অসমীয়াত বুজাই দিম।",
            micPermissionNeeded = "আপোনাৰ মাত শুনিবলৈ মাইক্ৰ’ফোনৰ অনুমতি লাগে।"
        )

        "ur-IN" -> LocalizedUiCopy(
            greeting = "السلام علیکم! میں VaaniX ہوں۔ میں آپ کا فون استعمال کرنے میں مدد کروں گا۔ نیچے مائیک دبا کر آرام سے بولیں۔",
            tapToSpeak = "بولنے کے لیے دبائیں",
            listeningNow = "سن رہا ہوں... آرام سے بولیں",
            thinkingNow = "سمجھ رہا ہوں...",
            speakingNow = "بول رہا ہوں...",
            stopLabel = "روکیں",
            readyHint = "WhatsApp، YouTube، آواز یا فون سیٹنگز کے بارے میں پوچھیں۔",
            samplePrompts = listOf(
                "WhatsApp کھولیں",
                "YouTube پر گانے تلاش کریں",
                "مجھے فون چلانا سکھائیں",
                "آواز بڑھائیں"
            ),
            askConfirmationPrefix = "براہ کرم تصدیق کریں: ",
            cancelledMessage = "ٹھیک ہے، یہ کام روک دیا گیا ہے۔ کیا مزید کوئی مدد چاہیے؟",
            phoneGuideIntro = "میں آسان زبان میں فون استعمال کرنا سکھاؤں گا۔",
            screenHelpPrompt = "اپنا پیغام یہاں پیسٹ کریں، میں آسان اردو میں سمجھاؤں گا۔",
            micPermissionNeeded = "آپ کی آواز سننے کے لیے مائیک کی اجازت درکار ہے۔"
        )

        else -> LocalizedUiCopy(
            greeting = "Namaste! I am VaaniX. I help you use your smartphone easily. Tap the big microphone button below and speak naturally.",
            tapToSpeak = "Tap to Speak",
            listeningNow = "Listening... please speak calmly",
            thinkingNow = "Understanding your request...",
            speakingNow = "Speaking...",
            stopLabel = "Stop",
            readyHint = "Try saying: Open WhatsApp, Search devotional songs on YouTube, or Teach me my phone.",
            samplePrompts = listOf(
                "Open WhatsApp",
                "YouTube open karke songs search karo",
                "Naa kodukuki message pampinchu",
                "Mujhe phone use karna sikhao"
            ),
            askConfirmationPrefix = "Please confirm before I continue: ",
            cancelledMessage = "Okay, I cancelled that action. Let me know if you need anything else.",
            phoneGuideIntro = "I can guide you step-by-step in simple words.",
            screenHelpPrompt = "Paste or share any message or screen text below and I will explain it simply.",
            micPermissionNeeded = "VaaniX needs microphone permission only when you press Speak."
        )
    }
}
