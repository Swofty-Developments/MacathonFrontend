package net.swofty.catchngo.screens

import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.geometry.Offset

class RegisterScreenClass {
    private val darkBackground = Color(0xFF15202B) // Dark blue background
    private val darkSurface = Color(0xFF1E2732) // Slightly lighter for surfaces
    private val accentBlue = Color(0xFF1DA1F2) // Twitter/X blue
    private val textWhite = Color(0xFFE7E9EA) // Off-white text
    private val textSecondary = Color(0xFF8899A6) // Secondary text color

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RegisterScreen(
        onRegisterSuccess: () -> Unit,
        onNavigateToLogin: () -> Unit,
        authViewModel: AuthViewModel
    ) {
        val registerState by authViewModel.registerState.observeAsState()
        val questionsState by authViewModel.questionsState.observeAsState()

        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        val questionAnswers = remember { mutableStateMapOf<Int, String>() }

        // Fetch questions when screen is first displayed
        LaunchedEffect(Unit) {
            authViewModel.fetchQuestions()
        }

        // Check for successful registration
        LaunchedEffect(registerState) {
            if (registerState is AuthViewModel.RegisterState.Success) {
                onRegisterSuccess()
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "gradientAnimation")

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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create your account",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textSecondary
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Username field with animation
                    val usernameInteractionSource = remember { MutableInteractionSource() }
                    val usernameIsFocused by usernameInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (usernameIsFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
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
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text("Username", color = textSecondary) },
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = darkSurface,
                                cursorColor = accentBlue,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Username",
                                    tint = if (usernameIsFocused) accentBlue else textSecondary
                                )
                            },
                            interactionSource = usernameInteractionSource,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field with animation
                    val passwordInteractionSource = remember { MutableInteractionSource() }
                    val passwordIsFocused by passwordInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (passwordIsFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
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
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Password", color = textSecondary) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = darkSurface,
                                cursorColor = accentBlue,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = if (passwordIsFocused) accentBlue else textSecondary
                                )
                            },
                            interactionSource = passwordInteractionSource,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password field with animation
                    val confirmPasswordInteractionSource = remember { MutableInteractionSource() }
                    val confirmPasswordIsFocused by confirmPasswordInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (confirmPasswordIsFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
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
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        TextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = { Text("Confirm Password", color = textSecondary) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = darkSurface,
                                cursorColor = accentBlue,
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = if (confirmPasswordIsFocused) accentBlue else textSecondary
                                )
                            },
                            interactionSource = confirmPasswordInteractionSource,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Personality Questions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = textWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (val state = questionsState) {
                    is AuthViewModel.QuestionsState.Success -> {
                        items(state.questions) { question ->
                            QuestionItem(
                                question = question,
                                answer = questionAnswers[question.id] ?: "",
                                onAnswerChange = { answer ->
                                    questionAnswers[question.id] = answer
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    is AuthViewModel.QuestionsState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = accentBlue
                                )
                            }
                        }
                    }
                    is AuthViewModel.QuestionsState.Error -> {
                        item {
                            Text(
                                text = "Error loading questions: ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { authViewModel.fetchQuestions() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentBlue
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    else -> {}
                }

                item {
                    if (registerState is AuthViewModel.RegisterState.Error) {
                        Text(
                            text = (registerState as AuthViewModel.RegisterState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (validateInputs(username, password, confirmPassword, questionAnswers)) {
                                val questions = questionAnswers.map { (id, answer) ->
                                    ApiModels.QuestionAnswer(id, answer)
                                }
                                authViewModel.register(username, password, questions)
                            }
                        },
                        enabled = registerState !is AuthViewModel.RegisterState.Loading,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = accentBlue.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
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
                                shape = RoundedCornerShape(50.dp)
                            )
                    ) {
                        if (registerState is AuthViewModel.RegisterState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Sign up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onNavigateToLogin,
                    ) {
                        Text(
                            "Already have an account? Sign in",
                            color = accentBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QuestionItem(
        question: ApiModels.Question,
        answer: String,
        onAnswerChange: (String) -> Unit
    ) {
        val answerInteractionSource = remember { MutableInteractionSource() }
        val answerIsFocused by answerInteractionSource.collectIsFocusedAsState()

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textWhite
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (answerIsFocused) {
                            Modifier.border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
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
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                TextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    placeholder = { Text("Your answer", color = textSecondary) },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = darkSurface,
                        cursorColor = accentBlue,
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Question Answer",
                            tint = if (answerIsFocused) accentBlue else textSecondary
                        )
                    },
                    interactionSource = answerInteractionSource,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private fun validateInputs(
        username: String,
        password: String,
        confirmPassword: String,
        questionAnswers: Map<Int, String>
    ): Boolean {
        if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            return false
        }

        if (password != confirmPassword) {
            return false
        }

        if (questionAnswers.any { it.value.isBlank() }) {
            return false
        }

        return true
    }
}