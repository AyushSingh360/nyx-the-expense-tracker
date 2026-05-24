package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Expense
import com.example.ui.CategoryHelper
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun Modifier.glassCard(
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    tint: Color = Color.White,
    alphaMultiplier: Float = 1.0f
): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x2E1E1E1E).copy(alpha = 0.28f * alphaMultiplier),
                Color(0x1F0F0F0F).copy(alpha = 0.16f * alphaMultiplier)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = 0.28f * alphaMultiplier),
                Color.Transparent,
                tint.copy(alpha = 0.08f * alphaMultiplier)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = DarkBackground
                ) { innerPadding ->
                    ExpenseTrackerApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)
    )
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showInsightsSection by remember { mutableStateOf(true) } // toggled by "Insights" shortcut button!
    var showQuickSeedInfo by remember { mutableStateOf(false) } // toggled by "Settings" shortcut button

    // Tab Navigation State Simulation
    var activeTab by remember { mutableStateOf("HOME") }
    var calendarSelectedDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        )
    }

    val today = remember { Calendar.getInstance() }
    var calendarCurrentYear by remember { mutableStateOf(today.get(Calendar.YEAR)) }
    var calendarCurrentMonth by remember { mutableStateOf(today.get(Calendar.MONTH)) }
    var calendarFilterByDayOnly by remember { mutableStateOf(true) }

    // State Collection
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle(initialValue = 0.0)
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle(initialValue = 0.0)
    val categoryDistribution by viewModel.categoryDistribution.collectAsStateWithLifecycle()

    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val balance = totalIncome - totalExpense

    // Reactively compute daily list and display list in composable context, outer to any LazyListScope
    val calendarDailyExpensesList = remember(calendarSelectedDate, expenses) {
        val calSelected = Calendar.getInstance().apply { time = calendarSelectedDate }
        val selYear = calSelected.get(Calendar.YEAR)
        val selMonth = calSelected.get(Calendar.MONTH)
        val selDay = calSelected.get(Calendar.DAY_OF_MONTH)
        
        val calExpense = Calendar.getInstance()
        expenses.filter { expense ->
            calExpense.timeInMillis = expense.timestamp
            calExpense.get(Calendar.YEAR) == selYear &&
            calExpense.get(Calendar.MONTH) == selMonth &&
            calExpense.get(Calendar.DAY_OF_MONTH) == selDay
        }
    }
    val calendarDisplayList = remember(calendarFilterByDayOnly, calendarDailyExpensesList, expenses) {
        if (calendarFilterByDayOnly) {
            calendarDailyExpensesList
        } else {
            expenses.take(100)
        }
    }

    // Formatting Helpers
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind {
                // Base background black
                drawRect(color = Color(0xFF030101))

                val w = size.width
                val h = size.height

                // Draw background ambient blue/cyan backlights (Behind certain curves for color temperature contrast)
                // Ambient aura 1: middle-right serpentine curve aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2E12354F), Color(0x000F1F2F)),
                        center = Offset(w * 0.85f, h * 0.50f),
                        radius = w * 0.7f
                    ),
                    center = Offset(w * 0.85f, h * 0.50f),
                    radius = w * 0.7f
                )
                
                // Ambient aura 2: bottom curve aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x281C3040), Color(0x000A131C)),
                        center = Offset(w * 0.20f, h * 0.88f),
                        radius = w * 0.8f
                    ),
                    center = Offset(w * 0.20f, h * 0.88f),
                    radius = w * 0.8f
                )

                // 1. Serpentine/modular glowing track path matching layout exactly
                val conduitPath = Path().apply {
                    // Top horizontal/loop
                    moveTo(w * 0.12f, h * 0.08f)
                    cubicTo(w * 0.12f, h * 0.03f, w * 0.25f, h * 0.015f, w * 0.50f, h * 0.015f)
                    lineTo(w * 0.80f, h * 0.015f)
                    cubicTo(w * 0.94f, h * 0.015f, w * 0.94f, h * 0.08f, w * 0.94f, h * 0.08f)
                    lineTo(w * 0.94f, h * 0.12f)
                    
                    // Top inner partition back left
                    cubicTo(w * 0.94f, h * 0.18f, w * 0.78f, h * 0.18f, w * 0.50f, h * 0.18f)
                    lineTo(w * 0.20f, h * 0.18f)
                    
                    // Smooth transition back right
                    cubicTo(w * 0.06f, h * 0.18f, w * 0.06f, h * 0.28f, w * 0.20f, h * 0.28f)
                    lineTo(w * 0.80f, h * 0.28f)
                    
                    // Right loop down to middle
                    cubicTo(w * 0.94f, h * 0.28f, w * 0.94f, h * 0.35f, w * 0.94f, h * 0.38f)
                    
                    // The S curve sequence in middle-right
                    cubicTo(w * 0.94f, h * 0.42f, w * 0.82f, h * 0.45f, w * 0.72f, h * 0.48f)
                    cubicTo(w * 0.62f, h * 0.51f, w * 0.62f, h * 0.56f, w * 0.72f, h * 0.59f)
                    cubicTo(w * 0.82f, h * 0.62f, w * 0.94f, h * 0.65f, w * 0.94f, h * 0.68f)
                    lineTo(w * 0.94f, h * 0.74f)
                    
                    // Bottom loop capsule going back left
                    cubicTo(w * 0.94f, h * 0.82f, w * 0.78f, h * 0.82f, w * 0.50f, h * 0.82f)
                    lineTo(w * 0.20f, h * 0.82f)
                    
                    // Smooth transition to bottom-most horizontal tracking edge
                    cubicTo(w * 0.06f, h * 0.82f, w * 0.06f, h * 0.94f, w * 0.20f, h * 0.94f)
                    lineTo(w * 0.80f, h * 0.94f)
                }

                // 2. Left vertical dividing branch
                val divisionPath = Path().apply {
                    moveTo(w * 0.12f, h * 0.34f)
                    lineTo(w * 0.12f, h * 0.75f)
                }

                // 3. Screen Enclosing Oval/Rounded Border Outer frame
                val borderPath = Path().apply {
                    val rx = w * 0.04f
                    val ry = h * 0.02f
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(rx, ry, w - rx, h - ry),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
                        )
                    )
                }

                // Styles
                val orangeGlowBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1AEE5A1B),
                        Color(0x30CD7F32),
                        Color(0x15FF7F3F)
                    )
                )

                val copperBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A1E10), // Obsidian copper core dark shadow
                        Color(0xFFCD6F3E), // Lustrous warm copper highlight
                        Color(0xFFFFAD79), // Highly luminous specular gradient
                        Color(0xFF8B3A1A)  // Dark bronze rust transition
                    )
                )

                val rimGlintBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFDFB0),
                        Color(0xFFFFF6ED),
                        Color(0xFFFFEAD9)
                    )
                )

                // Define multiple strokes for photorealistic layering
                val wideGlowStroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
                val coreRidgeStroke = Stroke(width = w * 0.048f, cap = StrokeCap.Round)
                val darkValleyStroke = Stroke(width = w * 0.034f, cap = StrokeCap.Round)
                val specularStroke = Stroke(width = w * 0.005f, cap = StrokeCap.Round)

                // Render multi-layered 3D pipeline
                // Layer 1: Diffuse soft warm light shadows
                drawPath(path = borderPath, brush = orangeGlowBrush, style = wideGlowStroke)
                drawPath(path = conduitPath, brush = orangeGlowBrush, style = wideGlowStroke)
                drawPath(path = divisionPath, brush = orangeGlowBrush, style = wideGlowStroke)

                // Layer 2: Glossy metallic gold/copper borders
                drawPath(path = borderPath, brush = copperBrush, style = coreRidgeStroke)
                drawPath(path = conduitPath, brush = copperBrush, style = coreRidgeStroke)
                drawPath(path = divisionPath, brush = copperBrush, style = coreRidgeStroke)

                // Layer 3: High-contrast Dark Valley masks
                val valleyBlack = Color(0xFF030101)
                drawPath(path = borderPath, color = valleyBlack, style = darkValleyStroke)
                drawPath(path = conduitPath, color = valleyBlack, style = darkValleyStroke)
                drawPath(path = divisionPath, color = valleyBlack, style = darkValleyStroke)

                // Layer 4: Extremely sharp crisp Specular Rim highlight
                drawPath(path = borderPath, brush = rimGlintBrush, style = specularStroke)
                drawPath(path = conduitPath, brush = rimGlintBrush, style = specularStroke)
                drawPath(path = divisionPath, brush = rimGlintBrush, style = specularStroke)

                // Layer 5: Extremely bright glow flare at the bottom track line
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x00FF8C00), Color(0xFFFFF2D5), Color(0xFFFF5F1F), Color(0x00FF5F1F))
                    ),
                    topLeft = Offset(w * 0.15f, h * 0.935f),
                    size = androidx.compose.ui.geometry.Size(w * 0.7f, w * 0.015f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Highly optimized LazyColumn for scroll performance (60 FPS rendering & recycling)
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Top App Bar & Settings info overlay are rendered as the first static item
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OVERVIEW",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 3.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDarkSecondary.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = let {
                                    val currentMonth = SimpleDateFormat("MMMM", Locale.US).format(Date())
                                    currentMonth
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = TextDarkPrimary
                                )
                            )
                        }

                        // Avatar keyframe matching HTML border-[#2D2D2D]
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(1.dp, Color(0xFF2D2D2D), CircleShape)
                                .clickable { showQuickSeedInfo = !showQuickSeedInfo },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = TextDarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Custom Settings info overlay
                    AnimatedVisibility(visible = showQuickSeedInfo) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .glassCard(cornerRadius = 16.dp, tint = GoldAccent),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Tips", tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Sophisticated Ledger Mode Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )
                                    Text(
                                        text = "Tap categories below to filter, or press '＋' to register live records synchronously into the secure Room unit.",
                                        fontSize = 11.sp,
                                        color = TextDarkSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Area Toggled by Simulated Bottom Tabs
                when (activeTab) {
                    "HOME" -> {
                        item {
                            // Balance & Elegant stats area
                            AestheticBalanceCard(
                                balance = balance,
                                income = totalIncome,
                                expense = totalExpense,
                                formatter = currencyFormatter
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Shortcuts Bar (Add, Transfer, Insights Toggle, Settings Overlay)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column 1: Add (Translucent Glass Button style)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .glassCard(cornerRadius = 16.dp, tint = OffWhitePrimary)
                                            .background(OffWhitePrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                            .clickable { showAddDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Log",
                                            tint = OffWhitePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = "Add",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkSecondary.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Column 3: Insight Toggle Chart (Glassmorphic)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .glassCard(cornerRadius = 16.dp, tint = if (showInsightsSection) GoldAccent else Color.White)
                                            .background(if (showInsightsSection) GoldAccent.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(16.dp))
                                            .clickable { showInsightsSection = !showInsightsSection },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = "Insights Toggle",
                                            tint = if (showInsightsSection) GoldAccent else TextDarkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Insights",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showInsightsSection) GoldAccent else TextDarkSecondary.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Column 4: Reset DB or Limits toggler (Glassmorphic)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .glassCard(cornerRadius = 16.dp, tint = Color.White)
                                            .clickable { showQuickSeedInfo = !showQuickSeedInfo },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = TextDarkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Limits",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkSecondary.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Category Distribution Area
                        if (showInsightsSection) {
                            item {
                                AestheticAnalyticsCard(
                                    distribution = categoryDistribution,
                                    totalExpense = totalExpense,
                                    formatter = currencyFormatter
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        item {
                            // Search Bar & Filter Section
                            AestheticFilterSection(
                                searchQuery = searchQuery,
                                onSearchChange = { viewModel.updateSearchQuery(it) },
                                selectedType = selectedType,
                                onTypeSelect = { viewModel.selectTypeFilter(it) },
                                selectedCategory = selectedCategory,
                                onCategorySelect = { viewModel.selectCategoryFilter(it) }
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Recent Ledger Header and view all action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Ledger",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    color = TextDarkPrimary
                                )
                                if (expenses.isNotEmpty()) {
                                    Text(
                                        text = "VIEW ALL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkSecondary.copy(alpha = 0.4f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.clickable {
                                            viewModel.updateSearchQuery("")
                                            viewModel.selectTypeFilter("ALL")
                                            viewModel.selectCategoryFilter("ALL")
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Highly performant list items with lazy rendering & item recycling
                        if (expenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, Color(0xFF2D2D2D), RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inbox,
                                            contentDescription = "No receipts",
                                            tint = TextDarkMuted,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Zero matches found",
                                            color = TextDarkSecondary,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Adjust filters to locate ledger notes",
                                            color = TextDarkMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(expenses, key = { it.id }) { expense ->
                                TransactionItemRow(
                                    expense = expense,
                                    formatter = currencyFormatter,
                                    onDelete = { viewModel.deleteExpense(expense.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    "CALENDAR" -> {
                        // 1. Calendar header & month selection controls
                        item {
                            CalendarHeaderSection(
                                currentYear = calendarCurrentYear,
                                currentMonth = calendarCurrentMonth,
                                onPreviousMonth = {
                                    if (calendarCurrentMonth == 0) {
                                        calendarCurrentMonth = 11
                                        calendarCurrentYear -= 1
                                    } else {
                                        calendarCurrentMonth -= 1
                                    }
                                },
                                onNextMonth = {
                                    if (calendarCurrentMonth == 11) {
                                        calendarCurrentMonth = 0
                                        calendarCurrentYear += 1
                                    } else {
                                        calendarCurrentMonth += 1
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 2. Interactive grid showing the days of the month with dynamic indicator highlights
                        item {
                            val daysInMonth = remember(calendarCurrentYear, calendarCurrentMonth) {
                                getDaysInMonth(calendarCurrentYear, calendarCurrentMonth)
                            }
                            
                            // High performance O(1) monthly transactions lookup Map
                            val monthExpensesInfo = remember(calendarCurrentYear, calendarCurrentMonth, expenses) {
                                val infoMap = mutableMapOf<Int, Pair<Boolean, Boolean>>()
                                val cal = java.util.Calendar.getInstance()
                                expenses.forEach { expense ->
                                    cal.timeInMillis = expense.timestamp
                                    val expYear = cal.get(java.util.Calendar.YEAR)
                                    val expMonth = cal.get(java.util.Calendar.MONTH)
                                    if (expYear == calendarCurrentYear && expMonth == calendarCurrentMonth) {
                                        val expDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                                        val isIncome = expense.type == "INCOME"
                                        val isExpense = expense.type == "EXPENSE"
                                        val current = infoMap[expDay] ?: Pair(false, false)
                                        infoMap[expDay] = Pair(
                                            current.first || isIncome,
                                            current.second || isExpense
                                        )
                                    }
                                }
                                infoMap
                            }

                            CalendarGridCard(
                                currentYear = calendarCurrentYear,
                                currentMonth = calendarCurrentMonth,
                                daysInMonth = daysInMonth,
                                monthExpensesInfo = monthExpensesInfo,
                                selectedDate = calendarSelectedDate,
                                onSelectDate = { calendarSelectedDate = it }
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 3. Chosen Day Stats breakdown Card
                        item {
                            val selectedDateStr = remember(calendarSelectedDate) {
                                java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).format(calendarSelectedDate)
                            }
                            
                            val dailyExpensesList = remember(calendarSelectedDate, expenses) {
                                val calSelected = java.util.Calendar.getInstance().apply { time = calendarSelectedDate }
                                val selYear = calSelected.get(java.util.Calendar.YEAR)
                                val selMonth = calSelected.get(java.util.Calendar.MONTH)
                                val selDay = calSelected.get(java.util.Calendar.DAY_OF_MONTH)
                                
                                val calExpense = java.util.Calendar.getInstance()
                                expenses.filter { expense ->
                                    calExpense.timeInMillis = expense.timestamp
                                    calExpense.get(java.util.Calendar.YEAR) == selYear &&
                                    calExpense.get(java.util.Calendar.MONTH) == selMonth &&
                                    calExpense.get(java.util.Calendar.DAY_OF_MONTH) == selDay
                                }
                            }
                            val dailyExpenseTotal = remember(dailyExpensesList) {
                                dailyExpensesList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                            }
                            val dailyIncomeTotal = remember(dailyExpensesList) {
                                dailyExpensesList.filter { it.type == "INCOME" }.sumOf { it.amount }
                            }
                            val dailyNet = dailyIncomeTotal - dailyExpenseTotal

                            CalendarDailyStatsCard(
                                selectedDateStr = selectedDateStr,
                                dailyExpenseTotal = dailyExpenseTotal,
                                dailyIncomeTotal = dailyIncomeTotal,
                                dailyNet = dailyNet,
                                expenseCount = dailyExpensesList.filter { it.type == "EXPENSE" }.size,
                                incomeCount = dailyExpensesList.filter { it.type == "INCOME" }.size,
                                formatter = currencyFormatter
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 4. List Label with toggle controls
                        item {
                            CalendarListHeader(
                                filterByDayOnly = calendarFilterByDayOnly,
                                onToggleFilter = { calendarFilterByDayOnly = !calendarFilterByDayOnly }
                            )
                        }

                        // 5. Sequential Transaction cells using highly performant items() lazy recycling mechanics
                        if (calendarDisplayList.isEmpty()) {
                            item {
                                CalendarEmptyStateBox(filterByDayOnly = calendarFilterByDayOnly)
                            }
                        } else {
                            items(calendarDisplayList, key = { "calendar_${it.id}" }) { expense ->
                                TransactionItemRow(
                                    expense = expense,
                                    formatter = currencyFormatter,
                                    onDelete = { viewModel.deleteExpense(expense.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    "STATS" -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Analytics Lounge",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = TextDarkPrimary,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                AestheticAnalyticsCard(
                                    distribution = categoryDistribution,
                                    totalExpense = totalExpense,
                                    formatter = currencyFormatter
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(cornerRadius = 24.dp, tint = Color.White),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(
                                            text = "BUDGET HEALTH REPORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val percentStr = if (totalIncome > 0) String.format("%.0f%%", (totalExpense / totalIncome) * 100) else "0%"
                                        Text(
                                            text = "You have consumed $percentStr of your positive income streams this period.",
                                            fontSize = 13.sp,
                                            color = TextDarkPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "CARDS" -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Secure Ledger Accounts",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = TextDarkPrimary,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .glassCard(cornerRadius = 28.dp, tint = Color.White)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0x22FFFFFF),
                                                    Color(0x05FFFFFF)
                                                )
                                            ),
                                            shape = RoundedCornerShape(28.dp)
                                        ),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp)
                                    ) {
                                        Column(modifier = Modifier.align(Alignment.TopStart)) {
                                            Text(
                                                text = "OBSIDIAN DEBIT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDarkSecondary,
                                                letterSpacing = 2.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Premium Onyx Member",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = TextDarkPrimary
                                            )
                                        }
                                        Text(
                                            text = "**** **** **** 8850",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            color = TextDarkPrimary,
                                            modifier = Modifier.align(Alignment.CenterStart)
                                        )
                                        Text(
                                            text = "VIP LEDGER",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Serif,
                                            color = GoldAccent,
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "PLANS" -> {
                        item {
                            Text(
                                text = "Active Savings Goals",
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                color = TextDarkPrimary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        items(listOf(
                            Pair("Kyoto Escapade", 4500.0),
                            Pair("Hobby Mechanical KB", 350.0),
                            Pair("Secure Emergency Liquidity", 10000.0)
                        )) { (planName, targetValue) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .glassCard(cornerRadius = 20.dp, tint = GoldAccent),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = planName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                        Text(text = "Target: ${currencyFormatter.format(targetValue)}", fontSize = 12.sp, color = TextDarkSecondary)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        tint = GoldAccent,
                                        contentDescription = "Starred goal"
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp)) // dynamic padding to scroll completely past the floating navigation bar
                }
            }

            // Bottom Navigation Bar with gorgeous Floating Glassmorphic design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .glassCard(cornerRadius = 24.dp, tint = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Tab
                    BottomNavItem(
                        icon = Icons.Default.Home,
                        label = "HOME",
                        isActive = activeTab == "HOME",
                        onClick = { activeTab = "HOME" }
                    )

                    // Calendar Tab
                    BottomNavItem(
                        icon = Icons.Default.DateRange,
                        label = "CALENDAR",
                        isActive = activeTab == "CALENDAR",
                        onClick = { activeTab = "CALENDAR" }
                    )

                    // Stats Tab
                    BottomNavItem(
                        icon = Icons.Default.BarChart,
                        label = "STATS",
                        isActive = activeTab == "STATS",
                        onClick = { activeTab = "STATS" }
                    )

                    // Cards Tab
                    BottomNavItem(
                        icon = Icons.Default.CreditCard,
                        label = "CARDS",
                        isActive = activeTab == "CARDS",
                        onClick = { activeTab = "CARDS" }
                    )

                    // Plans Tab
                    BottomNavItem(
                        icon = Icons.Default.Bookmark,
                        label = "PLANS",
                        isActive = activeTab == "PLANS",
                        onClick = { activeTab = "PLANS" }
                    )
                }
            }
        }

        // Floating Action Button with Translucent Glass Style
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .padding(bottom = 80.dp) // shift upwards to avoid colliding with bottom tab menu
                .windowInsetsPadding(WindowInsets.navigationBars)
                .glassCard(cornerRadius = 16.dp, tint = OffWhitePrimary)
                .testTag("add_expense_fab"),
            containerColor = OffWhitePrimary.copy(alpha = 0.2f),
            contentColor = OffWhitePrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log Transaction",
                modifier = Modifier.size(28.dp)
            )
        }

        // Animated Dialog Box (Bottom Sheet Overlay style)
        if (showAddDialog) {
            AddTransactionOverlay(
                defaultDate = calendarSelectedDate,
                onDismiss = { showAddDialog = false },
                onSave = { title, amount, category, type, note, timestamp ->
                    viewModel.addExpense(title, amount, category, type, note, timestamp)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) TextDarkPrimary else TextDarkSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isActive) TextDarkPrimary else TextDarkSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun AestheticBalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    formatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("balance_card")
            .glassCard(cornerRadius = 32.dp, tint = Color.White),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL SPENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkSecondary.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹",
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextDarkPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                )
                val formatterVal = String.format(Locale.US, "%,.2f", expense)
                Text(
                    text = formatterVal,
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    letterSpacing = (-1.5).sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic budget status badge with micro dots
            val isSafe = expense <= income
            val dotColor = if (isSafe) EmeraldAccent else RedAccent
            val statusLabel = if (isSafe) "12% less than August" else "Exceeded budget limit"

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .glassCard(cornerRadius = 20.dp, borderWidth = 0.5.dp, tint = dotColor, alphaMultiplier = 0.6f)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkPrimary.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Dividers and micro analytics side-by-side
            HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "TOTAL INCOME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatter.format(income),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldAccent
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(Color(0xFF2D2D2D))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET LEDGER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatter.format(balance),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) GoldAccent else RedAccent
                    )
                }
            }
        }
    }
}

@Composable
fun AestheticAnalyticsCard(
    distribution: Map<String, Double>,
    totalExpense: Double,
    formatter: NumberFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp, tint = GoldAccent),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "SPENDING DISTRIBUTION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkSecondary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (distribution.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No recorded expense categories yet.",
                        color = TextDarkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Canvas Ring Chart
                    Box(
                        modifier = Modifier
                            .size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(68.dp)) {
                            var startAngle = -90f
                            distribution.forEach { (categoryId, percentage) ->
                                val category = CategoryHelper.getCategory(categoryId)
                                val sweepAngle = (percentage * 360f).toFloat()

                                drawArc(
                                    color = category.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SPENT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                            val kFormat = if (totalExpense >= 1000) {
                                String.format("%.1fk", totalExpense / 1000.0)
                            } else {
                                String.format("%.0f", totalExpense)
                            }
                            Text(
                                text = "₹$kFormat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDarkPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Legends list (max 3, others grouped or scrollable)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Take top 3 spent items
                        val sortedEntries = distribution.entries.sortedByDescending { it.value }.take(3)
                        sortedEntries.forEach { entry ->
                            val category = CategoryHelper.getCategory(entry.key)
                            val percentText = String.format("%.0f%%", entry.value * 100)

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(category.color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = category.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDarkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = percentText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                // Tiny progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceElevated)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(entry.value.toFloat())
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(category.color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AestheticFilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Aesthetic Glass-wrapped Search field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 14.dp, tint = Color.White)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                placeholder = { Text("Search transactions...", color = TextDarkMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextDarkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextDarkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextDarkPrimary,
                    unfocusedTextColor = TextDarkPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Type filter rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (id, label) ->
                val isActive = selectedType == id
                val activeBg = if (id == "EXPENSE") ExpenseRed else if (id == "INCOME") IncomeGreen else MintPrimary
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTypeSelect(id) }
                        .then(
                            if (isActive) {
                                Modifier.background(activeBg.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                    .border(1.5.dp, activeBg, RoundedCornerShape(10.dp))
                            } else {
                                Modifier.glassCard(cornerRadius = 10.dp, tint = Color.White, alphaMultiplier = 0.5f)
                            }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) activeBg else TextDarkSecondary
                    )
                }
            }
        }

        // Category filter carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryBadge(
                    label = "All Categories",
                    isActive = selectedCategory == "ALL",
                    onClick = { onCategorySelect("ALL") }
                )
            }
            items(CategoryHelper.categories) { cat ->
                CategoryBadge(
                    label = cat.id,
                    isActive = selectedCategory == cat.id,
                    activeColor = cat.color,
                    onClick = { onCategorySelect(cat.id) }
                )
            }
        }
    }
}

@Composable
fun CategoryBadge(
    label: String,
    isActive: Boolean,
    activeColor: Color = MintPrimary,
    onClick: () -> Unit
) {
    val cornerShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(cornerShape)
            .clickable { onClick() }
            .then(
                if (isActive) {
                    Modifier.background(activeColor.copy(alpha = 0.15f), cornerShape)
                } else {
                    Modifier
                }
            )
            .glassCard(cornerRadius = 20.dp, tint = if (isActive) activeColor else Color.White, alphaMultiplier = if (isActive) 1.2f else 0.4f)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) activeColor else TextDarkSecondary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemRow(
    expense: Expense,
    formatter: NumberFormat,
    onDelete: () -> Unit
) {
    val category = remember(expense.category) { CategoryHelper.getCategory(expense.category) }
    val isExpense = expense.type == "EXPENSE"

    val rowTint = if (isExpense) Color.White else GoldAccent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp, tint = rowTint, alphaMultiplier = 0.7f)
            .padding(14.dp)
            .testTag("transaction_row_${expense.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle with background opacity matching html style
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2D2D2D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = expense.category,
                tint = if (isExpense) TextDarkPrimary else GoldAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles block
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = expense.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val dateStr = remember(expense.timestamp) {
                // Today vs others
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)
                sdf.format(Date(expense.timestamp))
            }
            Text(
                text = if (expense.note.isNotEmpty()) "${expense.note} • $dateStr" else "${category.displayName} • $dateStr",
                fontSize = 11.sp,
                color = TextDarkSecondary.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Amount & Date / Delete block
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val formattedAmount = formatter.format(expense.amount)
                Text(
                    text = if (isExpense) "-$formattedAmount" else "+$formattedAmount",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) TextDarkPrimary else GoldAccent
                )
                Text(
                    text = category.displayName.uppercase(Locale.US),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkSecondary.copy(alpha = 0.4f),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Micro delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2D2D))
                    .testTag("delete_button_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = TextDarkSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionOverlay(
    defaultDate: java.util.Date = java.util.Date(),
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, Long) -> Unit
) {
    // Input parameters
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // or "INCOME"
    var selectedCategory by remember { mutableStateOf("Food") }

    // Dropdown items filtered by type automatically for convenient logging
    val filteredCategoriesForSelection = remember(selectedType) {
        if (selectedType == "INCOME") {
            CategoryHelper.categories.filter { it.id == "Salary" || it.id == "Investment" || it.id == "Others" }
        } else {
            CategoryHelper.categories.filter { it.id != "Salary" }
        }
    }

    // Reactively update selected category if it doesn't match selected filter type
    LaunchedEffect(selectedType) {
        if (selectedType == "INCOME" && selectedCategory != "Salary" && selectedCategory != "Investment" && selectedCategory != "Others") {
            selectedCategory = "Salary"
        } else if (selectedType == "EXPENSE" && selectedCategory == "Salary") {
            selectedCategory = "Food"
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val handleDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    // Modal Sheet Overlay Custom UI Box (Frosted Glass Mask)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD8050508)) // Safe elegant dark glass tint
            .drawBehind {
                // High-fidelity micro-fiber frosted glass simulation lines with efficient spacing
                val strokeWidth = 1f
                val spacing = 140f
                var offset = 0f
                val limit = size.width + size.height
                while (offset < limit) {
                    drawLine(
                        color = Color(0x0AFFFFFF), // ultra subtle premium refraction line
                        start = Offset(offset, 0f),
                        end = Offset(0f, offset),
                        strokeWidth = strokeWidth
                    )
                    offset += spacing
                }
            }
            .clickable {
                handleDismiss()
            }, // Dismiss when tapping outer mask
        contentAlignment = Alignment.BottomCenter
    ) {
        // Inner card body (Breathtaking glass bottom sheet)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .glassCard(cornerRadius = 28.dp, tint = Color.White, alphaMultiplier = 1.3f)
                .clickable(enabled = false) {} // block click throughs
                .windowInsetsPadding(WindowInsets.ime) // adjust for keyboard sliding
                .navigationBarsPadding()
                .padding(22.dp)
                .testTag("add_dialog_body")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header drag indicator bar look
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Transaction",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary
                    )
                    IconButton(onClick = handleDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = TextDarkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dual Type segment selector with neat layout border
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .glassCard(cornerRadius = 14.dp, tint = Color.White, alphaMultiplier = 0.6f)
                        .padding(4.dp)
                ) {
                    listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (typeId, label) ->
                        val isSelected = selectedType == typeId
                        val activeTint = if (typeId == "INCOME") MintPrimary else ExpenseRed

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedType = typeId }
                                .background(
                                    if (isSelected) activeTint.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) activeTint.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) activeTint else TextDarkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Large Amount input layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp, tint = Color.White, alphaMultiplier = 0.3f)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "AMOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextDarkMuted)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDarkPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = amountText,
                            onValueChange = { input ->
                                // Clean floating point inputs only
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amountText = input
                                }
                            },
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 220.dp)
                                .testTag("amount_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextDarkPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.SansSerif
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title / Merchant", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x1F2A2A2A),
                        unfocusedContainerColor = Color(0x0F1A1A1A),
                        focusedBorderColor = MintPrimary,
                        unfocusedBorderColor = Color(0x1F2D2D2D),
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x1F2A2A2A),
                        unfocusedContainerColor = Color(0x0F1A1A1A),
                        focusedBorderColor = MintPrimary,
                        unfocusedBorderColor = Color(0x1F2D2D2D),
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Category Selection flow
                Text(
                    text = "SELECT CATEGORY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkMuted,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable category icon row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCategoriesForSelection) { category ->
                        val isSelected = selectedCategory == category.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = category.id }
                                .then(
                                    if (isSelected) {
                                        Modifier.background(category.color.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                            .border(1.5.dp, category.color, RoundedCornerShape(14.dp))
                                    } else {
                                        Modifier.glassCard(cornerRadius = 14.dp, tint = Color.White, alphaMultiplier = 0.4f)
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.displayName,
                                    tint = if (isSelected) category.color else TextDarkSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category.id,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) category.color else TextDarkSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                val saveButtonEnabled = titleText.trim().isNotEmpty() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0

                // Selected Date display row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 14.dp, tint = Color.White)
                        .background(Color(0x11FFFFFF), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ledger Date",
                            fontSize = 12.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val formattedDate = remember(defaultDate) {
                        val sdf = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
                        sdf.format(defaultDate)
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (titleText.trim().isNotEmpty() && amount > 0.0) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSave(titleText.trim(), amount, selectedCategory, selectedType, noteText.trim(), defaultDate.time)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_transaction_button"),
                    enabled = saveButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintPrimary,
                        contentColor = Color(0xFF0C0F16),
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = TextDarkMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Confirm Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeaderSection(
    currentYear: Int,
    currentMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = remember(currentMonth) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.MONTH, currentMonth)
        }
        java.text.SimpleDateFormat("MMMM", java.util.Locale.US).format(cal.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Calendar Ledger",
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            color = TextDarkPrimary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = TextDarkPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "$monthName $currentYear",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                modifier = Modifier.widthIn(min = 90.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = TextDarkPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CalendarGridCard(
    currentYear: Int,
    currentMonth: Int,
    daysInMonth: List<Int?>,
    monthExpensesInfo: Map<Int, Pair<Boolean, Boolean>>,
    selectedDate: java.util.Date,
    onSelectDate: (java.util.Date) -> Unit
) {
    val selectedCal = remember(selectedDate) {
        java.util.Calendar.getInstance().apply { time = selectedDate }
    }
    val sYear = selectedCal.get(java.util.Calendar.YEAR)
    val sMonth = selectedCal.get(java.util.Calendar.MONTH)
    val sDay = selectedCal.get(java.util.Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp, tint = Color.White),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Weekday headings
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { dayLetter ->
                    Text(
                        text = dayLetter,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalCells = daysInMonth.size
            var cellIndex = 0
            while (cellIndex < totalCells) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0..6) {
                        if (cellIndex < totalCells) {
                            val day = daysInMonth[cellIndex]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isSelected = currentYear == sYear && currentMonth == sMonth && day == sDay
                                    val dayInfo = monthExpensesInfo[day]
                                    val hasIncome = dayInfo?.first == true
                                    val hasExpense = dayInfo?.second == true

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .clickable {
                                                val newDate = java.util.Calendar.getInstance().apply {
                                                    set(java.util.Calendar.YEAR, currentYear)
                                                    set(java.util.Calendar.MONTH, currentMonth)
                                                    set(java.util.Calendar.DAY_OF_MONTH, day)
                                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                    set(java.util.Calendar.MINUTE, 0)
                                                    set(java.util.Calendar.SECOND, 0)
                                                    set(java.util.Calendar.MILLISECOND, 0)
                                                }.time
                                                onSelectDate(newDate)
                                            }
                                            .then(
                                                if (isSelected) Modifier.background(GoldAccent) else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else TextDarkPrimary
                                            )

                                            if (!isSelected && (hasIncome || hasExpense)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    if (hasIncome) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(EmeraldAccent)
                                                        )
                                                    }
                                                    if (hasExpense) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(RedAccent)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            cellIndex++
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun CalendarDailyStatsCard(
    selectedDateStr: String,
    dailyExpenseTotal: Double,
    dailyIncomeTotal: Double,
    dailyNet: Double,
    expenseCount: Int,
    incomeCount: Int,
    formatter: java.text.NumberFormat
) {
    Text(
        text = "Daily Tracker • $selectedDateStr",
        fontSize = 15.sp,
        fontFamily = FontFamily.Serif,
        color = TextDarkSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp, tint = Color.White),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY SPENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatter.format(dailyExpenseTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyExpenseTotal > 0) RedAccent else TextDarkPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2D2D2D))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DAILY INCOME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatter.format(dailyIncomeTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyIncomeTotal > 0) EmeraldAccent else TextDarkPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF2D2D2D))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "NET BAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = (if (dailyNet >= 0) "+" else "") + formatter.format(dailyNet),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dailyNet >= 0) EmeraldAccent else RedAccent
                    )
                }
            }

            if (expenseCount > 0 || incomeCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Registered $expenseCount expenses and $incomeCount income records on this day.",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
        }
    }
}

@Composable
fun CalendarListHeader(
    filterByDayOnly: Boolean,
    onToggleFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (filterByDayOnly) "Selected Date Records" else "All Past Expenses",
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            color = TextDarkPrimary
        )

        Text(
            text = if (filterByDayOnly) "SHOW PAST HISTORY" else "SHOW SELECTED DATE ONLY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .clickable { onToggleFilter() }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
fun CalendarEmptyStateBox(filterByDayOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131313))
            .border(1.dp, Color(0xFF2D2D2D), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = "Empty list",
                tint = TextDarkMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (filterByDayOnly) "No transaction on this date" else "No overall history recorded",
                color = TextDarkSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helpers for Calendar:
fun getDaysInMonth(year: Int, month: Int): List<Int?> {
    val cal = java.util.Calendar.getInstance()
    cal.clear()
    cal.set(java.util.Calendar.YEAR, year)
    cal.set(java.util.Calendar.MONTH, month)
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)

    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
    
    // Offset Sunday (1 in java.util.Calendar) to align grid
    val offset = firstDayOfWeek - java.util.Calendar.SUNDAY

    val days = mutableListOf<Int?>()
    for (i in 0 until offset) {
        days.add(null)
    }

    for (day in 1..maxDays) {
        days.add(day)
    }
    return days
}

fun isSameDayDate(date1: java.util.Date, date2: java.util.Date): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
    val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
           cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
}

fun isSameDay(timestamp: Long, date: java.util.Date): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val cal2 = java.util.Calendar.getInstance().apply { time = date }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
           cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
}
