package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChildProfile
import com.example.data.model.DailyRecord
import com.example.data.model.HabitItem
import com.example.data.model.Rule
import com.example.ui.theme.*
import com.example.ui.viewmodel.GrowthViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class GrowthScreen {
    MAIN,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGrowthScreen(
    viewModel: GrowthViewModel,
    modifier: Modifier = Modifier
) {
    val childProfile by viewModel.childProfile.collectAsStateWithLifecycle()
    val activeHabits by viewModel.activeHabits.collectAsStateWithLifecycle()
    val allRecords by viewModel.allCompletedRecords.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val totalFlowers by viewModel.totalFlowersCount.collectAsStateWithLifecycle()

    val selectedChildId by viewModel.selectedChildId.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val webDavSettings by viewModel.webDavSettings.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMsg by viewModel.syncStatusMsg.collectAsStateWithLifecycle()

    val activeWeekDates by viewModel.activeWeekDates.collectAsStateWithLifecycle()
    val selectedWeekMonday by viewModel.selectedWeekMonday.collectAsStateWithLifecycle()
    val selectedStatsMonth by viewModel.selectedStatsMonth.collectAsStateWithLifecycle()

    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showRulesEditDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showChildrenManagementDialog by remember { mutableStateOf(false) }

    // Clicked habit states for edit/delete dialog
    var habitToEdit by remember { mutableStateOf<HabitItem?>(null) }
    var showHabitActionsDialog by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf(GrowthScreen.MAIN) }

    // Precalculate completed record lookup map: "habitId_date" -> Boolean
    val completedKeysMap = remember(allRecords) {
        allRecords.associate { "${it.habitId}_${it.date}" to true }
    }

    Scaffold(
        topBar = {
            if (currentScreen == GrowthScreen.MAIN) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "儿 童 成 长 表 现 记 录",
                            fontWeight = FontWeight.Bold,
                            color = Rose800,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Rose200.copy(alpha = 0.5f))
                                .clickable { viewModel.completeAllToday() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🌸 一键今日", fontSize = 12.sp, color = Rose800, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { currentScreen = GrowthScreen.SETTINGS }) {
                            Icon(Icons.Default.Settings, contentDescription = "设置与管理", tint = Rose600)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Rose50
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "设 置 与 管 理",
                            fontWeight = FontWeight.Bold,
                            color = Rose800,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { currentScreen = GrowthScreen.MAIN }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回主页", tint = Rose600)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Rose50
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                )
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == GrowthScreen.SETTINGS) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "ScreenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                GrowthScreen.MAIN -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Child Info & Big Flower Scoreboard card
                        item {
                            ChildProfileCard(
                                profile = childProfile,
                                totalFlowers = totalFlowers,
                                onEditClick = { currentScreen = GrowthScreen.SETTINGS },
                                onManageClick = { currentScreen = GrowthScreen.SETTINGS }
                            )
                        }

                        // 1.5 Quick Action Banner for today's habits
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Rose50),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.2.dp, Rose100),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.completeAllToday() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("🌸", fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = "一键标上今日所有花朵",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Rose800
                                            )
                                            Text(
                                                text = "今天表现很棒？快来帮全部习惯浇水开花吧！",
                                                fontSize = 11.sp,
                                                color = Slate400
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "一键打卡",
                                        tint = Rose600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 2. Weekly Daily Records Grid (Core)
                        item {
                            WeeklyRecordsGrid(
                                activeHabits = activeHabits,
                                weekDates = activeWeekDates,
                                selectedMonday = selectedWeekMonday,
                                completedKeysMap = completedKeysMap,
                                onCellToggle = { habitId, date, completed ->
                                    viewModel.toggleDailyRecord(habitId, date, completed)
                                },
                                onHabitClick = { habit ->
                                    habitToEdit = habit
                                    showHabitActionsDialog = true
                                },
                                onPrevWeek = { viewModel.selectPreviousWeek() },
                                onNextWeek = { viewModel.selectNextWeek() },
                                onCurrentWeek = { viewModel.selectCurrentWeek() },
                                onAddHabitClick = { showAddHabitDialog = true }
                            )
                        }

                        // 3. Data Statistics Module (Weekly / Monthly breakdown)
                        item {
                            StatisticsCard(
                                activeHabits = activeHabits,
                                allRecords = allRecords,
                                activeWeekDates = activeWeekDates,
                                selectedStatsMonth = selectedStatsMonth,
                                onPrevMonth = { viewModel.selectPreviousStatsMonth() },
                                onNextMonth = { viewModel.selectNextStatsMonth() },
                                onCurrentMonth = { viewModel.selectCurrentStatsMonth() }
                            )
                        }

                        // A tiny friendly footer displaying soft cartoon vibes
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "让好习惯像花朵一样生机勃勃 💐",
                                    color = EarthyText.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                GrowthScreen.SETTINGS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Child Selection & Management Card
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                                border = BorderStroke(1.2.dp, Rose100),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .height(18.dp)
                                                .background(FlowerCoral, RoundedCornerShape(50.dp))
                                        )
                                        Text(
                                            text = "孩子管理与切换",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate700
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "当前孩子: ${childProfile?.name ?: "未设置"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Rose800
                                            )
                                            Text(
                                                text = "成长目标: ${childProfile?.goal ?: "无"}",
                                                fontSize = 12.sp,
                                                color = Slate400
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { showProfileEditDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("编辑目标", color = WarmIvory, fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { showChildrenManagementDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("切换/新建", color = WarmIvory, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Habit Project Management Card
                        item {
                            HabitManagementCard(
                                activeHabits = activeHabits,
                                onAddHabit = { viewModel.addHabit(it) },
                                onEditHabit = { habit ->
                                    habitToEdit = habit
                                    showHabitActionsDialog = true
                                }
                            )
                        }

                        // 3. Reward/Punishment Rules Card
                        item {
                            RulesCard(
                                rules = rules,
                                onEditClick = { showRulesEditDialog = true }
                            )
                        }

                        // 4. WebDAV Cloud Synchronization Card
                        item {
                            WebDavSyncCard(
                                settings = webDavSettings,
                                isSyncing = isSyncing,
                                syncStatusMsg = syncStatusMsg,
                                onSaveSettings = { viewModel.saveWebDavSettings(it) },
                                onBackup = { viewModel.performWebDavBackup() },
                                onRestore = { viewModel.performWebDavRestore() },
                                onSmartSync = { viewModel.performWebDavSmartSync() }
                            )
                        }

                        // Friendly Hint Footer
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "设置已自动保存 ✏️",
                                    color = EarthyText.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Children Profiles Management Dialog
    if (showChildrenManagementDialog) {
        var newKidName by remember { mutableStateOf("") }
        var newKidGoal by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showChildrenManagementDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.5.dp, Rose200),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "👧 切换 / 管理宝贝档案",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Rose800
                    )

                    Text(
                        text = "已有宝贝成员:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        allProfiles.forEach { profile ->
                            val isActive = profile.id == selectedChildId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) Rose50 else Color.White)
                                    .border(
                                        width = if (isActive) 1.5.dp else 0.8.dp,
                                        color = if (isActive) Rose200 else SoftGreyBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.selectChild(profile.id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "👦", fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = profile.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isActive) Rose800 else EarthyText
                                        )
                                        if (profile.goal.isNotBlank()) {
                                            Text(
                                                text = profile.goal,
                                                fontSize = 11.sp,
                                                color = Slate400,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isActive) {
                                        Text(
                                            text = "当前使用 🌟",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Rose600
                                        )
                                    }

                                    if (allProfiles.size > 1) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteChild(profile.id)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除该宝贝",
                                                tint = Color.Red.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SoftGreyBorder, thickness = 1.dp)

                    Text(
                        text = "✨ 添加新宝贝档案:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = newKidName,
                        onValueChange = { newKidName = it },
                        label = { Text("新宝贝姓名", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Rose600,
                            focusedLabelColor = Rose600
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newKidGoal,
                        onValueChange = { newKidGoal = it },
                        label = { Text("成长心愿 (可选)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Rose600,
                            focusedLabelColor = Rose600
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newKidName.isNotBlank()) {
                                viewModel.addChild(newKidName, newKidGoal)
                                newKidName = ""
                                newKidGoal = ""
                            }
                        },
                        enabled = newKidName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("添加并切换至该宝贝 🍼", color = WarmIvory, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showChildrenManagementDialog = false }) {
                            Text("关闭", color = Rose800, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Edit Child Profile Dialog
    if (showProfileEditDialog) {
        val currentProfile = childProfile ?: ChildProfile(name = "")
        var tempName by remember { mutableStateOf(currentProfile.name) }
        var tempGoal by remember { mutableStateOf(currentProfile.goal) }

        Dialog(onDismissRequest = { showProfileEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.2.dp, Rose100),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "✏️ 编辑孩子档案",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("孩子姓名") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowerCoral,
                            focusedLabelColor = FlowerCoral
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempGoal,
                        onValueChange = { tempGoal = it },
                        label = { Text("成长心愿 / 目标") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowerCoral,
                            focusedLabelColor = FlowerCoral
                        ),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showProfileEditDialog = false }) {
                            Text("取消", color = EarthyText.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateProfile(tempName, tempGoal)
                                showProfileEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral)
                        ) {
                            Text("保存", color = WarmIvory)
                        }
                    }
                }
            }
        }
    }

    // Edit Rules Dialog
    if (showRulesEditDialog) {
        val currentRules = rules ?: Rule()
        var tempReward by remember { mutableStateOf(currentRules.rewardRule) }
        var tempPunish by remember { mutableStateOf(currentRules.punishRule) }

        Dialog(onDismissRequest = { showRulesEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.2.dp, Sky100),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📒 修改奖惩规则",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = tempReward,
                        onValueChange = { tempReward = it },
                        label = { Text("🎁 奖励规则") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempPunish,
                        onValueChange = { tempPunish = it },
                        label = { Text("⚠️ 惩罚规章") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRulesEditDialog = false }) {
                            Text("取消", color = EarthyText.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateRules(tempReward, tempPunish)
                                showRulesEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Sky600)
                        ) {
                            Text("保存", color = WarmIvory)
                        }
                    }
                }
            }
        }
    }

    // Add Custom Habit Dialog
    if (showAddHabitDialog) {
        var tempHabitName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddHabitDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.2.dp, Rose100),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⭐ 新增习惯项目",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = tempHabitName,
                        onValueChange = { tempHabitName = it },
                        label = { Text("习惯名称 (如: 课外阅读)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowerCoral,
                            focusedLabelColor = FlowerCoral
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddHabitDialog = false }) {
                            Text("取消", color = EarthyText.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempHabitName.isNotBlank()) {
                                    viewModel.addHabit(tempHabitName)
                                    showAddHabitDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral)
                        ) {
                            Text("添加", color = WarmIvory)
                        }
                    }
                }
            }
        }
    }

    // Habit Rename / Delete Dialog
    if (showHabitActionsDialog && habitToEdit != null) {
        val habit = habitToEdit!!
        var tempName by remember { mutableStateOf(habit.name) }

        Dialog(onDismissRequest = { showHabitActionsDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.2.dp, Rose100),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚙️ 管理习惯: ${habit.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("修改名称") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlowerCoral,
                            focusedLabelColor = FlowerCoral
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button (Soft delete)
                        TextButton(
                            onClick = {
                                viewModel.deleteHabit(habit.id)
                                showHabitActionsDialog = false
                                habitToEdit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = FlowerCoral)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除项目")
                        }

                        Row {
                            TextButton(onClick = {
                                showHabitActionsDialog = false
                                habitToEdit = null
                            }) {
                                Text("取消", color = EarthyText.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (tempName.isNotBlank()) {
                                        viewModel.updateHabit(habit.copy(name = tempName.trim()))
                                        showHabitActionsDialog = false
                                        habitToEdit = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                            ) {
                                Text("保存", color = WarmIvory)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------ PRIVATE UI COMPONENTS ------

// 1. Header Card with Kid's Profile and big flower score badge
@Composable
fun ChildProfileCard(
    profile: ChildProfile?,
    totalFlowers: Int,
    onEditClick: () -> Unit,
    onManageClick: () -> Unit
) {
    val currentProfile = profile ?: ChildProfile(name = "")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Rose50),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, Rose100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar circular box: bg White, border 4dp Rose100/Rose200
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .border(3.1.dp, Rose200, CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👦", fontSize = 24.sp)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentProfile.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose800
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改姓名",
                            tint = Rose600.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onEditClick() }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Switch/manage badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Rose200.copy(alpha = 0.5f))
                                .clickable { onManageClick() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "切换 🔄",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose800
                            )
                        }
                    }

                    Text(
                        text = if (currentProfile.goal.isNotBlank()) "目标: ${currentProfile.goal}" else "点击设定你的首要习惯目标吧 🌱",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FlowerCoral,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Scoreboard Badge: White rounded Box with Rose border
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.2.dp, Rose100, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LittleRedFlower(modifier = Modifier.size(22.dp))
                    Text(
                        text = "$totalFlowers",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Rose600
                    )
                }
            }
        }
    }
}

// 2. Weekly Records Grid (The Core Element)
@Composable
fun WeeklyRecordsGrid(
    activeHabits: List<HabitItem>,
    weekDates: List<LocalDate>,
    selectedMonday: LocalDate,
    completedKeysMap: Map<String, Boolean>,
    onCellToggle: (Int, LocalDate, Boolean) -> Unit,
    onHabitClick: (HabitItem) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onAddHabitClick: () -> Unit
) {
    val mondayFormatted = selectedMonday.format(DateTimeFormatter.ofPattern("M月d日"))
    val sundayFormatted = selectedMonday.plusDays(6).format(DateTimeFormatter.ofPattern("M月d日"))

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, Rose100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Section Header with vertical accent badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(18.dp)
                            .background(FlowerCoral, RoundedCornerShape(50.dp))
                    )
                    Text(
                        text = "本周表现记录",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Slate700
                    )
                }
                Text(
                    text = "$mondayFormatted - $sundayFormatted",
                    fontSize = 11.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }

            // Week Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevWeek) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "上一周", tint = Slate700)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onCurrentWeek() }
                ) {
                    Text(
                        text = "📆 成长记录表 (点击返回本周)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                    Text(
                        text = "周视图记录器",
                        fontSize = 11.sp,
                        color = Rose600,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onNextWeek) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "下一周", tint = Slate700)
                }
            }

            if (activeHabits.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "目前还没有习惯项目哦，快来添加一个吧！",
                        color = EarthyText.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = onAddHabitClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral)
                    ) {
                        Text("➕ 添加习惯", color = WarmIvory)
                    }
                }
            } else {
                // Table
                // Header (Mon ~ Sun) with soft balance coloring
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Rose50.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "习惯项目",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Rose600,
                        modifier = Modifier.weight(2.2f),
                        textAlign = TextAlign.Center
                    )

                    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                    weekDates.forEachIndexed { idx, date ->
                        val isToday = date == LocalDate.now()
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = weekLabels[idx],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isToday -> Rose600
                                    date.dayOfWeek.value >= 6 -> Sky600
                                    else -> Slate700
                                }
                            )
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("d")),
                                fontSize = 9.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Rose600 else Slate400
                            )
                        }
                    }
                }

                // Table Rows
                activeHabits.forEach { habit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Habit Name: clicking triggers renaming/delete with soft pink accent background
                        Row(
                            modifier = Modifier
                                .weight(2.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Rose50.copy(alpha = 0.2f))
                                .clickable { onHabitClick(habit) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = habit.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Mon to Sun checkmarks
                        weekDates.forEach { date ->
                            val key = "${habit.id}_${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                            val isChecked = completedKeysMap[key] == true
                            val isToday = date == LocalDate.now()

                            // Outer cell containing ring details
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isToday) Modifier
                                            .background(Rose50.copy(alpha = 0.4f))
                                            .border(1.2.dp, Rose200, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        onCellToggle(habit.id, date, !isChecked)
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    // Complete states: custom smiley flower
                                    LittleRedFlower(modifier = Modifier.size(24.dp))
                                } else {
                                    // Incomplete state: quiet dashed pink circle outline
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(DottedFlowerBg, CircleShape)
                                            .border(
                                                width = 1.1.dp,
                                                color = FlowerCoral.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = SoftGreyBorder, thickness = 0.8.dp)
                }
            }
        }
    }
}

// 3. Habit Project Management Card
@Composable
fun HabitManagementCard(
    activeHabits: List<HabitItem>,
    onAddHabit: (String) -> Unit,
    onEditHabit: (HabitItem) -> Unit
) {
    var newHabitName by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, Rose100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💡 习惯项目管理",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )

            // Inline add tool
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    placeholder = { Text("例: 早晚刷牙 / 整理玩具", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowerCoral,
                        unfocusedBorderColor = Rose100
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newHabitName.isNotBlank()) {
                            onAddHabit(newHabitName)
                            newHabitName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("添加", color = WarmIvory, fontWeight = FontWeight.Bold)
                }
            }

            // Quick list chips
            if (activeHabits.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeHabits.forEach { habit ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(Rose50)
                                .border(1.2.dp, Rose100, RoundedCornerShape(50.dp))
                                .clickable { onEditHabit(habit) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = habit.name,
                                fontSize = 12.sp,
                                color = EarthyText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = FlowerCoral,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom FlowRow alternative since Compose standard FlowRow is sometimes unstable in compile setups without experimental flags
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layouts = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentWidth = 0
        val horizontalGap = 8.dp.roundToPx()
        val verticalGap = 8.dp.roundToPx()

        placeables.forEach { placeable ->
            if (currentWidth + placeable.width > constraints.maxWidth) {
                layouts.add(currentLine)
                currentLine = mutableListOf(placeable)
                currentWidth = placeable.width + horizontalGap
            } else {
                currentLine.add(placeable)
                currentWidth += placeable.width + horizontalGap
            }
        }
        if (currentLine.isNotEmpty()) {
            layouts.add(currentLine)
        }

        var totalHeight = 0
        layouts.forEachIndexed { index, list ->
            val maxHeight = list.maxOf { it.height }
            totalHeight += maxHeight
            if (index < layouts.size - 1) totalHeight += verticalGap
        }

        layout(constraints.maxWidth, totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            layouts.forEach { line ->
                var x = 0
                val maxHeight = line.maxOf { it.height }
                line.forEach { placeable ->
                    placeable.placeRelative(x, y + (maxHeight - placeable.height) / 2)
                    x += placeable.width + horizontalGap
                }
                y += maxHeight + verticalGap
            }
        }
    }
}

// 4. Data Statistics Module
@Composable
fun StatisticsCard(
    activeHabits: List<HabitItem>,
    allRecords: List<DailyRecord>,
    activeWeekDates: List<LocalDate>,
    selectedStatsMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Week, 1 = Month

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, Sky100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 行为数据统计",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )

                // Tiny tabs for Week vs Month
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose50)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == 0) Sky600 else Color.Transparent)
                            .clickable { activeTab = 0 }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "本周",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) WarmIvory else Slate700
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == 1) Sky600 else Color.Transparent)
                            .clickable { activeTab = 1 }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "月度",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) WarmIvory else Slate700
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "statsTypeTransition"
            ) { tab ->
                if (tab == 0) {
                    // Weekly Statistics
                    val weekStartStr = activeWeekDates.firstOrNull()?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""
                    val weekEndStr = activeWeekDates.lastOrNull()?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""

                    val weekRecords = remember(allRecords, weekStartStr, weekEndStr) {
                        allRecords.filter { it.date in weekStartStr..weekEndStr }
                    }

                    val weekTotalFlowers = weekRecords.count { it.isCompleted }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Sky50, RoundedCornerShape(12.dp))
                                .border(1.2.dp, Sky100, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✨ 本周累积获得红花:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Sky700
                                )
                                Text(
                                    "$weekTotalFlowers 朵",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Sky700
                                )
                            }
                        }

                        if (activeHabits.isEmpty()) {
                            Text(
                                "暂无数据",
                                fontSize = 12.sp,
                                color = EarthyText.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            activeHabits.forEach { habit ->
                                val completedCount = weekRecords.count { it.habitId == habit.id && it.isCompleted }
                                val percent = completedCount / 7f

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = habit.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate700
                                        )
                                        Text(
                                            text = "完成 $completedCount/7天",
                                            fontSize = 11.sp,
                                            color = Slate700,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        color = Sky400,
                                        trackColor = SoftGreyBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Monthly statistics
                    val monthHeader = selectedStatsMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月"))
                    val startOfMonth = selectedStatsMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val endOfMonth = selectedStatsMonth.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)

                    val monthRecords = remember(allRecords, startOfMonth, endOfMonth) {
                        allRecords.filter { it.date in startOfMonth..endOfMonth }
                    }

                    val totalDaysInMonth = selectedStatsMonth.lengthOfMonth()
                    val monthTotalFlowers = monthRecords.count { it.isCompleted }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Month switch tool
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Rose50, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上月", tint = Slate700)
                            }

                            Text(
                                text = monthHeader,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.clickable { onCurrentMonth() }
                            )

                            IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下月", tint = Slate700)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Sky50, RoundedCornerShape(12.dp))
                                .border(1.2.dp, Sky100, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✨ 本月累积获得红花:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Sky700
                                )
                                Text(
                                    "$monthTotalFlowers 朵",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Sky700
                                )
                            }
                        }

                        if (activeHabits.isEmpty()) {
                            Text(
                                "暂无数据",
                                fontSize = 12.sp,
                                color = EarthyText.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            activeHabits.forEach { habit ->
                                val completedCount = monthRecords.count { it.habitId == habit.id && it.isCompleted }
                                val percent = completedCount.toFloat() / totalDaysInMonth.toFloat()

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = habit.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate700
                                        )
                                        Text(
                                            text = "完成 $completedCount/${totalDaysInMonth}天",
                                            fontSize = 11.sp,
                                            color = Slate700,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { percent },
                                        color = Sky400,
                                        trackColor = SoftGreyBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
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

// 5. Reward/Punishment Rules Card
@Composable
fun RulesCard(
    rules: Rule?,
    onEditClick: () -> Unit
) {
    val currentRules = rules ?: Rule()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.2.dp, Amber100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 我们的奖惩规章",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )

                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowerCoral),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "修改规则", tint = WarmIvory, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("修改", color = WarmIvory, fontSize = 11.sp)
                }
            }

            // Reward Rules Pane with beautiful amber pastel palette
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Amber50, RoundedCornerShape(16.dp))
                    .border(1.2.dp, Amber100.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🎁 红花奖励兑换:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber800
                )
                Text(
                    text = currentRules.rewardRule,
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 18.sp
                )
            }

            // Punishment Rules Pane with beautiful soft light rose colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Rose50, RoundedCornerShape(16.dp))
                    .border(1.2.dp, Rose100, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "⚠️ 行为纠正规章:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Rose800
                )
                Text(
                    text = currentRules.punishRule,
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// 6. WebDAV Cloud Sync Card
@Composable
fun WebDavSyncCard(
    settings: com.example.data.sync.SyncSettings,
    isSyncing: Boolean,
    syncStatusMsg: String,
    onSaveSettings: (com.example.data.sync.SyncSettings) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSmartSync: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var urlInput by remember(settings.url) { mutableStateOf(settings.url) }
    var usernameInput by remember(settings.username) { mutableStateOf(settings.username) }
    var passwordInput by remember(settings.password) { mutableStateOf(settings.password) }
    var fileNameInput by remember(settings.fileName) { mutableStateOf(settings.fileName) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
        border = BorderStroke(1.2.dp, Sky100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header clickable toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("☁️", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "WebDAV 云端同步与备份",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EarthyText
                        )
                        Text(
                            text = "上次同步: ${settings.lastSyncTime}",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "展开配置",
                        tint = Slate400
                    )
                }
            }

            // Sync Status Banner
            if (syncStatusMsg.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Sky50, RoundedCornerShape(12.dp))
                        .border(0.8.dp, Sky400, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = syncStatusMsg,
                        fontSize = 12.sp,
                        color = Sky700,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Config Form
            if (isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚙️ 配置 WebDAV 服务器",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarthyText
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("WebDAV 服务器网址 (如坚果云: https://dav.jianguoyun.com/dav/ )", fontSize = 11.sp) },
                        placeholder = { Text("https://example.com/dav/", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("账号 / 邮箱", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("应用授权密码", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("备份文件名", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sky600,
                            focusedLabelColor = Sky600
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val updated = com.example.data.sync.SyncSettings(
                                url = urlInput,
                                username = usernameInput,
                                password = passwordInput,
                                fileName = fileNameInput.ifBlank { "child_growth_backup.json" },
                                lastSyncTime = settings.lastSyncTime
                            )
                            onSaveSettings(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Sky600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存同步配置 💾", color = WarmIvory, fontSize = 13.sp)
                    }

                    HorizontalDivider(color = Slate400.copy(alpha = 0.3f), thickness = 1.dp)
                }
            }

            // Sync Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSmartSync,
                    enabled = !isSyncing && settings.url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Sky600),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "同步",
                        modifier = Modifier.size(16.dp),
                        tint = WarmIvory
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("智能双向同步", fontSize = 12.sp, color = WarmIvory)
                }

                Button(
                    onClick = onBackup,
                    enabled = !isSyncing && settings.url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGreyBorder),
                    border = BorderStroke(1.dp, Slate400),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("备份上云", fontSize = 12.sp, color = Slate700)
                }

                Button(
                    onClick = { showConfirmRestoreDialog = true },
                    enabled = !isSyncing && settings.url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose100.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Rose200),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("覆盖恢复", fontSize = 12.sp, color = Color(0xFFC92A2A))
                }
            }
        }
    }

    if (showConfirmRestoreDialog) {
        Dialog(onDismissRequest = { showConfirmRestoreDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftCardWhite),
                border = BorderStroke(1.5.dp, Rose200),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚠️ 极其重要警告",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC92A2A)
                    )

                    Text(
                        text = "「覆盖恢复」将会彻底清空本地目前的全部宝贝档案及记录，并拉取云端备份来完全覆盖！此操作不可逆。建议您首选使用「智能双向同步」来合并数据。\n\n确实要继续「覆盖恢复」吗？",
                        fontSize = 14.sp,
                        color = EarthyText,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showConfirmRestoreDialog = false }) {
                            Text("取消", color = EarthyText.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showConfirmRestoreDialog = false
                                onRestore()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC92A2A))
                        ) {
                            Text("确定覆盖恢复", color = WarmIvory)
                        }
                    }
                }
            }
        }
    }
}

// Custom Draw Canvas red flower icon for premium look
@Composable
fun LittleRedFlower(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 3f
        val petalRadius = size.width / 4.2f

        // 5 petals color tones for child friendly look
        val petalColors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFF7E7E),
            Color(0xFFFF5252),
            Color(0xFFFF6B6B),
            Color(0xFFFF8E8E)
        )

        for (i in 0 until 5) {
            val angle = (i * 2 * Math.PI / 5) - Math.PI / 2
            val petalCenter = Offset(
                (center.x + Math.cos(angle) * (radius * 1.05f)).toFloat(),
                (center.y + Math.sin(angle) * (radius * 1.05f)).toFloat()
            )
            drawCircle(
                color = petalColors[i],
                radius = petalRadius,
                center = petalCenter
            )
        }

        // Central yellow core
        drawCircle(
            color = Color(0xFFFFD43B),
            radius = radius * 0.7f,
            center = center
        )

        // Cute smiling eyes
        val eyeRadius = radius * 0.12f
        drawCircle(
            color = Color(0xFF3B3232),
            radius = eyeRadius,
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.1f)
        )
        drawCircle(
            color = Color(0xFF3B3232),
            radius = eyeRadius,
            center = Offset(center.x + radius * 0.25f, center.y - radius * 0.1f)
        )

        // Tiny smiling curve
        val smilePath = Path().apply {
            val rect = Rect(
                center.x - radius * 0.22f,
                center.y - radius * 0.05f,
                center.x + radius * 0.22f,
                center.y + radius * 0.18f
            )
            arcTo(rect, 0f, 180f, forceMoveTo = true)
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF3B3232),
            style = Stroke(width = 3f)
        )
    }
}
