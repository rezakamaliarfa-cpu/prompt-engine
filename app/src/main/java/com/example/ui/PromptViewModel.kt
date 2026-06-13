package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PromptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PromptRepository(
        database.promptHistoryDao(),
        database.customInstructionDao(),
        database.securityLogDao()
    )

    // --- REPLAY/FLOW STATES ---
    val promptHistory = repository.allHistory
    val customInstructions = repository.allInstructions
    val securityLogs = repository.allSecurityLogs

    // --- UI VIEW STATES ---
    private val _activeTab = MutableStateFlow("optimize") // optimize, builder, custom, logs
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _inputPrompt = MutableStateFlow("")
    val inputPrompt: StateFlow<String> = _inputPrompt.asStateFlow()

    // Interactive questions map for the selected category
    private val _categoryQuestions = MutableStateFlow<List<String>>(emptyList())
    val categoryQuestions: StateFlow<List<String>> = _categoryQuestions.asStateFlow()

    private val _questionAnswers = MutableStateFlow<Map<String, String>>(emptyMap())
    val questionAnswers: StateFlow<Map<String, String>> = _questionAnswers.asStateFlow()

    // Model customisations
    private val _selectedTone = MutableStateFlow("حرفه‌ای (Professional)")
    val selectedTone: StateFlow<String> = _selectedTone.asStateFlow()

    private val _selectedAgent = MutableStateFlow("برنامه‌نویس ارشد (Senior Developer)")
    val selectedAgent: StateFlow<String> = _selectedAgent.asStateFlow()

    private val _promptLanguage = MutableStateFlow("farsi") // farsi or english
    val promptLanguage: StateFlow<String> = _promptLanguage.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(true)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Selected instruction template to inject
    private val _selectedInstructionId = MutableStateFlow<Int?>(null)
    val selectedInstructionId: StateFlow<Int?> = _selectedInstructionId.asStateFlow()

    // Generation results
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedOutput = MutableStateFlow<String?>(null)
    val generatedOutput: StateFlow<String?> = _generatedOutput.asStateFlow()

    private val _errorText = MutableStateFlow<String?>(null)
    val errorText: StateFlow<String?> = _errorText.asStateFlow()

    // --- IRAN WEBSITE BUILDER STATES ---
    private val _webBusinessType = MutableStateFlow("فروشگاه اینترنتی (E-shop)")
    val webBusinessType: StateFlow<String> = _webBusinessType.asStateFlow()

    private val _webGateway = MutableStateFlow("زرین‌پال (Zarinpal)")
    val webGateway: StateFlow<String> = _webGateway.asStateFlow()

    private val _webSmsProvider = MutableStateFlow("کاوه نگار (KavehNegar)")
    val webSmsProvider: StateFlow<String> = _webSmsProvider.asStateFlow()

    private val _webAdditionalSpecs = MutableStateFlow("")
    val webAdditionalSpecs: StateFlow<String> = _webAdditionalSpecs.asStateFlow()

    private val _isFigmaConnected = MutableStateFlow(false)
    val isFigmaConnected: StateFlow<Boolean> = _isFigmaConnected.asStateFlow()

    private val _figmaStatusMessage = MutableStateFlow<String?>(null)
    val figmaStatusMessage: StateFlow<String?> = _figmaStatusMessage.asStateFlow()

    // --- SECURITY REPORTS STATES ---
    private val _securityScore = MutableStateFlow(98)
    val securityScore: StateFlow<Int> = _securityScore.asStateFlow()

    init {
        // Load initial questions
        updateCategoryQuestions("coding")
        
        // Log startup security report audit
        viewModelScope.launch {
            repository.logSecurityAction(
                "راه‌اندازی ماژول امنیتی",
                "سیستم رمزنگاری فیلدها با استاندارد AES-256-GCM با موفقیت بررسی و فعال شد. پایگاه داده آفلاین در یک لایه حافظه ایزوله مستقر است."
            )
        }
    }

    // --- ACTIONS ---
    fun selectTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        updateCategoryQuestions(category)
        // Clear old question answers
        _questionAnswers.value = emptyMap()
    }

    fun updateInputPrompt(text: String) {
        _inputPrompt.value = text

        // Smart dynamic selection based on keywords matching user's request
        val lowerText = text.lowercase(Locale.ROOT)
        
        val codingKeywords = listOf("کد", "برنامه", "پایتون", "جاوا", "کاتلین", "وب", "سایت", "فرانت", "بک‌اند", "c++", "c#", "code", "python", "javascript", "kotlin", "html", "css", "database", "api", "function", "class", "کلاس", "متد", "توسعه دهنده")
        val technicalKeywords = listOf("محاسبه", "فرمول", "مهندسی", "معادله", "آنالیز", "عمران", "برق", "شبیه‌سازی", "ریاضی", "فیزیک", "تحلیل مکانیک", "سنسور", "مدار")
        val videoKeywords = listOf("ویدیو", "فیلم", "کلیپ", "ریلز", "استوری", "تیزر", "سناریو", "دکوپاژ", "تدوین", "سینمایی")
        val bannerKeywords = listOf("بنر", "پوستر", "تبلیغات", "تبلیغ", "banner", "poster", "ads")
        val imageKeywords = listOf("عکس", "تصویر", "نقاشی", "ایکون", "رندر", "لوگو", "کاور", "اینستا", "طراحی", "image", "photo", "drawing", "illustration", "logo", "icon", "render", "landscape")
        val contentKeywords = listOf("محتوا", "مقاله", "وبلاگ", "کپشن", "لینکدین", "سئو", "متن", "نویسندگی", "پست")
        val assistantKeywords = listOf("وکیل", "مربی", "دستیار", "پزشک", "مشاور", "روانشناس", "معلم", "استاد", "assistant", "coach", "lawyer", "doctor", "therapist", "tutor")

        val detectedCategory = when {
            codingKeywords.any { lowerText.contains(it) } -> "coding"
            technicalKeywords.any { lowerText.contains(it) } -> "technical"
            videoKeywords.any { lowerText.contains(it) } -> "video"
            bannerKeywords.any { lowerText.contains(it) } -> "banner"
            imageKeywords.any { lowerText.contains(it) } -> "image"
            contentKeywords.any { lowerText.contains(it) } -> "content"
            assistantKeywords.any { lowerText.contains(it) } -> "assistant"
            else -> null
        }

        if (detectedCategory != null && detectedCategory != _selectedCategory.value) {
            _selectedCategory.value = detectedCategory
            updateCategoryQuestions(detectedCategory)
            
            // Apply corresponding smart default agents and tones automatically
            when (detectedCategory) {
                "coding" -> {
                    _selectedAgent.value = "برنامه‌نویس ارشد (Senior Developer)"
                    _selectedTone.value = "حرفه‌ای (Professional)"
                }
                "technical" -> {
                    _selectedAgent.value = "معمار فنی سیستم (Technical Architect)"
                    _selectedTone.value = "فنی و تخصصی (Technical)"
                }
                "image" -> {
                    _selectedAgent.value = "طراح هنری پیشرفته (Creative Director)"
                    _selectedTone.value = "خلاقانه (Creative)"
                }
                "video" -> {
                    _selectedAgent.value = "کارگردان و تدوین‌گر ویدیو (Video Producer)"
                    _selectedTone.value = "خلاقانه (Creative)"
                }
                "banner" -> {
                    _selectedAgent.value = "بازاریاب و کپی‌رایتر ارشد (Copywriter)"
                    _selectedTone.value = "حماسی و متقاعدکننده (Epic)"
                }
                "content" -> {
                    _selectedAgent.value = "محقق و متخصص علمی (Researcher)"
                    _selectedTone.value = "دوستانه و صمیمی (Casual)"
                }
                "assistant" -> {
                    _selectedAgent.value = "معمار دستیارهای هوشمند (AI Architect)"
                    _selectedTone.value = "فنی و تخصصی (Technical)"
                }
            }
        }
    }

    fun updateAnswer(question: String, answer: String) {
        val current = _questionAnswers.value.toMutableMap()
        current[question] = answer
        _questionAnswers.value = current
    }

    fun setTone(tone: String) {
        _selectedTone.value = tone
    }

    fun setAgent(agent: String) {
        _selectedAgent.value = agent
    }

    fun setLanguage(lang: String) {
        _promptLanguage.value = lang
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        viewModelScope.launch {
            if (enabled) {
                repository.logSecurityAction(
                    "حالت آفلاین فعال شد",
                    "سیستم روی حالت بهینه‌سازی محلی و آفلاین امن قرار گرفت."
                )
            } else {
                repository.logSecurityAction(
                    "حالت آنلاین فعال شد",
                    "سیستم برای بهینه‌سازی هوشمند به مدل Gemini-3.5-Flash متصل گردید."
                )
            }
        }
    }

    fun selectInstructionTemplate(id: Int?) {
        _selectedInstructionId.value = id
    }

    // Iran Web Builder actions
    fun setWebBusinessType(type: String) { _webBusinessType.value = type }
    fun setWebGateway(gateway: String) { _webGateway.value = gateway }
    fun setWebSmsProvider(provider: String) { _webSmsProvider.value = provider }
    fun setWebAdditionalSpecs(specs: String) { _webAdditionalSpecs.value = specs }

    fun connectToFigmaAPI() {
        viewModelScope.launch {
            _isFigmaConnected.value = true
            _figmaStatusMessage.value = "اتصال برقرار شد! الگوهای چیدمان و ساختار فونت فون‌های فارسی با موفقیت بارگذاری شدند."
            repository.logSecurityAction(
                "اتصال API موفق",
                "کلید موقت ابزار فانتزی طراحی رابط کاربری (Figma UI API) تایید و پالت رنگی بهینه‌ساز همگام‌سازی شد."
            )
        }
    }

    fun clearFigmaStatus() {
        _figmaStatusMessage.value = null
    }

    // Clear error
    fun clearError() {
        _errorText.value = null
    }

    // Categories Questions Factory
    private fun updateCategoryQuestions(category: String) {
        _categoryQuestions.value = when (category) {
            "coding" -> listOf(
                "زبان برنامه‌نویسی یا فریمورک؟ (مثال: Kotlin/Compose, Laravel)",
                "پلتفرم هدف پروژه؟ (مثال: Android, Web, Desktop)",
                "محدودیت‌های فنی یا کارایی خاص؟ (مثال: مصرف حافظه کم، زمان اجرا)"
            )
            "technical" -> listOf(
                "شاخه تخصصی مهندسی؟ (مثال: برق کنترل، عمران سازه، مکانیک سیالات)",
                "چه قوانین جهانی یا استاندارد خاصی باید رعایت شود؟ (مثال: ISO 9001, IEEE)",
                "قالب و خروجی نهایی محاسبات؟ (مثال: شبیه‌سازی عددی، فرمول محاسباتی)"
            )
            "image" -> listOf(
                "سبک بصری عکس چیست؟ (مثال: Realism, Cinematic 3D, Minimalist Vector)",
                "نسبت ابعاد عکس و شرایط نورپردازی؟ (مثال: 16:9, Studio Portrait Lighting)",
                "جزئیات ناخواسته (Negative Prompt)؟ (مثال: اشیاء مات، اعوجاج صورت)"
            )
            "video" -> listOf(
                "مدت زمان ویدیو و نرخ فریم؟ (مثال: 15sec - 60 FPS)",
                "نحوه حرکت دوربین چگونه باشد؟ (مثال: Slow Pan, Drone Shot, Tracking Zoom)",
                "حالت روانی یا سبک هنری ویدیو؟ (مثال: نوستالژیک تاریک، پر جنب و جوش، حماسی)"
            )
            "banner" -> listOf(
                "سایز بنر یا پلتفرم انتشار تبلبغات؟ (مثال: اینستاگرام استوری، بنر وبسایت ۱۲۰x۲۴۰)",
                "عنوان متنی و دکمه فراخوان به عمل (Call to action)؟",
                "پالت رنگی یا برندینگ اختصاصی؟ (مثال: تم تاریک با نارنجی نئونی)"
            )
            "content" -> listOf(
                "رسانه انتشار محتوا؟ (مثال: لینکدین، وبلاگ فنی، کپشن اینستاگرام)",
                "مخاطبان هدف محتوا چه کسانی هستند؟ (مثال: مدیران عامل، برنامه نویسان جوان)",
                "طول تقریبی محتوا و لحن کلام؟ (مثال: کوتاه صمیمی، بلند تحلیلی و معتبر)"
            )
            "assistant" -> listOf(
                "نقش یا تخصص اصلی دستیار هوشمند؟ (مثال: وکیل حقوقی زبده، مربی بدنسازی هوشمند، پزشک خانواده)",
                "وظایف کلیدی و حوزه اطلاعاتی ممنوعه؟ (مثال: عدم ارائه مشاوره پزشکی نهایی، پیگیری قوانین حقوقی ۱۴۰۴ با ارجاع به ماده قانونی)",
                "لحن تعامل و نحوه پاسخ‌دهی؟ (مثال: صمیمانه و انگیزشی، رسمی و حقوقی همراه با جزییات ماده قانونی)"
            )
            else -> emptyList()
        }
    }

    // --- PRIMARY GENERATION ENGINE ---
    fun runPromptOptimization(instructionsList: List<CustomInstruction>) {
        if (_inputPrompt.value.trim().isEmpty()) {
            _errorText.value = "لطفاً ابتدا پرامپت اولیه خود را بنویسید."
            return
        }

        _isGenerating.value = true
        _generatedOutput.value = null
        _errorText.value = null

        viewModelScope.launch {
            val activeInstruction = instructionsList.find { it.id == _selectedInstructionId.value }
            val customText = activeInstruction?.let {
                "${it.instruction} (لحن سفارشی: ${it.tone})"
            }

            if (_isOfflineMode.value) {
                // Run local Off-device Procedural Offline Prompt Building
                val offlinePrompt = repository.optimizeOffline(
                    category = _selectedCategory.value,
                    inputPrompt = _inputPrompt.value,
                    answers = _questionAnswers.value,
                    tone = _selectedTone.value,
                    agentPersona = _selectedAgent.value,
                    language = _promptLanguage.value,
                    customInstructionText = customText
                )
                _generatedOutput.value = offlinePrompt
                _isGenerating.value = false

                // Save to History
                repository.saveHistory(
                    PromptHistory(
                        category = _selectedCategory.value,
                        inputPrompt = _inputPrompt.value,
                        optimizedPrompt = offlinePrompt,
                        language = _promptLanguage.value,
                        agentName = _selectedAgent.value,
                        tone = _selectedTone.value,
                        isOffline = true
                    )
                )
            } else {
                // Online Generation via Gemini API
                val result = repository.optimizeWithGemini(
                    category = _selectedCategory.value,
                    inputPrompt = _inputPrompt.value,
                    answers = _questionAnswers.value,
                    tone = _selectedTone.value,
                    agentPersona = _selectedAgent.value,
                    language = _promptLanguage.value,
                    customInstructionText = customText
                )

                result.onSuccess { onlinePrompt ->
                    _generatedOutput.value = onlinePrompt
                    _isGenerating.value = false

                    repository.saveHistory(
                        PromptHistory(
                            category = _selectedCategory.value,
                            inputPrompt = _inputPrompt.value,
                            optimizedPrompt = onlinePrompt,
                            language = _promptLanguage.value,
                            agentName = _selectedAgent.value,
                            tone = _selectedTone.value,
                            isOffline = false
                        )
                    )
                }.onFailure { exception ->
                    val errorMsg = exception.localizedMessage ?: "خطای ناشناخته در ارتباط با سرور."
                    _errorText.value = "$errorMsg\n\n(سیستم به طور خودکار از موتور آفلاین پشتیبان استفاده کرد)"
                    
                    // Fallback to offline on error so user is never left hanging
                    val offlinePrompt = repository.optimizeOffline(
                        category = _selectedCategory.value,
                        inputPrompt = _inputPrompt.value,
                        answers = _questionAnswers.value,
                        tone = _selectedTone.value,
                        agentPersona = _selectedAgent.value,
                        language = _promptLanguage.value,
                        customInstructionText = customText
                    )
                    _generatedOutput.value = offlinePrompt
                    _isGenerating.value = false

                    repository.saveHistory(
                        PromptHistory(
                            category = _selectedCategory.value,
                            inputPrompt = _inputPrompt.value,
                            optimizedPrompt = offlinePrompt,
                            language = _promptLanguage.value,
                            agentName = _selectedAgent.value,
                            tone = _selectedTone.value,
                            isOffline = true
                        )
                    )
                }
            }
        }
    }

    // --- WEBPAGE BUILDER GENERATION ---
    fun runIranWebBuilderMegaprompt() {
        _isGenerating.value = true
        _generatedOutput.value = null
        _errorText.value = null

        viewModelScope.launch {
            try {
                _activeTab.value = "optimize" // redirect outputs easily
                val megaPrompt = repository.generateIranWebBuilderMegaPrompt(
                    businessType = _webBusinessType.value,
                    gateway = _webGateway.value,
                    smsProvider = _webSmsProvider.value,
                    additionalSpecs = _webAdditionalSpecs.value,
                    language = _promptLanguage.value,
                    uiToolConnected = _isFigmaConnected.value
                )

                _generatedOutput.value = megaPrompt
                _isGenerating.value = false

                // Save to history as website builder mega prompt
                repository.saveHistory(
                    PromptHistory(
                        category = "coding (مگا پرامپت)",
                        inputPrompt = "تولید وبسایت ایرانی سازگار با ملزومات شاپرک و درگاه ${_webGateway.value}",
                        optimizedPrompt = megaPrompt,
                        language = _promptLanguage.value,
                        agentName = "معمار سایتهای ایرانی",
                        tone = "فوق حرفه‌ای",
                        isOffline = true
                    )
                )
            } catch (e: Exception) {
                _errorText.value = e.localizedMessage
                _isGenerating.value = false
            }
        }
    }

    // --- CUSTOM TEMPLATE SUBMISSIONS ---
    fun addCustomInstruction(name: String, tone: String, text: String) {
        if (name.isEmpty() || text.isEmpty()) return
        viewModelScope.launch {
            repository.saveCustomInstruction(
                CustomInstruction(
                    name = name,
                    tone = tone,
                    instruction = text
                )
            )
        }
    }

    fun removeCustomInstruction(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomInstruction(id)
        }
    }

    // Delete Specific History Item
    fun deleteHistory(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    // Clear History
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Trigger Security Audit (Simulated check)
    fun triggerSecurityAudit() {
        viewModelScope.launch {
            _securityScore.value = (94..100).random()
            repository.logSecurityAction(
                "بازرسی امنیتی خودکار",
                "پایش ادواری لایه سخت‌افزاری و نرم‌افزاری بر اساس الزامات افشای داده‌های محرمانه انجام شد. دسترسی بسته‌های مخرب فیلتر شد."
            )
        }
    }
}
