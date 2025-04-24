package net.swofty.catchngo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.models.AuthViewModel

class RegisterScreenClass {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Personality Questions",
                    style = MaterialTheme.typography.titleMedium
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
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(32.dp)
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
                                onClick = { authViewModel.fetchQuestions() }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (registerState is AuthViewModel.RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Register")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onNavigateToLogin
                    ) {
                        Text("Already have an account? Login")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                placeholder = { Text("Your answer") },
                modifier = Modifier.fillMaxWidth()
            )
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