package net.swofty.catchngo.screens

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.AuthViewModel
import net.swofty.catchngo.models.QuestionsViewModel

class RegisterScreenClass {

    /* ------ Colours ------ */
    private val darkBackground = Color(0xFF15202B)
    private val darkSurface    = Color(0xFF1E2732)
    private val accentBlue     = Color(0xFF1DA1F2)
    private val textWhite      = Color(0xFFE7E9EA)
    private val textSecondary  = Color(0xFF8899A6)

    /* =========================================================
       Composable entry-point
       ========================================================= */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RegisterScreen(
        onRegisterSuccess: () -> Unit,
        onNavigateToLogin: () -> Unit,
        authViewModel: AuthViewModel,
        questionsViewModel: QuestionsViewModel
    ) {

        /* ------------ State from ViewModels ------------ */
        val registerState   by authViewModel.registerState.observeAsState()
        val questionsState  by questionsViewModel.state.observeAsState(QuestionsViewModel.State.Loading)

        /* ------------ Local UI state ------------ */
        var username        by remember { mutableStateOf("") }
        var password        by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        val questionAnswers = remember { mutableStateMapOf<Int, String>() }

        /* ---------- Validation state ---------- */
        var attemptedSubmit        by remember { mutableStateOf(false) }
        var usernameError          by remember { mutableStateOf(false) }
        var passwordError          by remember { mutableStateOf(false) }
        var confirmPasswordError   by remember { mutableStateOf(false) }
        var questionsError         by remember { mutableStateOf(false) }
        var errorBannerMessage     by remember { mutableStateOf<String?>(null) }

        /* ---------- One-shot actions ---------- */
        LaunchedEffect(Unit) { questionsViewModel.refresh() }
        LaunchedEffect(registerState) {
            if (registerState is AuthViewModel.RegisterState.Success) onRegisterSuccess()
        }

        /* =========================================================
           Layout
           ========================================================= */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /* ---------- Title ---------- */
                item {
                    Text(
                        text = "Catch N Go",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textWhite,
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create your account",
                        style = MaterialTheme.typography.bodyLarge.copy(color = textSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                }

                /* ---------- Username ---------- */
                item {
                    FieldUsername(
                        value           = username,
                        onChange        = { username = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = usernameError
                    )
                }

                /* ---------- Password ---------- */
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    FieldPassword(
                        value           = password,
                        onChange        = { password = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = passwordError,
                        label           = "Password"
                    )
                }

                /* ---------- Confirm Password ---------- */
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    FieldPassword(
                        value           = confirmPassword,
                        onChange        = { confirmPassword = it },
                        attemptedSubmit = attemptedSubmit,
                        showError       = confirmPasswordError,
                        label           = "Confirm Password"
                    )
                }

                /* ---------- Personality Questions ---------- */
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Personality Questions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = textWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                }

                when (val qs = questionsState) {
                    is QuestionsViewModel.State.Success -> {
                        items(qs.questions) { q ->
                            val qError = attemptedSubmit && questionAnswers[q.id].isNullOrBlank()
                            QuestionItem(
                                question    = q,
                                answer      = questionAnswers[q.id] ?: "",
                                onAnswerChange = { ans -> questionAnswers[q.id] = ans },
                                showError   = qError
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    is QuestionsViewModel.State.Loading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = accentBlue) }
                    }
                    is QuestionsViewModel.State.Error -> item {
                        Text(
                            text   = "Error loading questions: ${qs.message}",
                            color  = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = questionsViewModel::refresh,
                                colors  = ButtonDefaults.buttonColors(containerColor = accentBlue),
                                shape   = RoundedCornerShape(50.dp)
                            ) { Text("Retry") }
                        }
                    }
                    else -> {}
                }

                /* ---------- Error banner & Sign-up button ---------- */
                item {
                    if (errorBannerMessage != null) {
                        Text(
                            errorBannerMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            attemptedSubmit = true

                            val errors = validateInputs(
                                username,
                                password,
                                confirmPassword,
                                questionAnswers
                            )

                            usernameError        = errors.usernameMissing
                            passwordError        = errors.passwordMissing
                            confirmPasswordError = errors.confirmPasswordMissing || errors.passwordsMismatch
                            questionsError       = errors.questionsIncomplete

                            errorBannerMessage = when {
                                errors.usernameMissing || errors.passwordMissing ||
                                        errors.confirmPasswordMissing || errors.questionsIncomplete ->
                                    "Please fill in all required fields"
                                errors.passwordsMismatch -> "Passwords do not match"
                                else -> null
                            }

                            val isValid = !(errors.usernameMissing ||
                                    errors.passwordMissing ||
                                    errors.confirmPasswordMissing ||
                                    errors.passwordsMismatch ||
                                    errors.questionsIncomplete)

                            if (isValid) {
                                authViewModel.register(
                                    username,
                                    password,
                                    questionAnswers.map { (id, ans) ->
                                        ApiModels.QuestionAnswer(id, ans)
                                    }
                                )
                            }
                        },
                        enabled = registerState !is AuthViewModel.RegisterState.Loading,
                        shape   = RoundedCornerShape(50.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor        = accentBlue,
                            contentColor          = Color.White,
                            disabledContainerColor= accentBlue.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(
                                1.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f),
                                        Color.White.copy(alpha = 0.0f)
                                    ),
                                    center = Offset.Zero
                                ),
                                RoundedCornerShape(50.dp)
                            )
                    ) {
                        if (registerState is AuthViewModel.RegisterState.Loading)
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color    = Color.White
                            )
                        else
                            Text("Sign up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Sign in", color = accentBlue)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    /* =========================================================
       Field composables
       ========================================================= */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FieldUsername(
        value: String,
        onChange: (String) -> Unit,
        attemptedSubmit: Boolean,
        showError: Boolean
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused         by interactionSource.collectIsFocusedAsState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .borderForField(isFocused, showError)
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder    = { Text("Username", color = textSecondary) },
                leadingIcon    = {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = "Username",
                        tint               = if (isFocused) accentBlue else textSecondary
                    )
                },
                interactionSource = interactionSource,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor         = darkSurface,
                    cursorColor            = accentBlue,
                    focusedTextColor       = textWhite,
                    unfocusedTextColor     = textWhite,
                    focusedIndicatorColor  = Color.Transparent,
                    unfocusedIndicatorColor= Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FieldPassword(
        value: String,
        onChange: (String) -> Unit,
        attemptedSubmit: Boolean,
        showError: Boolean,
        label: String
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused         by interactionSource.collectIsFocusedAsState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .borderForField(isFocused, showError)
        ) {
            TextField(
                value = value,
                onValueChange = onChange,
                placeholder    = { Text(label, color = textSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = label,
                        tint               = if (isFocused) accentBlue else textSecondary
                    )
                },
                interactionSource = interactionSource,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor         = darkSurface,
                    cursorColor            = accentBlue,
                    focusedTextColor       = textWhite,
                    unfocusedTextColor     = textWhite,
                    focusedIndicatorColor  = Color.Transparent,
                    unfocusedIndicatorColor= Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    /* =========================================================
       Question item
       ========================================================= */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QuestionItem(
        question: ApiModels.Question,
        answer: String,
        onAnswerChange: (String) -> Unit,
        showError: Boolean
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused         by interactionSource.collectIsFocusedAsState()

        Column(Modifier.fillMaxWidth()) {
            Text(question.questionText, style = MaterialTheme.typography.bodyMedium.copy(color = textWhite))
            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .borderForField(isFocused, showError)
            ) {
                TextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    placeholder = { Text("Your answer", color = textSecondary) },
                    leadingIcon  = {
                        Icon(
                            imageVector        = Icons.Default.AddCircle,
                            contentDescription = "Answer",
                            tint               = if (isFocused) accentBlue else textSecondary
                        )
                    },
                    interactionSource = interactionSource,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor         = darkSurface,
                        cursorColor            = accentBlue,
                        focusedTextColor       = textWhite,
                        unfocusedTextColor     = textWhite,
                        focusedIndicatorColor  = Color.Transparent,
                        unfocusedIndicatorColor= Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    /* =========================================================
       Validation helpers
       ========================================================= */
    private data class ValidationErrors(
        val usernameMissing: Boolean,
        val passwordMissing: Boolean,
        val confirmPasswordMissing: Boolean,
        val passwordsMismatch: Boolean,
        val questionsIncomplete: Boolean
    )

    private fun validateInputs(
        username: String,
        password: String,
        confirmPassword: String,
        questionAnswers: Map<Int, String>
    ): ValidationErrors {
        val usernameMissing       = username.isBlank()
        val passwordMissing       = password.isBlank()
        val confirmPasswordMissing= confirmPassword.isBlank()
        val passwordsMismatch     = password != confirmPassword
        val questionsIncomplete   = questionAnswers.any { it.value.isBlank() }

        return ValidationErrors(
            usernameMissing,
            passwordMissing,
            confirmPasswordMissing,
            passwordsMismatch,
            questionsIncomplete
        )
    }

    private fun Modifier.borderForField(isFocused: Boolean, hasError: Boolean): Modifier {
        return when {
            hasError -> this.border(
                2.dp,
                Color.Red,
                RoundedCornerShape(12.dp)
            )
            isFocused -> this.border(
                2.dp,
                Brush.sweepGradient(
                    listOf(
                        accentBlue.copy(alpha = 0.2f),
                        accentBlue.copy(alpha = 0.5f),
                        accentBlue.copy(alpha = 0.8f),
                        accentBlue,
                        accentBlue.copy(alpha = 0.8f),
                        accentBlue.copy(alpha = 0.5f),
                        accentBlue.copy(alpha = 0.2f)
                    ),
                    center = Offset.Zero
                ),
                RoundedCornerShape(12.dp)
            )
            else -> this
        }
    }
}
