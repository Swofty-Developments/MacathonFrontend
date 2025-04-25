package net.swofty.catchngo.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.QuestionsViewModel

class QuestionsScreen {

    private val accentBlue   = Color(0xFF1DA1F2)
    private val accentRed    = Color(0xFFE0245E)
    private val accentGreen  = Color(0xFF4CAF50)
    private val textPrimary  = Color(0xFF14171A)
    private val textSecondary= Color(0xFF657786)
    private val lightSurface = Color(0xFFF5F8FA)
    private val poppins      = FontFamily.Default

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    fun QuestionsContent(
        trackedUser: ApiModels.NearbyUser,
        onFinished: (allCorrect: Boolean) -> Unit,
        vm: QuestionsViewModel = viewModel()
    ) {
        // 1) load MCQ questions on first compose
        val mcqState by vm.mcqState.observeAsState()
        val validationState by vm.mcqValidation.observeAsState()

        LaunchedEffect(trackedUser.id) {
            vm.fetchMcqQuestions(trackedUser.id.toString())
            vm.resetMcqValidation() // Reset validation state on new quiz
        }

        // 2) local navigation through questions
        var idx      by remember { mutableStateOf(0) }
        var selected by remember { mutableStateOf<Int?>(null) }
        val answers  = remember { mutableStateListOf<Int>() }
        var timeLeft by remember { mutableStateOf(30) }
        val scope    = rememberCoroutineScope()

        LaunchedEffect(idx) {
            selected = null
            timeLeft = 30
            while (timeLeft > 0 && selected == null) {
                delay(1_000); timeLeft--
            }
            if (selected == null) {
                answers.add(-1) // Add -1 for skipped/timed out questions
                idx++
            }
        }
        val progress by animateFloatAsState(timeLeft / 30f)

        fun choose(optId: Int) {
            if (selected != null) return
            selected = optId
            answers.add(optId)
            scope.launch {
                delay(500)
                idx++
            }
        }

        // 3) after final answer → submit to backend
        LaunchedEffect(idx, mcqState) {
            val list = (mcqState as? QuestionsViewModel.McqState.Success)?.questions
            if (list != null && idx >= list.size && validationState is QuestionsViewModel.McqValidationState.Idle) {
                vm.validateMcqAnswers(trackedUser.id.toString(), answers.toList())
            }
        }

        // 4) observe validation
        LaunchedEffect(validationState) {
            when (validationState) {
                is QuestionsViewModel.McqValidationState.Success ->
                    onFinished((validationState as QuestionsViewModel.McqValidationState.Success).allCorrect)
                is QuestionsViewModel.McqValidationState.Error ->
                    onFinished(false)
                else -> { /* idle or loading */ }
            }
        }

        // ── UI ────────────────────────────────────────────────────────────
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Quiz", color = Color.White, fontFamily = poppins) },
                    navigationIcon = {
                        IconButton(onClick = { onFinished(false) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = accentBlue)
                )
            },
            containerColor = lightSurface
        ) { pad ->
            Box(modifier = Modifier.fillMaxSize().padding(pad)) {
                when (val s = mcqState) {
                    is QuestionsViewModel.McqState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = accentBlue
                        )
                    }
                    is QuestionsViewModel.McqState.Error -> {
                        Text(
                            text = s.message,
                            color = accentRed,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is QuestionsViewModel.McqState.Success -> {
                        val list = s.questions
                        if (idx < list.size) {
                            val q = list[idx]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    text = q.questionText,
                                    fontFamily = poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    color = textPrimary
                                )
                                // timer bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.LightGray.copy(alpha=0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress)
                                            .background(
                                                Brush.horizontalGradient(listOf(accentBlue, accentGreen)),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                // display MCQ options
                                q.options.forEach { opt ->
                                    OutlinedButton(
                                        onClick = { choose(opt.id) },
                                        enabled = selected == null,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected == opt.id) accentBlue.copy(alpha = 0.1f) else Color.Transparent,
                                            contentColor = if (selected == opt.id) accentBlue else textPrimary
                                        ),
                                        border = BorderStroke(
                                            width = if (selected == opt.id) 2.dp else 1.dp,
                                            color = if (selected == opt.id) accentBlue else Color.Gray.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = opt.answerText,
                                            fontFamily = poppins,
                                            fontWeight = if (selected == opt.id) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Question ${idx+1} of ${list.size}",
                                    fontFamily = poppins,
                                    color = textSecondary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            // waiting for validation response
                            if (validationState is QuestionsViewModel.McqValidationState.Loading) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = accentBlue)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Checking your answers...",
                                        fontFamily = poppins,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // Initial state, should be replaced by Loading state
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = accentBlue
                        )
                    }
                }
            }
        }
    }
}