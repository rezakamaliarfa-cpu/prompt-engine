package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomInstruction
import com.example.data.PromptHistory
import com.example.ui.theme.TechAmber
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechEmerald
import com.example.ui.theme.TechMidnight
import com.example.ui.theme.TechSurface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptOptimizerApp(viewModel: PromptViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val inputPrompt by viewModel.inputPrompt.collectAsStateWithLifecycle()
    val categoryQuestions by viewModel.categoryQuestions.collectAsStateWithLifecycle()
    val questionAnswers by viewModel.questionAnswers.collectAsStateWithLifecycle()
    val selectedTone by viewModel.selectedTone.collectAsStateWithLifecycle()
    val selectedAgent by viewModel.selectedAgent.collectAsStateWithLifecycle()
    val promptLanguage by viewModel.promptLanguage.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val selectedInstructionId by viewModel.selectedInstructionId.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatedOutput by viewModel.generatedOutput.collectAsStateWithLifecycle()
    val errorText by viewModel.errorText.collectAsStateWithLifecycle()

    val webBusinessType by viewModel.webBusinessType.collectAsStateWithLifecycle()
    val webGateway by viewModel.webGateway.collectAsStateWithLifecycle()
    val webSmsProvider by viewModel.webSmsProvider.collectAsStateWithLifecycle()
    val webAdditionalSpecs by viewModel.webAdditionalSpecs.collectAsStateWithLifecycle()
    val isFigmaConnected by viewModel.isFigmaConnected.collectAsStateWithLifecycle()
    val figmaStatusMessage by viewModel.figmaStatusMessage.collectAsStateWithLifecycle()

    val securityScore by viewModel.securityScore.collectAsStateWithLifecycle()

    // Database flows
    val history by viewModel.promptHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val instructionsList by viewModel.customInstructions.collectAsStateWithLifecycle(initialValue = emptyList())
    val securityLogsList by viewModel.securityLogs.collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopSection(
                isOffline = isOfflineMode,
                onOfflineToggle = { viewModel.setOfflineMode(it) },
                securityScore = securityScore,
                onSecurityCheck = { viewModel.triggerSecurityAudit() }
            )
        },
        bottomBar = {
            BottomNavigation(activeTab = activeTab, onTabSelected = { viewModel.selectTab(it) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Notification toast for errors
            LaunchedEffect(errorText) {
                errorText?.let {
                    Toast.makeText(context, "⚠️ $it", Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }

            // Notification for Figma status
            LaunchedEffect(figmaStatusMessage) {
                figmaStatusMessage?.let {
                    Toast.makeText(context, "🔌 $it", Toast.LENGTH_SHORT).show()
                    viewModel.clearFigmaStatus()
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "optimize" -> {
                        OptimizeTab(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            inputPrompt = inputPrompt,
                            onInputPromptChange = { viewModel.updateInputPrompt(it) },
                            questions = categoryQuestions,
                            answers = questionAnswers,
                            onAnswerChange = { q, a -> viewModel.updateAnswer(q, a) },
                            selectedTone = selectedTone,
                            onToneSelected = { viewModel.setTone(it) },
                            selectedAgent = selectedAgent,
                            onAgentSelected = { viewModel.setAgent(it) },
                            promptLanguage = promptLanguage,
                            onLanguageSelected = { viewModel.setLanguage(it) },
                            customInstructions = instructionsList,
                            selectedInstructionId = selectedInstructionId,
                            onInstructionSelected = { viewModel.selectInstructionTemplate(it) },
                            isOffline = isOfflineMode,
                            onOfflineToggle = { viewModel.setOfflineMode(it) },
                            isGenerating = isGenerating,
                            generatedOutput = generatedOutput,
                            onOptimizeTrigger = { viewModel.runPromptOptimization(instructionsList) },
                            onCopyPrompt = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "پرامپت کپی شد!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    "builder" -> {
                        WebBuilderTab(
                            businessType = webBusinessType,
                            onBusinessTypeChange = { viewModel.setWebBusinessType(it) },
                            gateway = webGateway,
                            onGatewayChange = { viewModel.setWebGateway(it) },
                            smsProvider = webSmsProvider,
                            onSmsProviderChange = { viewModel.setWebSmsProvider(it) },
                            additionalSpecs = webAdditionalSpecs,
                            onAdditionalSpecsChange = { viewModel.setWebAdditionalSpecs(it) },
                            isFigmaConnected = isFigmaConnected,
                            onConnectFigma = { viewModel.connectToFigmaAPI() },
                            onGenerateMegaPrompt = { viewModel.runIranWebBuilderMegaprompt() }
                        )
                    }
                    "custom" -> {
                        CustomInstructionsTab(
                            instructionsList = instructionsList,
                            onAddInstruction = { name, tone, text -> viewModel.addCustomInstruction(name, tone, text) },
                            onDeleteInstruction = { id -> viewModel.removeCustomInstruction(id) }
                        )
                    }
                    "logs" -> {
                        LogsAndHistoryTab(
                            history = history,
                            securityLogs = securityLogsList,
                            onDeleteHistory = { id -> viewModel.deleteHistory(id) },
                            onClearHistory = { viewModel.clearAllHistory() },
                            onCopyHistory = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "پرامپت بهینه‌سازی شده کپی شد!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    "about" -> {
                        AboutAppTab()
                    }
                }
            }
        }
    }
}

// --- SUBSECTION COMPOSABLES ---

@Composable
fun TopSection(
    isOffline: Boolean,
    onOfflineToggle: (Boolean) -> Unit,
    securityScore: Int,
    onSecurityCheck: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action / Indicators Column (Toggle & Audit badges)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Online/Offline mode interactive badge (Clickable to Toggle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isOffline) Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        .clickable { onOfflineToggle(!isOffline) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isOffline) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "⚠️ آفلاین بومی" else "⚡ آنلاین Gemini",
                        fontSize = 10.sp,
                        color = if (isOffline) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Security badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .clickable { onSecurityCheck() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF10B981)) // Emerald 500
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "امنیت: %$securityScore",
                        fontSize = 10.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Branding: Logo + Title Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "مهندس پرامپت",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOffline) "مهندسی آفلاین و بومی" else "بهینه‌ساز متصل به Gemini",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }

                // Rotated square logo box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    // draw rotated card inside
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(45f)
                            .border(1.5.dp, Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun OptimizeTab(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    inputPrompt: String,
    onInputPromptChange: (String) -> Unit,
    questions: List<String>,
    answers: Map<String, String>,
    onAnswerChange: (String, String) -> Unit,
    selectedTone: String,
    onToneSelected: (String) -> Unit,
    selectedAgent: String,
    onAgentSelected: (String) -> Unit,
    promptLanguage: String,
    onLanguageSelected: (String) -> Unit,
    customInstructions: List<CustomInstruction>,
    selectedInstructionId: Int?,
    onInstructionSelected: (Int?) -> Unit,
    isOffline: Boolean,
    onOfflineToggle: (Boolean) -> Unit,
    isGenerating: Boolean,
    generatedOutput: String?,
    onOptimizeTrigger: () -> Unit,
    onCopyPrompt: (String) -> Unit
) {
    // Premium Category branch specification metadata for gorgeous window views
    val categories = listOf(
        "coding" to Triple("دستیار کد نویسی", "💻", "مهندسی کدهای کاتلین، ری‌اکت، ساختارهای شی‌گرا، الگوریتم‌های پیچیده و عیب‌یابی بومی فرانت‌اند و بک‌اند به صورت کاملاً لوکال."),
        "technical" to Triple("فنی ، مهندسی", "⚙️", "طراحی فرمول‌های محاسباتی، معماری پیشرفته سیستم‌های توزیع‌شده، دیتابیس بومی، اتوماسیون صنعتی و دوآپس ایمن."),
        "image" to Triple("خلاقیت تصویر (Graphic)", "🖼️", "مهندسی دقیق پرامپت‌های تصویری Midjourney، مدل‌های گرافیکی مدرن، تنظیمات نورپردازی استودیویی و نگاتیو پرامپت."),
        "video" to Triple("تولید و تدوین ویدیو", "🎬", "تهیه سناریوها و دکوپاژهای سینمایی کوتاه و بلند، نگارش اسکریپت فیلم‌نامه‌های صنعتی و دستور کارگردانی Runway/Sora."),
        "banner" to Triple("مهندسی تبلیغات و بنر", "🎨", "کپی‌رایتینگ متقاعدکننده، تنظیم گرافیک پالت در بنرهای تبلیغاتی ایرانی، شعارسازی تجاری و عبارات فراخوان به عمل اثربخش."),
        "content" to Triple("تولید محتوای تخصصی", "📝", "نویسندگی سئو شده وبلاگ، نگارش پست‌های تعاملی لینکدین، متون شبکه اجتماعی، کپشن‌ها و تحلیل‌های عمیق بازاریابی کلامی."),
        "assistant" to Triple("ساخت دستیار هوشمند", "🤖", "طراحی هوشمند پرامپت‌های سیستم، مهارت‌ها و گاردریل‌های بهینه هوشمند برای انواع دستیارهای شخصی هوشمند مانند ساخت وکیل، مربی بدنسازی هوش مصنوعی و غیره.")
    )

    val tones = listOf(
        "حرفه‌ای (Professional)",
        "خلاقانه (Creative)",
        "فنی و تخصصی (Technical)",
        "دوستانه و صمیمی (Casual)",
        "ادبی و رسمی (Formal)",
        "حماسی و متقاعدکننده (Epic)"
    )

    val agents = listOf(
        "برنامه‌نویس ارشد (Senior Developer)",
        "معمار فنی سیستم (Technical Architect)",
        "طراح هنری پیشرفته (Creative Director)",
        "کارگردان و تدوین‌گر ویدیو (Video Producer)",
        "بازاریاب و کپی‌رایتر ارشد (Copywriter)",
        "محقق و متخصص علمی (Researcher)",
        "معمار دستیارهای هوشمند (AI Architect)"
    )

    var agentMenuExpanded by remember { mutableStateOf(false) }
    var toneMenuExpanded by remember { mutableStateOf(false) }

    if (selectedCategory.isEmpty()) {
        // --- LANDING PAGE: PREMIUM CATEGORY CATALOG LAUNCHPAD ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // Welcome Warm Banner (anti eye strain)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "دستیار امنیتی مهندسی پرامپت",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("💡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای شروع فرآیند غنی‌سازی و مهندسی پرامپت، یکی از بخش‌های تخصصی زیر را انتخاب نمایید تا پنجرهٔ پردازش لوکال و امن مخصوص به آن باز شود.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Categories list (Styled as gorgeous distinct branch windows)
            item {
                Text(
                    text = "دپارتمان‌های بهینه‌سازی (انتخاب شاخه):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                    textAlign = TextAlign.Right
                )
            }

            // High Fidelity Display of each department in pairs or clean single cards
            // To fit all screens nicely, let's display them as gorgeous stacked card windows!
            categories.forEach { (id, details) ->
                val (farsiName, emoji, description) = details
                item {
                    Card(
                        onClick = { onCategorySelected(id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "پنجره مجزا",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = farsiName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "ورود به پنجره مهندسی",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Enter",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Large Category Emoji Icon box
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- SEPARATE WINDOW VIEW FOR SELECTED CATEGORY WORKSPACE ---
        val selectedDetails = categories.find { it.first == selectedCategory }?.second
        val categoryName = selectedDetails?.first ?: "شاخه تخصصی"
        val categoryEmoji = selectedDetails?.second ?: "💻"
        val categoryDesc = selectedDetails?.third ?: ""

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // BACK BUTTON & WINDOW HEADER BAR
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back/Close Button styled to mimic separate app window
                    Button(
                        onClick = { onCategorySelected("") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خروج از پنجره", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Window Title details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "پنجره اختصاصی: $categoryName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = categoryEmoji, fontSize = 16.sp)
                    }
                }
            }

            // Description info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "محلی و امن: $categoryDesc",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Agent Identity Panel within this window Workspace
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "۱. تخصیص هویت کمکی ایجنت:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { agentMenuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown Open",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = selectedAgent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right
                                )
                            }
                            
                            DropdownMenu(
                                expanded = agentMenuExpanded,
                                onDismissRequest = { agentMenuExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                agents.forEach { agent ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = agent,
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedAgent == agent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedAgent == agent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        onClick = {
                                            onAgentSelected(agent)
                                            agentMenuExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tone Selection within this window Workspace
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "۲. انتخاب لحن و بیان خروجی:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { toneMenuExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown Open",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = selectedTone,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right
                                )
                            }
                            
                            DropdownMenu(
                                expanded = toneMenuExpanded,
                                onDismissRequest = { toneMenuExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                tones.forEach { tone ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = tone,
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedTone == tone) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedTone == tone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        onClick = {
                                            onToneSelected(tone)
                                            toneMenuExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Draft Simple Prompt Input Block
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "۳. ایده یا پرامپت سادهٔ اولیه شما:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp),
                            textAlign = TextAlign.Right
                        )
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = onInputPromptChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "ایده اصلی خود را اینجا بنویسید تا سیستم آن را برای شما کامپایل کند...",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Right
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Content
                            ),
                            singleLine = false,
                            maxLines = 5,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Refinement Questions (Dynamic per Category Branch)
            if (questions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "۴. سوالات اختصاصی بهینه‌سازی (اختیاری):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Questions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            questions.forEach { question ->
                                val answer = answers[question] ?: ""
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = question,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        textAlign = TextAlign.Right
                                    )
                                    OutlinedTextField(
                                        value = answer,
                                        onValueChange = { onAnswerChange(question, it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Right,
                                            textDirection = TextDirection.Content
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language & Template Configs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                Button(
                                    onClick = { onLanguageSelected("farsi") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (promptLanguage == "farsi") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        "فارسی",
                                        fontSize = 10.sp,
                                        color = if (promptLanguage == "farsi") MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onLanguageSelected("english") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (promptLanguage == "english") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        "English",
                                        fontSize = 10.sp,
                                        color = if (promptLanguage == "english") MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = "۵. زبان خروجی پرامپت:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Saved custom templates injection
                        if (customInstructions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تزریق فرمان‌های اختصاصی شخصی شما:",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp),
                                textAlign = TextAlign.Right
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectedInstructionId == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable { onInstructionSelected(null) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("بدون دستور", fontSize = 10.sp)
                                    }
                                }
                                items(customInstructions) { template ->
                                    val isSelected = selectedInstructionId == template.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable { onInstructionSelected(template.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(template.name, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ⚡ ONLINE/OFFLINE ENGINE SELECTOR CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOffline) MaterialTheme.colorScheme.surface 
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    border = BorderStroke(1.dp, if (isOffline) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left interactive selector switcher
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Offline Option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isOffline) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { onOfflineToggle(true) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "آفلاین بومی", 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOffline) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Online Option
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isOffline) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { onOfflineToggle(false) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "آنلاین Gemini", 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isOffline) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "موتور بهینه‌سازی پرامپت:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isOffline) 
                                "💡 تولید محلی: پردازش بئمی با بهره‌گیری از هویت تخصصی فریمورک‌های CO-STAR و RTFC به صورت آفلاین." 
                                else "⚡ بهینه‌سازی ابری: اتصال زنده به مدل قدرتمند Gemini-3.5-Flash جهت عمیق‌سازی و بال‌وپر دادن بی‌نظیر به ایده شما.",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // PRIMARY PROCESS BUTTON inside this dedicated Window
            item {
                Button(
                    onClick = onOptimizeTrigger,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isOffline) "در حال شبیه‌سازی الگوها..." else "در حال تحلیل و بهینه‌سازی با Gemini...",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isOffline) "تولید و کامپایل پرامپت محلی" else "بهینه‌سازی هوشمند با هوش مصنوعی (Gemini)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isOffline) "🚀" else "⚡", fontSize = 16.sp)
                        }
                    }
                }
            }

            // OUTPUT AREA inside this dedicated Window
            if (generatedOutput != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onCopyPrompt(generatedOutput) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("کپی پرامپت", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "خروجی مهندسی شده نهایی:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = generatedOutput ?: "",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WebBuilderTab(
    businessType: String,
    onBusinessTypeChange: (String) -> Unit,
    gateway: String,
    onGatewayChange: (String) -> Unit,
    smsProvider: String,
    onSmsProviderChange: (String) -> Unit,
    additionalSpecs: String,
    onAdditionalSpecsChange: (String) -> Unit,
    isFigmaConnected: Boolean,
    onConnectFigma: () -> Unit,
    onGenerateMegaPrompt: () -> Unit
) {
    val types = listOf(
        "فروشگاه اینترنتی (E-shop)",
        "سامانه آموزشی / آموزشگاهی (LMS)",
        "شرکتی با خدمات معرفی محصول (CMS)",
        "پرتال نوبت‌دهی و رزرو آنلاین (Booking)"
    )

    val gateways = listOf(
        "زرین‌پال (Zarinpal)",
        "نکست‌پی (NextPay)",
        "آی‌دی‌پی (IDPay)",
        "درگاه مستقیم بانک ملت (Behpardakht)"
    )

    val smsProviders = listOf(
        "کاوه نگار (KavehNegar)",
        "ملی پیامک (MelliPayamak)",
        "سامانه پیامک ایده پردازان (SMS.ir)"
    )

    var businessTypeExpanded by remember { mutableStateOf(false) }
    var gatewayExpanded by remember { mutableStateOf(false) }
    var smsProviderExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "کدساز و مگا-پرامپت ویژه وبسایتهای ایرانی",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "تولید کدهای سازگار با فیلترها، سیستم مالی، شاپرک و درگاه‌های پیامکی ایران ویژه Next.js و سایر فریمورک‌ها.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Section for design tool integration API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onConnectFigma,
                        enabled = !isFigmaConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = TechEmerald.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFigmaConnected) "اتصال API تایید شد" else "اتصال به Figma API",
                            fontSize = 11.sp,
                            color = if (isFigmaConnected) TechEmerald else MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "همگام‌سازی با ابزارهای طراحی:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFigmaConnected) "کلید Figma بارگذاری شد" else "اتصال صوری به توکن‌های طراحی",
                            fontSize = 10.sp,
                            color = if (isFigmaConnected) TechEmerald else TechAmber
                        )
                    }
                }
            }
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Type dropdown
                    Text("نوع کسب‌وکار:", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = businessType,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { businessTypeExpanded = true },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { businessTypeExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            )
                        )
                        DropdownMenu(
                            expanded = businessTypeExpanded,
                            onDismissRequest = { businessTypeExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        onBusinessTypeChange(type)
                                        businessTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gateway dropdown
                    Text("درگاه بانکی شاپرک (سازگار با ایران):", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = gateway,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { gatewayExpanded = true },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { gatewayExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            )
                        )
                        DropdownMenu(
                            expanded = gatewayExpanded,
                            onDismissRequest = { gatewayExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            gateways.forEach { gate ->
                                DropdownMenuItem(
                                    text = { Text(gate, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        onGatewayChange(gate)
                                        gatewayExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SMS providers
                    Text("سامانه احراز هویت پیامکی بومی:", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = smsProvider,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { smsProviderExpanded = true },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { smsProviderExpanded = true }
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            )
                        )
                        DropdownMenu(
                            expanded = smsProviderExpanded,
                            onDismissRequest = { smsProviderExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            smsProviders.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        onSmsProviderChange(prov)
                                        smsProviderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Additional specifications
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "جزییات و نیازمندی‌های اضافه وبسایت:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = additionalSpecs,
                        onValueChange = onAdditionalSpecsChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "مثال: پیاده سازی سبد خرید ریالی با وب‌پک، یا قوانین اینماد...",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.Content
                        ),
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Generate mega prompt button
        item {
            Button(
                onClick = onGenerateMegaPrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Web Mega-prompt")
                Spacer(modifier = Modifier.width(6.dp))
                textDirectionSemantics(TextDirection.Rtl)
                Text("ساخت مگا پرامپت سایت‌ساز سازگار با ایران", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomInstructionsTab(
    instructionsList: List<CustomInstruction>,
    onAddInstruction: (String, String, String) -> Unit,
    onDeleteInstruction: (Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("تخصصی") }
    var instructionValue by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "دستور دستورات و مدل‌سازی اختصاصی شما:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = "شخصی‌سازی لحن پاسخ‌ها و بارگذاری قوانین طلایی ثابت شما در موتور بهینه‌ساز هوشمند.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // Form to add templates
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "افزودن دستور شخصی جدید:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام دستور (مثال: سئو و کلید واژه)", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Right, textDirection = TextDirection.Content),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tone,
                        onValueChange = { tone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("لحن خروجی پاسخ (مثال: طنزآمیز، بسیار آکادمیک)", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Right, textDirection = TextDirection.Content),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = instructionValue,
                        onValueChange = { instructionValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("قوانین و بدنه دستورالعمل اختصاصی شما:", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Right, textDirection = TextDirection.Content),
                        maxLines = 5,
                        shape = RoundedCornerShape(6.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && instructionValue.isNotEmpty()) {
                                onAddInstruction(name, tone, instructionValue)
                                name = ""
                                instructionValue = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت دستور در محیط ایزوله امن", fontSize = 12.sp)
                    }
                }
            }
        }

        // List of custom instructions
        if (instructionsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ دستور دائم ثبت نشده است. از منوی بالا اضافه کنید.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(instructionsList) { inst ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onDeleteInstruction(inst.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = inst.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "لحن: ${inst.tone}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = inst.instruction,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogsAndHistoryTab(
    history: List<PromptHistory>,
    securityLogs: List<com.example.data.SecurityLog>,
    onDeleteHistory: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onCopyHistory: (String) -> Unit
) {
    val subTab = "history"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        if (subTab == "history") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (history.isNotEmpty()) {
                    Button(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("حذف همگی", fontSize = 10.sp, color = Color.Red)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = "تاریخچه محلی رمزنگاری‌شده:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("تاریخچه‌ای ثبت نشده است.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        IconButton(onClick = { onDeleteHistory(record.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { onCopyHistory(record.optimizedPrompt) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (record.isOffline) {
                                                Text(
                                                    "[آفلاین]",
                                                    fontSize = 9.sp,
                                                    color = TechAmber,
                                                    modifier = Modifier.padding(end = 4.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = when(record.category) {
                                                    "coding" -> "دستیار کد نویسی"
                                                    "technical" -> "فنی ، مهندسی"
                                                    "image" -> "تولید عکس"
                                                    "video" -> "تولید ویدیو"
                                                    "banner" -> "تولید بنر"
                                                    "content" -> "تولید محتوا"
                                                    "assistant" -> "ساخت دستیار هوشمند"
                                                    else -> record.category
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        val date = SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date(record.timestamp))
                                        Text(text = date, fontSize = 9.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "پرامپت اولیه: ${record.inputPrompt}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = record.optimizedPrompt,
                                        fontSize = 10.sp,
                                        maxLines = 4,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Security Logs view
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(TechEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("پرداخت لایه ایزوله: AES-GCM", fontSize = 10.sp, color = TechEmerald)
                }
                Text(
                    text = "بازرسی دوره‌ای امنیت سیستم:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(securityLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (log.action.contains("خطا")) Color.Red.copy(alpha = 0.05f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (log.action.contains("خطا")) Color.Red.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.encryptionType,
                                    fontSize = 9.sp,
                                    color = if (log.action.contains("خطا")) Color.Red else TechEmerald,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = log.action,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.action.contains("خطا")) Color.Red else MaterialTheme.colorScheme.primary
                                    )
                                    val logDate = SimpleDateFormat("HH:mm:ss.S", Locale.getDefault()).format(Date(log.timestamp))
                                    Text(text = logDate, fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.details,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigation(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = activeTab == "about",
            onClick = { onTabSelected("about") },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "About") },
            label = { Text("درباره برنامه", fontSize = 9.sp) }
        )

        NavigationBarItem(
            selected = activeTab == "logs",
            onClick = { onTabSelected("logs") },
            icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Logs") },
            label = { Text("مهندس پرامپت", fontSize = 9.sp) }
        )

        NavigationBarItem(
            selected = activeTab == "custom",
            onClick = { onTabSelected("custom") },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Templates") },
            label = { Text("فرمان شخصی", fontSize = 9.sp) }
        )

        NavigationBarItem(
            selected = activeTab == "builder",
            onClick = { onTabSelected("builder") },
            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Builder") },
            label = { Text("سایت‌ساز بومی", fontSize = 9.sp) }
        )

        NavigationBarItem(
            selected = activeTab == "optimize",
            onClick = { onTabSelected("optimize") },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("بهینه‌سازی پرامپت", fontSize = 9.sp) }
        )
    }
}

// Utility extension for styling RTL clean layouts
@Composable
private fun textDirectionSemantics(direction: TextDirection) {
    LocalTextStyle.current.copy(textDirection = direction)
}

@Composable
fun AboutAppTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // App Specifications & Logo
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Box (glowing cyber design)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(45f)
                                .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "بهینه‌ساز پرامپت (نسخه ۴.۲ لوکال)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "دستیار مهندسی پرامپت و همکار توسعه بومی ایران",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Programmer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "رضا کمالی",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "طراح و برنامه‌نویس ارشد اندروید",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // Visual Avatar/Initials
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "رک",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    
                    // Contact Info with Call Action
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:090126302843")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تماس مستقیم (کلیک کنید)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "090126302843",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Features Card List
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ویژگی‌ها و قابلیت‌های محوری:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                    textAlign = TextAlign.Right
                )
                
                val features = listOf(
                    Triple("🔒", "عملکرد ۱۰۰٪ مستقل و آفلاین بومی", "سیستم بهینه‌سازی پرامپت‌ها بدون نیاز به اینترنت یا سرویس‌های هوش مصنوعی ابری به صورت کاملاً لوکال و با امنیت بالا کار می‌کند تا حریم خصوصی کدهای حساس و داده‌های شرکتی شما تضمین شود."),
                    Triple("🎯", "مهندسی پرامپت چند لایه ساختاریافته", "اعمال ساختار پیشرفته الگو، شخصیت (Persona)، قوانین خروجی سخت‌گیرانه تعبیه شده و قالب‌های دستوری طبق استاندارد برترین مدل‌های زبانی روز دنیا."),
                    Triple("🛠️", "سایت‌ساز بومی تخصصی ایران", "امکان اختصاصی تولید مگافرانت‌اندهای بهینه‌شده وب فارسی شامل کدهای اتصال به درگاه‌های پرداخت ایرانی، پیامک‌رسان‌های داخلی و هماهنگ‌سازی گرافیک فیگما."),
                    Triple("📂", "تاریخچه و لاگ امنیتی محلی", "امکان ذخیره، دسته‌بندی و پاکسازی تاریخچه استفاده به همراه بررسی مداوم امتیاز امنیتی پرامپت‌ها به صورت آفلاین بر روی دیتابیس Room دستگاه.")
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    features.forEach { (emoji, title, desc) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

