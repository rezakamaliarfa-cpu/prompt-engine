package com.example.data

import com.example.api.GenerateContentRequest
import com.example.api.Content
import com.example.api.Part
import com.example.api.GenerationConfig
import com.example.api.GeminiApiClient
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PromptRepository(
    private val historyDao: PromptHistoryDao,
    private val instructionDao: CustomInstructionDao,
    private val securityLogDao: SecurityLogDao
) {
    // --- DB Flow Queries ---
    val allHistory: Flow<List<PromptHistory>> = historyDao.getAllHistory()
    val allInstructions: Flow<List<CustomInstruction>> = instructionDao.getAllInstructions()
    val allSecurityLogs: Flow<List<SecurityLog>> = securityLogDao.getAllLogs()

    // --- Database Operations ---
    suspend fun saveHistory(history: PromptHistory) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(history)
        logSecurityAction("ثبت تاریخچه", "پرامپت بهینه‌سازی شده با شناسه ${history.category} در پایگاه داده رمزنگاری‌شده ذخیره شد.")
    }

    suspend fun deleteHistory(id: Int) = withContext(Dispatchers.IO) {
        historyDao.deleteHistory(id)
        logSecurityAction("حذف تاریخچه", "رکورد تاریخچه با شناسه $id به طور کامل و ایمن از حافظه حذف شد.")
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
        logSecurityAction("پاکسازی کامل تاریخچه", "تمامی رکوردهای مربوط به تاریخچه پرامپت‌ها به طور کامل امحا شدند.")
    }

    suspend fun saveCustomInstruction(instruction: CustomInstruction) = withContext(Dispatchers.IO) {
        instructionDao.insertInstruction(instruction)
        logSecurityAction("ثبت دستور شخصی", "دستور شخصی‌سازی شده با نام '${instruction.name}' در محیط ایزوله ذخیره شد.")
    }

    suspend fun deleteCustomInstruction(id: Int) = withContext(Dispatchers.IO) {
        instructionDao.deleteInstruction(id)
        logSecurityAction("حذف دستور شخصی", "تنظیمات دستور شخصی با شناسه $id به طور کامل حذف شد.")
    }

    suspend fun logSecurityAction(action: String, details: String) = withContext(Dispatchers.IO) {
        val log = SecurityLog(
            action = action,
            details = details,
            timestamp = System.currentTimeMillis(),
            encryptionType = "AES-256-GCM (محافظت‌شده)"
        )
        securityLogDao.insertLog(log)
    }

    suspend fun clearSecurityLogs() = withContext(Dispatchers.IO) {
        securityLogDao.clearLogs()
    }

    // --- Gemini Online Optimization ---
    suspend fun optimizeWithGemini(
        category: String,
        inputPrompt: String,
        answers: Map<String, String>,
        tone: String,
        agentPersona: String,
        language: String, // "farsi" or "english"
        customInstructionText: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            logSecurityAction("خطای کلید API", "تلاش برای استفاده از Gemini بدون تنظیم کلید API معتبر.")
            return@withContext Result.failure(Exception("کلید API هوش مصنوعی در بخش تنظیمات/Secrets تعریف نشده است. لطفاً کلید معتبر را در پنل Secrets وارد کنید یا از حالت آفلاین استفاده نمایید."))
        }

        val formattedAnswers = answers.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }
        val categoryFarsi = getCategoryNameFarsi(category)

        val targetLangInstruction = if (language == "farsi") {
            "خروجی نهایی پرامپت باید کاملاً به زبان **فارسی** روان و خوانا باشد. تمام بخش‌های بهینه‌سازی نیز به فارسی نوشته شود."
        } else {
            "The optimized prompt inside the codeblock MUST be strictly in **English**. The explanations and structural details can be in Persian or English, but the prompt itself inside the copy block must be ready for English AI engines."
        }

        val systemInstruction = """
            شما یک مهندس ارشد، طراح خلاقیت طراز اول و معمار حوزه پرامپت (Prompt Architect) هستید. وظیفه شما بهینه‌سازی پرامپت‌های کوتاه و ساده ورودی کاربر و تبدیل آن‌ها به مگا-پرامپت‌های فوق‌حرفه‌ای، مهندسی‌شده، بسیار تفصیلی، دقیق و دارای عمق توصیفی فوق‌العاده بالا (پرامپت‌های بال‌ودار) است.
            
            دسته بندی فعلی پرامپت: $categoryFarsi
            نقش یا ایجنت انتخاب شده: $agentPersona
            لحن خروجی پاسخ خلاقانه: $tone
            $targetLangInstruction
            
            ${customInstructionText?.let { "دستورات شخصی اضافه برای سفارشی‌سازی: $it" } ?: ""}
            
            ⚠️ **قانون طلایی بال و پر دادن به پرامپت (Rich Enrichment & Storyboard Rule)**:
            خروجی شما به هیچ وجه نباید خلاصه، کوتاه یا ساده باشد. شما باید به پرامپت کاربر بالاترین میزان جزئیات و توصیفات خلاقانه را بدهید تا ایده اولیه زنده و ملموس شود. این قانون برای تمامی دسته‌ها بدون استثنا باید رعایت شود:
            1. **فضاسازی و اتمسفر اتمسفریک صحنه (Scene Atmosphere & Setting)**: توصیف بسیار دقیق و زنده از حس و حال محیط، متریال‌ها، نورپردازی سینمایی (مانند نور رندبراند، اشعه‌های حجمی نئونی یا گرگ و میش)، پالت رنگی هماهنگ، عمق میدان دوربین (Depth of Field) و توازن بصری.
            2. **توالی فریم‌ها و ریتم جریان (Sequential Frames / Storyboards / Pipeline)**: تفکیک گام‌به‌گام یا فریم‌به‌فریم دقیق (به صورت فریم ۱، فریم ۲، فریم ۳...) یا تعیین مراحل پیاده‌سازی متوالی با ذکر زاویه و حرکت دوربین (Zoom-in, Slow Pan, Dolly)، دکوپاژ سینمایی دقیق و افکت‌های صوتی یا ترانزیشن‌های هر فریم.
            3. **کالبدشکافی مسائل فنی و کاربردی (Technical Architecture & Core Specs)**: تشریح تمام مسائل فنی مربوطه، ساختار دیتابیس بومی، متغیرها، پلتفرم هدف، توصیف گاردریل‌های رفتاری و مرزهای ممنوعه برای ایجنت، سیستم خطاگیری و متدهای بهینه‌سازی به صورت کاملاً جامع.
            4. **افکت‌ها و صداگذاری جانبی**: در بخش‌های تصویر و ویدیو صراحتاً افکت‌های صوتی (SFX) پیرامون یا موسیقی هماهنگ ثبت شود.
            
            لطفاً خروجی بهینه‌سازی شده را کاملاً تمیز و ساختاریافته ارائه دهید:
            1. **پرامپت نهایی بهینه‌سازی شده**: داخل یک بلوک کد (Code Block) با دکمه کپی آسان قرار گیرد تا کاربر بتواند مستقیماً از آن استفاده کند. این مگا-پرامپت باید تمام جزییات، اتمسفر صحنه، توالی فریم‌ها/مراحل گام‌به‌گام و محدودیت‌های ذکر شده را پوشش دهد.
            
            ⚠️ **قانون حیاتی**: به هیچ وجه خارج از بلوک کد پرامپت، هیچ بخش دیگری مانند تحلیل کیفیت پرامپت، توضیحات مهندسی بهینه‌سازی، تفکیک تحلیل یا پارامترهای موتور طراح اضافه نکنید. پاسخ فقط و فقط باید جزییات بهینه‌سازی شده را در همان بلوک کد نگه دارد و هیچ متنی در آخر به عنوان تحلیل کیفیت یا بررسی کیفی اضافه نشود. تمامی خروجی‌ها باید درون کادر کد block باشد.
        """.trimIndent()

        val promptToSend = """
            پرامپت اولیه کاربر: $inputPrompt
            
            پاسخ‌های کاربر به سوالات بهینه‌سازی:
            $formattedAnswers
        """.trimIndent()

        try {
            logSecurityAction("درخواست بهینه‌سازی هوشمند آنلاین", "ارسال درخواست بهینه‌سازی زنده با قانون غنی‌سازی عمیق به مدل Gemini-3.5-Flash برای دسته $categoryFarsi.")
            
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = promptToSend)))),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
                generationConfig = GenerationConfig(temperature = 0.5f)
            )

            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "پاسخی دریافت نشد."
            
            logSecurityAction("پاسخ موفق هوش مصنوعی", "بهینه‌سازی پرامپت برای دسته $categoryFarsi با موفقیت انجام شد.")
            Result.success(textResult)
        } catch (e: Exception) {
            logSecurityAction("خطای ارتباطی هوش مصنوعی", "خطا در ارتباط با API: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    // --- On-Device Offline Optimization Engine ---
    fun optimizeOffline(
        category: String,
        inputPrompt: String,
        answers: Map<String, String>,
        tone: String,
        agentPersona: String,
        language: String,
        customInstructionText: String? = null
    ): String {
        // --- Smart Category Override to Avoid Irrelevant Prompt Generation ---
        var activeCategory = category.lowercase(Locale.ROOT)
        val lowerText = inputPrompt.lowercase(Locale.ROOT)
        
        val codingKeywords = listOf("کد", "برنامه", "پایتون", "جاوا", "کاتلین", "وب", "سایت", "فرانت", "بک‌اند", "c++", "c#", "code", "python", "javascript", "kotlin", "html", "css", "database", "api", "function", "class", "کلاس", "متد", "توسعه دهنده")
        val technicalKeywords = listOf("محاسبه", "فرمول", "مهندسی", "معادله", "آنالیز", "عمران", "برق", "شبیه‌سازی", "ریاضی", "فیزیک", "تحلیل مکانیک", "سنسور", "مدار")
        val videoKeywords = listOf("ویدیو", "فیلم", "کلیپ", "ریلز", "استوری", "تیزر", "سناریو", "دکوپاژ", "تدوین", "سینمایی")
        val bannerKeywords = listOf("بنر", "پوستر", "تبلیغات", "تبلیغ", "banner", "poster", "ads")
        val imageKeywords = listOf("عکس", "تصویر", "نقاشی", "ایکون", "رندر", "لوگو", "کاور", "اینستا", "طراحی", "image", "photo", "drawing", "illustration", "logo", "icon", "render", "landscape")
        val contentKeywords = listOf("محتوا", "مقاله", "وبلاگ", "کپشن", "لینکدین", "سئو", "متن", "نویسندگی", "پست")
        val assistantKeywords = listOf("وکیل", "مربی", "دستیار", "پزشک", "مشاور", "روانشناس", "معلم", "استاد", "assistant", "coach", "lawyer", "doctor", "therapist", "tutor")

        // Prioritize specific keywords to correct any mismatch
        when {
            codingKeywords.any { lowerText.contains(it) } -> activeCategory = "coding"
            technicalKeywords.any { lowerText.contains(it) } -> activeCategory = "technical"
            videoKeywords.any { lowerText.contains(it) } -> activeCategory = "video"
            bannerKeywords.any { lowerText.contains(it) } -> activeCategory = "banner"
            imageKeywords.any { lowerText.contains(it) } -> activeCategory = "image"
            contentKeywords.any { lowerText.contains(it) } -> activeCategory = "content"
            assistantKeywords.any { lowerText.contains(it) } -> activeCategory = "assistant"
        }
        if (activeCategory.isEmpty()) {
            activeCategory = category
        }

        val categoryFarsi = getCategoryNameFarsi(activeCategory)
        val formattedAnswers = answers.entries.joinToString("\n") { "  * ${it.key}: ${it.value}" }
        val isFarsiOutput = language == "farsi"

        // --- Smart Platform & Canvas Size Auto-Detection ---
        var detectedPlatformFarsi = ""
        var detectedResolutionFarsi = ""
        var detectedAspectRatioFarsi = ""
        var detectedLayoutGuidelineFarsi = ""

        var detectedPlatformEng = ""
        var detectedResolutionEng = ""
        var detectedAspectRatioEng = ""
        var detectedLayoutGuidelineEng = ""

        if (lowerText.contains("ریلز") || lowerText.contains("reels") || lowerText.contains("reel") ||
            lowerText.contains("استوری") || lowerText.contains("story") || lowerText.contains("شورت") ||
            lowerText.contains("shorts") || lowerText.contains("تیک") || lowerText.contains("tiktok")) {
            
            detectedPlatformFarsi = "اینستاگرام ریلز / استوری یا تیک‌تاک (ویدیو یا المان عمودی)"
            detectedResolutionFarsi = "۱۰۸۰ در ۱۹۲۰ پیکسل (Full HD عمودی)"
            detectedAspectRatioFarsi = "9:16 عمودی"
            detectedLayoutGuidelineFarsi = "تمامی عناوین حیاتی، چهره کاراکترها و زیرنویس‌ها را در بازه محدود امن وسط (مرکز کادر) تنظیم کنید تا رابط کاربری بومی اپلیکیشن (مانند دکمه لایک، اشتراک یا آواتار پیج در کناره سمت راست و متن کپشن در پایین) آن‌ها را نپوشاند."

            detectedPlatformEng = "Instagram Reels / Story / Web TikTok Vertical"
            detectedResolutionEng = "1080x1920 pixels (Full HD Vertical)"
            detectedAspectRatioEng = "9:16 vertical"
            detectedLayoutGuidelineEng = "Analyze and center all critical actions, texts, and subject faces strictly in the vertical viewport. Keep a 15% safety margin on both top and bottom edges to avoid overlapping with native overlay controls (likes, engagement layout) and user subtitle cards."
            
        } else if (lowerText.contains("کاور") || lowerText.contains("cover") || lowerText.contains("پست") ||
            lowerText.contains("post") || lowerText.contains("اینستا") || lowerText.contains("instagram")) {
            
            detectedPlatformFarsi = "پست فید یا کاور شبکه‌ای اینستاگرام"
            detectedResolutionFarsi = "۱۰۸۰ در ۱۰۸۰ پیکسل (مربعی استاندارد) یا ۱۰۸۰ در ۱۳۵۰ پیکسل (عمودی پرتره)"
            detectedAspectRatioFarsi = "1:1 یا 4:5"
            detectedLayoutGuidelineFarsi = "سوژه اصلی به طور دقیق در کانون مرکزی قرار گیرد. تیترها را خوانا و با کنتراست رنگی بسیار قوی طراحی کنید تا حتی هنگام نمایش به صورت تامبنیل کوچک در زبانه اکسپلور یا نمای پروفایل (Grid View 1:1) جلب توجه کند."

            detectedPlatformEng = "Instagram Grid Feed Post / Channel Cover Art"
            detectedResolutionEng = "1080x1080 pixels (Standard Square) or 1080x1350 pixels (Vertical Feed)"
            detectedAspectRatioEng = "1:1 or 4:5 Aspect Ratio"
            detectedLayoutGuidelineEng = "Force subjects to sit in the exact center safe box. Utilize punchy colors with high contrast values with the background to maximize click rates when the graphic renders as a microscopic thumbnail in the Explore grid layout."

        } else if (lowerText.contains("یوتیوب") || lowerText.contains("youtube") || lowerText.contains("بنر سایت") || lowerText.contains("landscape")) {
            
            detectedPlatformFarsi = "ویدیو افقی یوتیوب / هدر وبسایت دسکتاپ لنداسکیپ"
            detectedResolutionFarsi = "۱۹۲۰ در ۱۰۸۰ پیکسل (رزولوشن 1080p عریض)"
            detectedAspectRatioFarsi = "16:9 عریض افقی"
            detectedLayoutGuidelineFarsi = "رعایت ترکیب‌بندی خطوط طلایی یک‌سوم (Rule of Thirds). فضاهای خالی جانبی (Negative Space) کافی برای قرار گرفتن تایتل‌ها ایجاد کنید و سوژه اصلی را در سمت مخالف کادر متوازن سازید."

            detectedPlatformEng = "YouTube Desktop Widescreen / Website Hero Banner Landscape"
            detectedResolutionEng = "1920x1080 px (1080p High-Resolution)"
            detectedAspectRatioEng = "16:9 Widescreen Landscape"
            detectedLayoutGuidelineEng = "Distribute geometric visual weights evenly over the grid lines. Consistently dedicate one half of the layout as clear negative space to integrate titles safely without creating sensory clutter."
        }

        val promptBuilder = StringBuilder()
        promptBuilder.append("```markdown\n")

        if (isFarsiOutput) {
            promptBuilder.append("# 🧠 مگا-پرامپت تخصصی مهندسی شده هوشمند (نسخه محلی ارتقایافته)\n\n")
            
            promptBuilder.append("## 👤 مشخصات هویت و آرکتایپ ایجنت (Elite Agent Persona)\n")
            promptBuilder.append("- **نقش تخصصی**: ایجنت ارشد و نخبه با هویت متمایز [$agentPersona]\n")
            promptBuilder.append("- **لحن و لحن کلام**: $tone\n")
            promptBuilder.append("- **رویکرد تحلیلی**: عمل‌گرا، دقیق، خلاق و متمرکز بر حل قطعی مسئله بهینه با رویکرد بدون خطا\n\n")

            promptBuilder.append("## 🎯 مأموریت و هدف نهایی (Primary Objective & Task)\n")
            promptBuilder.append("هدف اصلی شما تجزیه و تحلیل و پاسخ به مسئله یا ایده زیر با توجه به نقش تخصصی تعریف شده است:\n")
            promptBuilder.append("> $inputPrompt\n\n")

            if (detectedPlatformFarsi.isNotEmpty()) {
                promptBuilder.append("## 📐 مشخصات ابعاد و فرمت بومی خودکار (Smart Detected Platform Parameters)\n")
                promptBuilder.append("- **پلتفرم هدف**: $detectedPlatformFarsi\n")
                promptBuilder.append("- **ابعاد کانواس مصوب**: $detectedResolutionFarsi\n")
                promptBuilder.append("- **نسبت تصویر استاندارد (Aspect Ratio)**: $detectedAspectRatioFarsi\n")
                promptBuilder.append("- **توصیه ساختاری چیدمان**: $detectedLayoutGuidelineFarsi\n\n")
            }

            if (answers.isNotEmpty()) {
                promptBuilder.append("## 📋 لایه اطلاعات تکمیلی و پارامترها (Context & Injected Inputs)\n")
                promptBuilder.append("برای ارائه جامع‌ترین پاسخ، متغیرهای محیطی زیر را دقیقاً مبنای فرآیند فکری خود قرار دهید:\n")
                promptBuilder.append("$formattedAnswers\n\n")
            }

            if (!customInstructionText.isNullOrEmpty()) {
                promptBuilder.append("## ⚙️ دستورالعمل‌های الحاقی سفارشی (Injected System Instructions)\n")
                promptBuilder.append("- $customInstructionText\n\n")
            }

            // Category-Specific Advanced Structural Modules with extreme details (Wings Rule)
            promptBuilder.append("## 🔍 قوانین تخصصی بخش ($categoryFarsi) - فضاسازی، تبیین فریم‌ها و پاسخ جامع\n")
            when (activeCategory) {
                "coding" -> {
                    promptBuilder.append("- **اصول معماری**: کدهای خروجی باید کاملاً منطبق بر اصول شی‌گرایی، SOLID، معماری تمیز و خوانایی بالا (Clean Code) باشند.\n")
                    promptBuilder.append("- **توالی فریم‌ها و صفحات برنامه (Screen & UI Frames)**: طراح باید ساختار بصری رابط کاربری را به صورت توالی فریم‌های متمایز (مانند فریم ورود، فریم داشبورد، فریم لودینگ و فریم خطا) تفکیک کرده و هندلینگ ورودی‌ها را در هر فریم دقیقاً توضیح دهد.\n")
                    promptBuilder.append("- **اتمسفر ظاهری و تم طراحی (UI Atmosphere)**: تم رنگی تیره یا روشن بومی، فواصل استاندارد بر اساس پیکسل، حاشیه‌ها و آیکون‌های متناسب با پلتفرم برای هر فریم یا نما تشریح شود.\n")
                    promptBuilder.append("- **مدیریت خطا و پایپلاین فنی**: تضمین کنید که تک‌تک بخش‌های کد دارای مکانیزم‌های بومی پیشگیری و رسیدگی روان به خطاهای احتمالی (Error Handling & Try-Catch) هستند.\n")
                    promptBuilder.append("- **عملکرد و تست‌پذیری**: کدهایی ارائه دهید که از نظر مصرف حافظه و پیچیدگی زمانی کاملاً بهینه بوده و ساختار آن ماژولار و قابل نوشتن تست‌های واحد (Unit Testing) باشد.\n")
                }
                "technical" -> {
                    promptBuilder.append("- **صحت ریاضیاتی و فرمولی**: محاسبات باید چندلایه و فرآیند استخراج نتایج بر اساس فرمول‌های استاندارد بررسی شده و دقیق باشد.\n")
                    promptBuilder.append("- **توالی گام‌های فرآیند (Process Sequence Frames)**: جریان فرآیندی فنی را به صورت بخش‌های توالی منظم (فریم‌های فنی ۱، ۲، ۳...) و فلوچارت‌های تحلیلی مکتوب توصیف کنید.\n")
                    promptBuilder.append("- **فضاسازی عملکردی و بوم‌شناسی سیستم (System Atmosphere & Environmental Co-efficients)**: کلیه ضرایب خطا، فاکتورهای ریسک، اورلود فیزیکی یا سخت‌افزاری و راهکارهای غلبه بر بدترین حالات خرابی دقیقاً بررسی شود.\n")
                }
                "image" -> {
                    promptBuilder.append("- **فضاسازی زنده و اتمسفر فوق‌تفصیلی (Atmosphere & Scene Setting)**: توصیف بی‌نهایت دقیق اتمسفر، نورپردازی‌های سینمایی حرفه‌ای (مانند نور Rembrandt، گرگ‌ومیش طلایی، پرتوهای حجمی، بازتاب ذرات معلق در هوا)، متریال‌ها، حس و حال محیطی و پالت رنگی غنی.\n")
                    promptBuilder.append("- **شات‌ها و فریم‌های کلیدی (Key Frames & Compostion)**: توصیف دقیق کادر بصری، فوکوس عمیق، فاصله کانونی عکاسی (مثل 85mm f/1.4 Lens)، پرسپکتیو پویا (زاویه پایین یا چشم پرنده) و جزئیات چیدمان پس‌زمینه.\n")
                    promptBuilder.append("- **مدل رندر و استایل**: سبک نهایی رندر (مانند Photorealistic, Cinematic Engine v6, 3D Octane Render, Surreal Digital Art) صراحتاً در بدنه توصیف شود.\n")
                    promptBuilder.append("- **نقاط ممنوعه (Negative prompt)**: مشخص کنید که مدل باید از اعضای بدن از ریخت افتاده، متون تار، نویز و تلو تلو خوردن بصری خودداری کند.\n")
                }
                "video" -> {
                    promptBuilder.append("- **دکوپاژ استوری‌بورد و توالی فریم‌های ویدیو (Cinematic Frame Sequences)**: سناریوی خروجی باید به صورت فریم‌های کلیدی متوالی با زمان‌بندی دقیق به ثانیه (مانند فریم ۱: ثانیه ۰ تا ۳، فریم ۲: ثانیه ۳ تا ۸ و...) تفکیک شود.\n")
                    promptBuilder.append("- **اتمسفر سینمایی و فضاسازی محیطی (Scenic Atmosphere)**: توصیف همه‌جانبه فضای صحنه، حس روانشناسی ناشی از محیط، طراحی دکور، اتمسفر نورهای نئونی یا طبیعی و پالت‌های عاطفی صحنه.\n")
                    promptBuilder.append("- **حرکت جنبشی دوربین و بازیگران**: توصیف جزئی حرکات خیره‌کننده دوربین (Slow pan, Zoom-in tracking, Drone cinematic movement, Dolly action) و جابجایی دقیق کاراکترها در کادر.\n")
                    promptBuilder.append("- **فضای صوتی و موسیقی (Audio Atmosphere & SFX)**: تبیین موشکافانه افکت‌های صوتی پیرامونی (SFX)، ضرب‌آهنگ تدوین صوتی با ضرب‌های تصاویر و جریان احساسی موسیقی متن همراه.\n")
                }
                "banner" -> {
                    promptBuilder.append("- **فرمول بازاریابی AIDA**: ساختار متنی بنر باید بر پایه ۴ اصل طلایی: توجه جلب کردن (Attention)، ایجاد علاقه (Interest)، القای تمایل (Desire)، و فراخوان صریح به اقدام (Action) باشد.\n")
                    promptBuilder.append("- **اتمسفر بصری و نقشه رنگ (Design Atmosphere)**: تحلیل روانشناسی پالت رنگی پیشنهادی، تضاد نوری شدید برای خوانایی پیام و جایگذاری تایتل‌ها.\n")
                    promptBuilder.append("- **توالی نگاه مخاطب و فریم‌های بصری (Eye-tracking Pipeline Frames)**: تشریح اینکه چشم مخاطب در هر فریم یا بخش از بنر ابتدا به کجا خیره شود و چگونه به دکمه فراخوان به عمل هدایت گردد.\n")
                }
                "content" -> {
                    promptBuilder.append("- **سئو و کلیدواژه‌های طلایی**: متن باید ساخت یافته شامل تگ‌های سئو مستدل (H1, H2, H3)، متادیسکریپشن جذاب و توزیع مناسب چگالی کلمات کلیدی باشد.\n")
                    promptBuilder.append("- **اتمسفر کلامی و جریان لحن (Tone Atmosphere)**: ایجاد یک اتمسفر کلامی پویا و گیرا بر اساس مخاطبان با قلاب‌های طلایی ذهن‌ربا (Hooks).\n")
                    promptBuilder.append("- **توالی پاراگراف‌ها یا فریم‌های متنی (Message Structure Frames)**: تفکیک فاز به فاز مطلب شامل بخش فرضیه‌ها، جداول مقایسه‌ای، کادرهای نقل قول با فرمت Markdown.\n")
                }
                "assistant" -> {
                    promptBuilder.append("- **اتمسفر هویتی و پرسونای عمیق دستیار (System Persona & Guiding Spirit)**: مشخصات علمی، ویژگی‌های روانشناختی، محدودیت‌ها و آداب رفتاری دستیار به طور کامل پیاده شود.\n")
                    promptBuilder.append("- **سناریوهای تعاملی و فریم‌های مکالمه (Interaction Dialogue Frames)**: حداقل ۳ وظیفه کلیدی یا فلوچارت‌های پاسخی و سناریوهای پله‌پله گفتگو برای پاسخ به کاربر شبیه‌سازی شود.\n")
                    promptBuilder.append("- **سلب مسئولیت و گارد امنیتی (Guardrails)**: تعیین دقیق رفتارهای غیرمجاز یا مواردی که دستیار حتماً باید سلب مسئولیت (Disclaimer) کند.\n")
                }
            }

            promptBuilder.append("\n## 🧠 فرآیند فکری و گام‌به‌گام شناختی (Chain-of-Thought Protocol)\n")
            promptBuilder.append("برای رسیدن به متعالی‌ترین نتیجه، فرآیند زیر را در ذهن خود پیش از صدور پاسخ نهایی طی کنید:\n")
            promptBuilder.append("۱. ابتدا مسئله را به بخش‌های مجزا خرد کنید.\n")
            promptBuilder.append("۲. برای هر بخش، فرضیات خود را آزمایش کرده و ایده‌های جایگزین را مقایسه نمایید.\n")
            promptBuilder.append("۳. در حین تفکر گام‌به‌گام، هرگونه تناقض یا ایراد منطقی را از میان بردارید.\n")
            promptBuilder.append("۴. تنها پاسخ نهایی تایید شده و فوق‌العاده با کیفیت را بنویسید.\n\n")

            promptBuilder.append("## ⛔ مرزهای ممنوعه و خطوط قرمز (Negative Guidelines & Constraints)\n")
            promptBuilder.append("- **ممنوعیت اطلاعات نامعتبر**: به هیچ وجه اطلاعات حدسی، ابهام آمیز یا رفرنس‌های ساختگی صادر نکنید.\n")
            promptBuilder.append("- **ممنوعیت کلمات هرز و تکراری**: از تطویل کلام بی‌مورد و مقدمه‌چینی‌های کلیشه‌ای هوش مصنوعی دوری کنید.\n")
            promptBuilder.append("- **عدم نقض قوانین**: به هیچ وجه قوانین فرمت‌بندی RTL و قوانین ساختاری خروجی بخش مربوطه را نقض نکنید.\n\n")

            promptBuilder.append("## 📊 فرمت خروجی نهایی (Ultimate Blueprint Configuration)\n")
            promptBuilder.append("- پاسخ خود را تماماً به صورت سازمان یافته مجزا، همراه با تیترهای فارسی جذاب ارائه دهید.\n")
            promptBuilder.append("- کدهای برنامه‌نویسی یا سناریوها را در بلوک‌های کد مخصوص (Code Blocks) محصور کنید.\n")
            promptBuilder.append("- پاسخ نهایی با استاندارد دقیق و بهینه صادر شود.\n")
        } else {
            promptBuilder.append("# 🧠 High-Fidelity Engineered Mega-Prompt (Offline Advanced Engine)\n\n")
            
            promptBuilder.append("## 👤 Elite Persona Specs\n")
            promptBuilder.append("- **Professional Identity**: Senior Technical Architect & Elite Practitioner with specialization in [$agentPersona]\n")
            promptBuilder.append("- **Tone & Communication Profile**: $tone\n")
            promptBuilder.append("- **Cognitive Stance**: Pragmatic, highly precise, creative, and strictly focused on comprehensive, error-free output.\n\n")

            promptBuilder.append("## 🎯 Primary Objective\n")
            promptBuilder.append("Analyze and solve the core prompt or task defined below under the elite practitioner archetype rules:\n")
            promptBuilder.append("> $inputPrompt\n\n")

            if (detectedPlatformEng.isNotEmpty()) {
                promptBuilder.append("## 📐 Smart Detected Platform Parameters\n")
                promptBuilder.append("- **Target Platform**: $detectedPlatformEng\n")
                promptBuilder.append("- **Canvas Resolution**: $detectedResolutionEng\n")
                promptBuilder.append("- **Aspect Ratio**: $detectedAspectRatioEng\n")
                promptBuilder.append("- **Layout Guideline**: $detectedLayoutGuidelineEng\n\n")
            }

            if (answers.isNotEmpty()) {
                promptBuilder.append("## 📋 Context & Injected Variables\n")
                promptBuilder.append("For high-fidelity outcome, integrate these variables as rigid operational guidelines:\n")
                answers.entries.forEach { (q, a) ->
                    promptBuilder.append("- **$q**: $a\n")
                }
                promptBuilder.append("\n")
            }

            if (!customInstructionText.isNullOrEmpty()) {
                promptBuilder.append("## ⚙️ Injected Custom Guidelines\n")
                promptBuilder.append("- $customInstructionText\n\n")
            }

            promptBuilder.append("## 🔍 Domain-Specific Optimization Rules\n")
            when (activeCategory) {
                "coding" -> {
                    promptBuilder.append("- **Clean Architecture**: Design the code according to SOLID principles, modular structure, and optimal complexity.\n")
                    promptBuilder.append("- **No Mock Templates**: Implement full operational functions instead of writing comments or incomplete modules.\n")
                    promptBuilder.append("- **Error Handling**: Code must be safe, containing mature try-catch exceptions and secure validation paths.\n")
                    promptBuilder.append("- **Performance Profile**: Minimize execution time and memory footprint (Big-O optimization).\n")
                    promptBuilder.append("- **Zero-dependency Core**: Build self-contained functions where possible for straightforward testing.\n")
                }
                "technical" -> {
                    promptBuilder.append("- **Formulary Precision**: All calculations must satisfy exact modern engineering formulas and validation parameters.\n")
                    promptBuilder.append("- **Failure Mode Analysis**: Highlight worst-case edge scenarios, technical overhead limits, and scaling hazards.\n")
                    promptBuilder.append("- **Diagrammatic Layout**: Describe the operational workflow sequentially with clear textual pipelines.\n")
                }
                "image" -> {
                    promptBuilder.append("- **Advanced Scene Setting**: Incorporate dynamic lighting terms (Rembrandt, volumetric god-rays, warm studio glow) and precise focal lengths (e.g., 85mm f/1.4 cinematic lens, 8k resolution).\n")
                    promptBuilder.append("- **Rendering Specifications**: Request modern rendering engines (e.g., Midjourney v6 photorealistic, Unreal Engine 5 render, Octane visual render).\n")
                    promptBuilder.append("- **Angle & Composition**: Specify highly cinematic camera perspectives and rule-of-thirds compositions.\n")
                    promptBuilder.append("- **Injected Negative Specs**: Keep the output free of duplicate limbs, blurry textures, out-of-frame objects, and visual noise.\n")
                }
                "video" -> {
                    promptBuilder.append("- **Segmented Directing Checklist**: Frame scenarios clearly divided into: Action, Camera Motion, Characters, Visual Atmosphere, and SFX profiles.\n")
                    promptBuilder.append("- **Camera Kinetic Energy**: Use structured movements (Slow pan, dollies, tracking sweeps, aerial cranes).\n")
                    promptBuilder.append("- **Structural Pacing**: Enforce rigid timing blocks or pacing cues to secure maximum user retention.\n")
                }
                "banner" -> {
                    promptBuilder.append("- **AIDA Framework Priority**: Structure the copy to trigger: Attention -> Interest -> Desire -> Action sequence.\n")
                    promptBuilder.append("- **Neuromarketing Hooks**: Apply persuasive trigger words and high-converting hooks that resonate with the demographics.\n")
                    promptBuilder.append("- **Visual Contrast Spec**: Detail color theory palettes, high-contrast text ratios, and call-to-action positions.\n")
                }
                "content" -> {
                    promptBuilder.append("- **Semantics & SEO Compliance**: Ensure SEO headings (H1, H2, H3), proper LSI keywords, and rich metadata descriptions are included.\n")
                    promptBuilder.append("- **Engagement Mechanics**: Build an instant Hook in the intro, and construct interactive checklists, comparative grids and summaries.\n")
                    promptBuilder.append("- **Readability Matrix**: Avoid dense blocks by using tables, formatted callouts, lists, and spacing.\n")
                }
                "assistant" -> {
                    promptBuilder.append("- **High-Fidelity Agent Persona**: Clearly define deep system instructions, professional ethics, boundaries, and personality of the AI Assistant.\n")
                    promptBuilder.append("- **Structured Skill Matrices**: Provide at least 3 concrete execution workflows with exact prompt directives the user can demand.\n")
                    promptBuilder.append("- **Rigid Safety Guardrails**: Enforce strict do-not-comply policies for out-of-scope inquiries and include necessary disclaimer mechanisms.\n")
                }
            }

            promptBuilder.append("\n## 🧠 Chain-of-Thought Cognitive Protocol\n")
            promptBuilder.append("Please internalize these execution steps before emitting the output:\n")
            promptBuilder.append("1. Deconstruct the request into basic structural requirements.\n")
            promptBuilder.append("2. Generate intermediate steps representing your underlying logical choices.\n")
            promptBuilder.append("3. Review draft answers against the negative constraints to eliminate fluff.\n")
            promptBuilder.append("4. Present only the highly refined final answer.\n\n")

            promptBuilder.append("## ⛔ Negative Constraints (Behaviors to Avoid)\n")
            promptBuilder.append("- **Zero Guesswork**: Never fabricate sources, non-existent SDK versions, or speculative data.\n")
            promptBuilder.append("- **No generic templates**: Avoid typical AI prefaces (e.g., 'Sure, I can help you with that', 'Here is your...').\n")
            promptBuilder.append("- **Zero Redundancy**: Focus directly on high information-density content.\n\n")

            promptBuilder.append("## 📊 Formatting & Layout Blueprints\n")
            promptBuilder.append("- Format everything beautifully using clean Markdown conventions.\n")
            promptBuilder.append("- Wrap all code, equations, or configurations inside explicit triple-backtick markdown blocks.\n")
            promptBuilder.append("- Structure answers using bolded focal points and visual separations.\n")
        }

        promptBuilder.append("```\n")

        return promptBuilder.toString()
    }

    // --- Special Mega-Prompt Iran Website Builder Generator ---
    fun generateIranWebBuilderMegaPrompt(
        businessType: String,
        gateway: String,
        smsProvider: String,
        additionalSpecs: String,
        language: String,
        uiToolConnected: Boolean
    ): String {
        val isFarsi = language == "farsi"
        val promptBuilder = StringBuilder()

        promptBuilder.append("```markdown\n")
        if (isFarsi) {
            promptBuilder.append("# پرامپت ویژه مگا-سایت‌ساز حرفه‌ای سازگار با ایران\n\n")
            promptBuilder.append("## نقش سیستم (System Role):\n")
            promptBuilder.append("شما یک توسعه‌دهنده فول‌استک ارشد و متخصص سئو محلی ایران، مسلط به فریمورک‌های مدرن (Next.js/Laravel/FastAPI) و استانداردهای بومی وب فارسی هستید.\n\n")
            
            promptBuilder.append("## تسک اصلی تفصیلی (Task):\n")
            promptBuilder.append("یک معماری فنی کامل، نقشه دیتابیس و کدهای اولیه پروژه برای یک وبسایت [$businessType] طراحی کنید که با ویژگی‌های زیر سازگار باشد:\n\n")
            
            promptBuilder.append("## نیازمندی‌های اختصاصی ایران (Iranian Ecosystem Requirements):\n")
            promptBuilder.append("۱. **درگاه پرداخت**: پیاده‌سازی متدهای وب‌سرویس درگاه شاپرک [$gateway] با مدیریت صحیح تراکنش‌های موفق، ناموفق، بازپرداخت و مغایرت مالی (آدرس‌های Callback و متد Verify تراکنش).\n")
            promptBuilder.append("۲. **احراز هویت پیامکی**: سیستم ثبت‌نام دو مرحله‌ای (OTP) با وب‌سرویس [$smsProvider] شامل ارسال الگو (Pattern Mode) جهت عبور از بلک‌لیست مخابرات در کمتر از ۵ ثانیه.\n")
            promptBuilder.append("۳. **تایپوگرافی و طراحی رابط کاربری (RTL UI)**: استفاده اختصاصی از فونت‌های فارسی استاندارد (مانند وزیرمتن یا شبنم)، فونت فارسی برای اعداد جهت جلوگیری از بهم ریختگی اعداد انگلیسی در فیلدهای موبایل/کد ملی، و طراحی کامپوننت‌های کاملاً راست‌چین با کلاس‌های Tailwind CSS قرینه (مانند md:mr-auto یا pl-4 به صورت متناظر RTL).\n")
            promptBuilder.append("۴. **پایگاه داده و لوکالیزیشن**: پشتیبانی کامل فیلدها از کاراکترست UTF-8 فارسی (مانند ی و ک فارسی متمایز از عربی)، ذخیره‌سازی تاریخ‌ها به قالب میلادی با تبدیل پویا در کد به تقویم هجری شمسی (Jalali Date)، و پیاده‌سازی فرمت صحیح شماره‌های ایران (با شروع 09).\n")
            promptBuilder.append("۵. **مقررات بومی دیجیتال**: سازگار با ملزومات نماد اعتماد الکترونیکی (اینماد) و آیین‌نامه‌های ثبت‌نام در سامانه رسانه‌های دیجیتال (ساماندهی)، شامل صفحات شرایط استفاده، حریم خصوصی کاربران و مستندات امنیتی پایگاه داده جهت اخذ درگاه مستقیم.\n")
            
            if (uiToolConnected) {
                promptBuilder.append("۶. **هماهنگی با ابزار طراحی رابط کاربری**: قالب‌بندی و پالت‌های رنگی بهینه‌سازی شده بر اساس آخرین استانداردهای UI/UX خروجی گرفته شده از Figma.\n")
            }
            if (additionalSpecs.isNotEmpty()) {
                promptBuilder.append("۷. **سایر نیازمندی‌های مشخص شده**: $additionalSpecs\n")
            }
        } else {
            promptBuilder.append("# Iran-Compatible Fully Structured Mega Website-Builder Prompt\n\n")
            promptBuilder.append("## Role:\n")
            promptBuilder.append("You are an expert Senior Full-Stack Developer familiar with architectural patterns, local payment systems, and localization rules in the Middle East region.\n\n")
            
            promptBuilder.append("## Objective:\n")
            promptBuilder.append("Design a comprehensive software layout, class structure, and database migrations for an Iranian-localized [$businessType] system satisfying these requirements:\n\n")
            
            promptBuilder.append("## Localization Guidelines:\n")
            promptBuilder.append("1. **Payment Gateway**: Integration layer for the [$gateway] payment service (Shetab/Shaparak framework), handles payment initiation, secure returns, and server-side verification.\n")
            promptBuilder.append("2. **Verification SMS**: Two-Factor OTP authentication via the [$smsProvider] API optimized for instant delivery.\n")
            promptBuilder.append("3. **RTL Dynamic Layouts**: Web typography based on standard Persian fonts (Vazirmatn/Shabnam), with proper right-to-left alignment utilizing Tailwind CSS classes.\n")
            promptBuilder.append("4. **Schema Specifications**: Datetime calculations translated via solar Jalali calendar utilities, UTF-8 unicode encoding, and strict validation of Persian strings.\n")
            promptBuilder.append("5. **E-commerce Compliance**: Adherence to local e-Namad regulations, privacy standards, and secure user data isolation.\n")
            
            if (uiToolConnected) {
                promptBuilder.append("6. **UI/UX Design Spec Sync**: Design palette and typography matching external tool tokens retrieved via secure integration.\n")
            }
        }
        promptBuilder.append("```\n")
        return promptBuilder.toString()
    }

    private fun getCategoryNameFarsi(category: String): String {
        return when(category) {
            "coding" -> "کدنویسی"
            "technical" -> "فنی و مهندسی"
            "image" -> "تولید عکس"
            "video" -> "تولید ویدیو"
            "banner" -> "تولید بنر"
            "content" -> "تولید محتوا"
            "assistant" -> "ساخت دستیار هوشمند"
            else -> category
        }
    }
}
